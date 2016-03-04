/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.internal.services;

import com.google.common.base.Preconditions;
import io.github.essencepowered.essence.Essence;
import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.internal.PermissionRegistry;
import io.github.essencepowered.essence.internal.interfaces.CancellableTask;
import io.github.essencepowered.essence.internal.interfaces.InternalEssenceUser;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TeleportHandler {

    private final Essence plugin;
    private final Map<UUID, TeleportPrep> ask = new HashMap<>();

    public static final String tptoggleBypassPermission = PermissionRegistry.PERMISSIONS_PREFIX + "teleport.tptoggle.exempt";
    private Text acceptDeny;

    public TeleportHandler(Essence plugin) {
        this.plugin = plugin;
    }

    public TeleportBuilder getBuilder() {
        return new TeleportBuilder(plugin);
    }

    public static boolean canBypassTpToggle(CommandSource from) {
        return from.hasPermission(tptoggleBypassPermission);
    }

    public void addAskQuestion(UUID target, TeleportPrep tp) {
        clearExpired();
        get(target).ifPresent(this::cancel);
        ask.put(target, tp);
    }

    public void clearExpired() {
        Instant now = Instant.now();
        ask.entrySet().stream().filter(x -> now.isAfter(x.getValue().getExpire())).map(Map.Entry::getKey).collect(Collectors.toList())
                .forEach(x -> cancel(ask.remove(x)));
    }

    public boolean getAndExecute(UUID uuid) throws Exception {
        Optional<TeleportPrep> otp = get(uuid);
        if (otp.isPresent()) {
            return otp.get().tpbuilder.startTeleport();
        }

        return false;
    }

    public Optional<TeleportPrep> get(UUID uuid) {
        clearExpired();
        return Optional.ofNullable(ask.remove(uuid));
    }

    public boolean remove(UUID uuid) {
        TeleportPrep tp = ask.remove(uuid);
        cancel(tp);
        return tp != null;
    }

    public Text getAcceptDenyMessage() {
        if (acceptDeny == null) {
            acceptDeny = Text.builder()
                    .append(
                            Text.builder(Util.getMessageWithFormat("standard.accept")).color(TextColors.GREEN).style(TextStyles.UNDERLINE)
                                    .onHover(TextActions.showText(Text.of(Util.getMessageWithFormat("teleport.accept.hover")))).onClick(TextActions.runCommand("/tpaccept")).build())
                    .append(Text.of(" - "))
                    .append(Text.builder(Util.getMessageWithFormat("standard.deny")).color(TextColors.GREEN).style(TextStyles.UNDERLINE)
                            .onHover(TextActions.showText(Text.of(Util.getMessageWithFormat("teleport.deny.hover")))).onClick(TextActions.runCommand("/tpdeny")).build())
                    .build();
        }

        return acceptDeny;
    }

    private void cancel(@Nullable TeleportPrep prep) {
        if (prep == null) {
            return;
        }

        if (prep.charged != null && prep.cost > 0) {
            if (prep.charged.isOnline()) {
                prep.charged.getPlayer().ifPresent(x -> x.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("teleport.prep.cancel", plugin.getEconHelper().getCurrencySymbol(prep.cost)))));
            }

            plugin.getEconHelper().depositInPlayer(prep.charged, prep.cost);
        }
    }

    public static class TeleportTask implements CancellableTask {

        private final Player from;
        private final Player to;
        private final Player charged;
        private final double cost;
        private final boolean safe;
        private final CommandSource source;
        private final Essence plugin;
        private final boolean silentSouce;

        private TeleportTask(Essence plugin, CommandSource source, Player from, Player to, boolean safe, boolean silentSouce) {
            this(plugin, source, from, to, null, 0, safe, silentSouce);
        }

        private TeleportTask(Essence plugin, CommandSource source, Player from, Player to, Player charged, double cost, boolean safe, boolean silentSouce) {
            this.plugin = plugin;
            this.source = source;
            this.from = from;
            this.to = to;
            this.cost = cost;
            this.charged = charged;
            this.safe = safe;
            this.silentSouce = silentSouce;
        }

        private void run() {
            if (to.isOnline()) {
                if (safe && !from.setLocationAndRotationSafely(to.getLocation(), to.getRotation())) {
                    if (!silentSouce) {
                        source.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("teleport.nosafe")));
                    }

                    onCancel();
                    return;
                } else {
                    from.setLocationAndRotation(to.getLocation(), to.getRotation());
                }

                if (!source.equals(from) && !silentSouce) {
                    source.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("teleport.success.source", from.getName(), to.getName())));
                }

                from.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("teleport.success", to.getName())));

                if (!silentSouce) {
                    to.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("teleport.from.success", from.getName())));
                }
            } else {
                if (!silentSouce) {
                    source.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("teleport.fail")));
                }

                onCancel();
            }
        }

        @Override
        public void accept(Task task) {
            run();
        }

        @Override
        public void onCancel() {
            if (!silentSouce) {
                source.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("teleport.cancelled")));
            }

            if (charged != null && cost > 0) {
                plugin.getEconHelper().depositInPlayer(charged, cost);
            }
        }
    }

    public static class TeleportBuilder {

        private CommandSource source;
        private Player from;
        private Player to;
        private Player charge;
        private double cost;
        private int warmupTime = 0;
        private boolean bypassToggle = false;
        private boolean safe = true;
        private boolean silentSource = false;

        private final Essence plugin;

        private TeleportBuilder(Essence plugin) {
            this.plugin = plugin;
        }

        public TeleportBuilder setSafe(boolean safe) {
            this.safe = safe;
            return this;
        }

        public TeleportBuilder setSource(CommandSource source) {
            this.source = source;
            return this;
        }

        public TeleportBuilder setFrom(Player from) {
            this.from = from;
            return this;
        }

        public TeleportBuilder setTo(Player to) {
            this.to = to;
            return this;
        }

        public TeleportBuilder setCharge(Player charge) {
            this.charge = charge;
            return this;
        }

        public TeleportBuilder setCost(double cost) {
            this.cost = cost;
            return this;
        }

        public TeleportBuilder setWarmupTime(int warmupTime) {
            this.warmupTime = warmupTime;
            return this;
        }

        public TeleportBuilder setBypassToggle(boolean bypassToggle) {
            this.bypassToggle = bypassToggle;
            return this;
        }

        public TeleportBuilder setSilentSource(boolean silent) {
            this.silentSource = silent;
            return this;
        }

        public boolean startTeleport() throws Exception {
            Preconditions.checkNotNull(from);
            Preconditions.checkNotNull(to);

            if (source == null) {
                source = from;
            }

            if (from.equals(to)) {
                source.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("command.teleport.self")));
                return false;
            }

            InternalEssenceUser toPlayer = plugin.getUserLoader().getUser(to);
            if (!bypassToggle && !toPlayer.isTeleportToggled() && !canBypassTpToggle(source)) {
                from.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("teleport.fail.targettoggle", to.getName())));
                return false;
            }

            TeleportTask tt;
            if (cost > 0 && charge != null) {
                tt = new TeleportTask(plugin, source, from, to, charge, cost, safe, silentSource);
            } else {
                tt = new TeleportTask(plugin, source, from, to, safe, silentSource);
            }

            if (warmupTime > 0) {
                from.sendMessage(Text.of(Util.getMessageWithFormat("teleport.warmup", String.valueOf(warmupTime))));
                plugin.getWarmupManager().addWarmup(from.getUniqueId(),
                        Sponge.getScheduler().createTaskBuilder().delay(warmupTime, TimeUnit.SECONDS)
                                .execute(tt).name("Essence - Teleport Waiter").submit(plugin));
            } else {
                tt.run();
            }

            return true;
        }
    }

    public static class TeleportPrep {
        private final Instant expire;
        private final User charged;
        private final double cost;
        private final TeleportBuilder tpbuilder;

        public TeleportPrep(Instant expire, User charged, double cost, TeleportBuilder tpbuilder) {
            this.expire = expire;
            this.charged = charged;
            this.cost = cost;
            this.tpbuilder = tpbuilder;
        }

        public Instant getExpire() {
            return expire;
        }

        public User getCharged() {
            return charged;
        }

        public double getCost() {
            return cost;
        }

        public TeleportBuilder getTpbuilder() {
            return tpbuilder;
        }
    }
}
