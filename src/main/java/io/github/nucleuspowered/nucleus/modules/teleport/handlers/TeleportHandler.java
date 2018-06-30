/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.handlers;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.dataservices.modular.ModularUserService;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.interfaces.CancellableTask;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import io.github.nucleuspowered.nucleus.internal.traits.InternalServiceManagerTrait;
import io.github.nucleuspowered.nucleus.internal.traits.MessageProviderTrait;
import io.github.nucleuspowered.nucleus.internal.traits.PermissionHandlerTrait;
import io.github.nucleuspowered.nucleus.modules.jail.JailModule;
import io.github.nucleuspowered.nucleus.modules.jail.datamodules.JailUserDataModule;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportAcceptCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportDenyCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.config.TeleportConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.teleport.datamodules.TeleportUserDataModule;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextStyles;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class TeleportHandler implements MessageProviderTrait, PermissionHandlerTrait {

    private final Map<UUID, TeleportPrep> ask = new HashMap<>();
    private final String acceptPerm = getPermissionHandlerFor(TeleportAcceptCommand.class).getBase();
    private final String denyPerm = getPermissionHandlerFor(TeleportDenyCommand.class).getBase();

    private static final String tptoggleBypassPermission = PermissionRegistry.PERMISSIONS_PREFIX + "teleport.tptoggle.exempt";

    public TeleportBuilder getBuilder() {
        return new TeleportBuilder();
    }

    public static boolean canBypassTpToggle(Subject from) {
        return from.hasPermission(tptoggleBypassPermission);
    }

    public static boolean canTeleportTo(CommandSource source, User to)  {
        if (source instanceof Player && !TeleportHandler.canBypassTpToggle(source)) {
            if (!Nucleus.getNucleus().getUserDataManager().get(to).map(x -> x.get(TeleportUserDataModule.class).isTeleportToggled()).orElse(true)) {
                source.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("teleport.fail.targettoggle", to.getName()));
                return false;
            }
        }

        return true;
    }

    public void addAskQuestion(UUID target, TeleportPrep tp) {
        clearExpired();
        get(target).ifPresent(this::cancel);
        this.ask.put(target, tp);
    }

    public void clearExpired() {
        Instant now = Instant.now();
        this.ask.entrySet().stream().filter(x -> now.isAfter(x.getValue().getExpire())).map(Map.Entry::getKey).collect(Collectors.toList())
                .forEach(x -> cancel(this.ask.remove(x)));
    }

    public boolean getAndExecute(UUID uuid) {
        Optional<TeleportPrep> otp = get(uuid);
        return otp.isPresent() && otp.get().tpbuilder.startTeleport();

    }

    public Optional<TeleportPrep> get(UUID uuid) {
        clearExpired();
        return Optional.ofNullable(this.ask.remove(uuid));
    }

    public boolean remove(UUID uuid) {
        TeleportPrep tp = this.ask.remove(uuid);
        cancel(tp);
        return tp != null;
    }

    public CommandResult accept(Player player) {
        return accept(player, null) ? CommandResult.success() : CommandResult.empty();
    }

    private boolean accept(Player player, @Nullable TeleportPrep target) {
        if (target == null) {
            target = get(player.getUniqueId()).orElse(null);
            if (target == null) {
                sendMessageTo(player, "command.tpaccept.nothing");
                return false;
            }
        } else if (this.ask.get(player.getUniqueId()) == target) {
            this.ask.remove(player.getUniqueId());
        }

        target.tpbuilder.startTeleport();
        sendMessageTo(player, "command.tpaccept.success");
        return true;
    }

    public CommandResult deny(Player player) {
        return deny(player, null) ? CommandResult.success() : CommandResult.empty();
    }

    private boolean deny(Player player, @Nullable TeleportPrep target) {
        if (target == null) {
            target = get(player.getUniqueId()).orElse(null);
            if (target == null) {
                sendMessageTo(player, "command.tpdeny.fail");
                return false;
            }
        } else if (this.ask.get(player.getUniqueId()) == target) {
            this.ask.remove(player.getUniqueId());
        }

        sendMessageTo(player, "command.tpdeny.deny");
        return true;
    }

    public Text getAcceptDenyMessage(Player forPlayer, TeleportPrep target) {
        return Text.builder()
                .append(
                        Text.builder().append(
                                getMessageFor(forPlayer.getLocale(), "standard.accept"))
                                .style(TextStyles.UNDERLINE)
                                .onHover(TextActions.showText(
                                        getMessageFor(forPlayer.getLocale(), "teleport.accept.hover")))
                                .onClick(TextActions.executeCallback(src -> {
                                    if (target.isExpired() || !src.hasPermission(this.acceptPerm) || !(src instanceof Player)) {
                                        sendMessageTo(src, "command.tpaccept.nothing");
                                        return;
                                    }
                                    accept((Player) src, target);
                                })).build()
                )
                .append(Text.of(" - "))
                .append(
                        Text.builder().append(
                                getMessageFor(forPlayer.getLocale(), "standard.deny"))
                                .style(TextStyles.UNDERLINE)
                                .onHover(TextActions.showText(getMessageFor(forPlayer.getLocale(), "teleport.deny.hover")))
                                .onClick(TextActions.executeCallback(src -> {
                                    if (target.isExpired() || !src.hasPermission(this.denyPerm) || !(src instanceof Player)) {
                                        sendMessageTo(src, "command.tpdeny.fail");
                                        return;
                                    }
                                    deny((Player) src, target);
                                })).build()
                ).build();
    }

    private void cancel(@Nullable TeleportPrep prep) {
        if (prep == null) {
            return;
        }

        if (prep.charged != null && prep.cost > 0) {
            if (prep.charged.isOnline()) {
                prep.charged.getPlayer().ifPresent(x -> x
                        .sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("teleport.prep.cancel",
                                Nucleus.getNucleus().getEconHelper().getCurrencySymbol(prep.cost))));
            }

            Nucleus.getNucleus().getEconHelper().depositInPlayer(prep.charged, prep.cost);
        }
    }

    private static class TeleportTask implements CancellableTask {

        private final Player playerToTeleport;
        private final Player playerToTeleportTo;
        private final User charged;
        private final double cost;
        private final boolean safe;
        private final CommandSource source;
        private final boolean silentSource;
        private final boolean silentTarget;

        private TeleportTask(CommandSource source, Player playerToTeleport, Player playerToTeleportTo, boolean safe, boolean silentSource, boolean silentTarget) {
            this(source, playerToTeleport, playerToTeleportTo, null, 0, safe, silentSource, silentTarget);
        }

        private TeleportTask(CommandSource source, Player playerToTeleport, Player playerToTeleportTo, User charged, double cost, boolean safe,
                             boolean silentSource, boolean silentTarget) {
            this.source = source;
            this.playerToTeleport = playerToTeleport;
            this.playerToTeleportTo = playerToTeleportTo;
            this.cost = cost;
            this.charged = charged;
            this.safe = safe;
            this.silentSource = silentSource;
            this.silentTarget = silentTarget;
        }

        private void run() {
            if (this.playerToTeleportTo.isOnline()) {
                // If safe, get the teleport mode
                NucleusTeleportHandler tpHandler = Nucleus.getNucleus().getTeleportHandler();
                NucleusTeleportHandler.StandardTeleportMode mode = this.safe ? tpHandler.getTeleportModeForPlayer(this.playerToTeleport) :
                    NucleusTeleportHandler.StandardTeleportMode.NO_CHECK;

                NucleusTeleportHandler.TeleportResult result =
                        tpHandler.teleportPlayer(this.playerToTeleport, this.playerToTeleportTo.getTransform(), mode, CauseStackHelper.createCause(this.source));
                if (!result.isSuccess()) {
                    if (!this.silentSource) {
                        this.source.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat(result ==
                                NucleusTeleportHandler.TeleportResult.FAILED_NO_LOCATION ? "teleport.nosafe" : "teleport.cancelled"));
                    }

                    onCancel();
                    return;
                }

                if (!this.source.equals(this.playerToTeleport) && !this.silentSource) {
                    this.source.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("teleport.success.source",
                            this.playerToTeleport.getName(),
                            this.playerToTeleportTo.getName()));
                }

                this.playerToTeleport.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("teleport.to.success",
                        this.playerToTeleportTo.getName()));
                if (!this.silentTarget) {
                    this.playerToTeleportTo.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("teleport.from.success",

                            this.playerToTeleport.getName()));
                }
            } else {
                if (!this.silentSource) {
                    this.source.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("teleport.fail.offline"));
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
            if (this.charged != null && this.cost > 0) {
                Nucleus.getNucleus().getEconHelper().depositInPlayer(this.charged, this.cost);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    public static class TeleportBuilder implements InternalServiceManagerTrait, MessageProviderTrait {

        private UUID source;
        private UUID from;
        private UUID to;
        private UUID charge;
        private double cost;
        private int warmupTime = 0;
        private boolean bypassToggle = false;
        private boolean safe;
        private boolean silentSource = false;
        private boolean silentTarget = false;

        private TeleportBuilder() {
            this.safe = getService(TeleportConfigAdapter.class).map(x -> x.getNodeOrDefault().isUseSafeTeleport()).orElse(true);
        }

        public TeleportBuilder setSafe(boolean safe) {
            this.safe = safe;
            return this;
        }

        public TeleportBuilder setSource(CommandSource source) {
            if (source instanceof User) {
                this.source = ((User) source).getUniqueId();
            } else {
                this.source = Util.consoleFakeUUID;
            }
            return this;
        }

        public TeleportBuilder setFrom(Player from) {
            this.from = from.getUniqueId();
            return this;
        }

        public TeleportBuilder setTo(Player to) {
            this.to = to.getUniqueId();
            return this;
        }

        public TeleportBuilder setCharge(Player charge) {
            this.charge = charge.getUniqueId();
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

        public TeleportBuilder setSilentTarget(boolean silentTarget) {
            this.silentTarget = silentTarget;
            return this;
        }

        public boolean startTeleport() {
            Preconditions.checkNotNull(this.from);
            Preconditions.checkNotNull(this.to);

            if (this.source == null) {
                this.source = this.from;
            }

            Optional<User> ou = Util.getUserFromUUID(this.source);
            if (!ou.isPresent() || !ou.get().isOnline()) {
                // failed.
                return false;
            }

            CommandSource source = ou.<CommandSource>map(x -> x.getPlayer().get()).orElseGet(() -> Sponge.getServer().getConsole());

            if (this.from.equals(this.to)) {
                sendMessageTo(source, "command.teleport.self");
                return false;
            }

            ModularUserService toPlayer = Nucleus.getNucleus().getUserDataManager().get(this.to).get();
            if (!this.bypassToggle && !toPlayer.get(TeleportUserDataModule.class).isTeleportToggled() && !canBypassTpToggle(source)) {
                sendMessageTo(source, "teleport.fail.targettoggle", toPlayer.getUser().getName());
                return false;
            }

            ModularUserService fromPlayer = Nucleus.getNucleus().getUserDataManager().get(this.from).get();
            if (Nucleus.getNucleus().isModuleLoaded(JailModule.ID) &&
                    fromPlayer.get(JailUserDataModule.class).getJailData().isPresent()) {
                // Don't teleport a jailed subject.
                if (!this.silentSource) {
                    sendMessageTo(source,"teleport.fail.jailed", fromPlayer.getUser().getName());
                }

                return false;
            }

            if (!fromPlayer.getPlayer().isPresent()) {
                // failed
                sendMessageTo(source, "teleport.fail.offlinenamed", fromPlayer.getUser().getName());
                return false;
            } else if (!toPlayer.getPlayer().isPresent()) {
                sendMessageTo(source, "teleport.fail.offlinenamed", toPlayer.getUser().getName());
                return false;
            }

            TeleportTask tt;
            if (this.cost > 0 && this.charge != null) {
                tt = new TeleportTask(source,
                        fromPlayer.getPlayer().get(),
                        toPlayer.getPlayer().get(),
                        Util.getUserFromUUID(this.charge).get(),
                        this.cost,
                        this.safe,
                        this.silentSource,
                        this.silentTarget);
            } else {
                tt = new TeleportTask(source,
                        fromPlayer.getPlayer().get(),
                        toPlayer.getPlayer().get(),
                        this.safe,
                        this.silentSource,
                        this.silentTarget);
            }

            if (this.warmupTime > 0) {
                sendMessageTo(fromPlayer.getPlayer().get(), "teleport.warmup", this.warmupTime);
                Nucleus.getNucleus().getWarmupManager().addWarmup(
                        this.from, Sponge.getScheduler().createTaskBuilder().delay(this.warmupTime, TimeUnit.SECONDS)
                        .execute(tt).name("NucleusPlugin - Teleport Waiter").submit(Nucleus.getNucleus()));
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

        public boolean isExpired() {
            return Instant.now().isAfter(this.expire);
        }

        public Instant getExpire() {
            return this.expire;
        }

        public User getCharged() {
            return this.charged;
        }

        public double getCost() {
            return this.cost;
        }

        public TeleportBuilder getTpbuilder() {
            return this.tpbuilder;
        }
    }
}
