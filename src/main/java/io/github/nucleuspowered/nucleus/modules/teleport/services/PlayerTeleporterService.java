/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResults;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.traits.MessageProviderTrait;
import io.github.nucleuspowered.nucleus.internal.traits.PermissionTrait;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import io.github.nucleuspowered.nucleus.modules.teleport.TeleportUserPrefKeys;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportAcceptCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportDenyCommand;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextStyles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nullable;

public class PlayerTeleporterService implements ServiceBase, MessageProviderTrait, PermissionTrait {

    private final SafeTeleportService safeTeleportService = Nucleus.getNucleus()
            .getInternalServiceManager()
            .getServiceUnchecked(SafeTeleportService.class);

    private static final String TP_TOGGLE_BYPASS_PERMISSION = PermissionRegistry.PERMISSIONS_PREFIX + "teleport.tptoggle.exempt";
    private static final String TPA_ACCEPT_PERM =
            Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(TeleportAcceptCommand.class).getBase();
    private static final String TPA_DENY_PERM =
            Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(TeleportDenyCommand.class).getBase();

    public boolean canTeleportTo(CommandSource source, User to)  {
        if (source instanceof Player && !canBypassTpToggle(source)) {
            UserPreferenceService ups = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(UserPreferenceService.class);
            if (!ups.get(to.getUniqueId(), TeleportUserPrefKeys.TELEPORT_TARGETABLE).orElse(true)) {
                sendMessageTo(source, "teleport.fail.targettoggle", to.getName());
                return false;
            }
        }

        return true;
    }

    private boolean canBypassTpToggle(Subject from) {
        return Nucleus.getNucleus().getPermissionResolver().hasPermission(from, TP_TOGGLE_BYPASS_PERMISSION);
    }

    private final Map<UUID, TeleportRequest> activeTeleportRequestsCommand = new HashMap<>();
    private final Multimap<UUID, TeleportRequest> activeTeleportRequests = HashMultimap.create();

    public TeleportResult teleportWithMessage(
            CommandSource source,
            Player playerToTeleport,
            Player target,
            boolean safe,
            boolean quietSource,
            boolean quietTarget) {

        TeleportResult result =
                this.safeTeleportService.teleportPlayerSmart(
                                playerToTeleport,
                                target.getTransform(),
                                false,
                                safe,
                                TeleportScanners.NO_SCAN
                        );
        if (result.isSuccessful()) {
            if (!source.equals(target) && !quietSource) {
                sendMessageTo(source, "teleport.success.source",
                        playerToTeleport.getName(),
                        target.getName());
            }

            sendMessageTo(playerToTeleport, "teleport.to.success", target.getName());
            if (!quietTarget) {
                sendMessageTo(target, "teleport.from.success", playerToTeleport.getName());
            }
        } else if (!quietSource) {
            sendMessageTo(source, result == TeleportResults.FAIL_NO_LOCATION ? "teleport.nosafe" : "teleport.cancelled");
        }

        return result;
    }

    public boolean requestTeleport(
            @Nullable Player requester,
            Player toRequest,
            double cost,
            int warmup,
            Player playerToTeleport,
            Player target,
            boolean safe,
            boolean silentTarget,
            boolean silentSource,
            @Nullable Consumer<Player> successCallback,
            String messageKey) {
        removeExpired();

        if (canTeleportTo(playerToTeleport, target)) {
            CommandSource src = requester == null ? Sponge.getServer().getConsole() : requester;

            TeleportRequest request = new TeleportRequest(
                    playerToTeleport.getUniqueId(),
                    target.getUniqueId(),
                    Instant.now().plus(30, ChronoUnit.SECONDS),
                    cost,
                    warmup,
                    requester == null ? null : requester.getUniqueId(),
                    safe,
                    silentTarget,
                    silentSource,
                    successCallback);
            this.activeTeleportRequestsCommand.put(toRequest.getUniqueId(), request);
            this.activeTeleportRequests.put(toRequest.getUniqueId(), request);

            sendMessageTo(toRequest, messageKey, src.getName());
            toRequest.sendMessage(getAcceptDenyMessage(toRequest, request));

            if (!silentSource) {
                sendMessageTo(src, "command.tpask.sent", toRequest.getName());
            }
            return true;
        }

        return false;
    }

    /**
     * Gets the request associated with the tp accept.
     *
     * @return The request, if any.
     */
    public Optional<TeleportRequest> getCurrentRequest(Player player) {
        return Optional.ofNullable(this.activeTeleportRequestsCommand.get(player.getUniqueId()));
    }

    /**
     * Removes any outstanding requests for the specified player.
     *
     * @param player The player
     */
    public void removeRequestsFor(Player player) {
        this.activeTeleportRequests.removeAll(player.getUniqueId()).forEach(x -> x.forceExpire(true));
        this.activeTeleportRequestsCommand.remove(player.getUniqueId());
    }

    public void removeExpired() {
        this.activeTeleportRequests.values().removeIf(x -> !x.isActive());
        this.activeTeleportRequestsCommand.values().removeIf(x -> !x.isActive());
    }

    private Text getAcceptDenyMessage(Player forPlayer, TeleportRequest target) {
        Text acceptText = Text.builder().append(
                getMessageFor(forPlayer.getLocale(), "standard.accept"))
                .style(TextStyles.UNDERLINE)
                .onHover(TextActions.showText(
                        getMessageFor(forPlayer.getLocale(), "teleport.accept.hover")))
                .onClick(TextActions.executeCallback(src -> {
                    if (!target.isActive() || !hasPermission(src, TPA_ACCEPT_PERM) || !(src instanceof Player)) {
                        sendMessageTo(src, "command.tpaccept.nothing");
                        return;
                    }
                    accept((Player) src, target);
                })).build();
        Text denyText = Text.builder().append(
                getMessageFor(forPlayer.getLocale(), "standard.deny"))
                .style(TextStyles.UNDERLINE)
                .onHover(TextActions.showText(getMessageFor(forPlayer.getLocale(), "teleport.deny.hover")))
                .onClick(TextActions.executeCallback(src -> {
                    if (!target.isActive() || !hasPermission(src, TPA_DENY_PERM) || !(src instanceof Player)) {
                        sendMessageTo(src, "command.tpdeny.fail");
                        return;
                    }
                    deny((Player) src, target);
                })).build();

        return Text.builder()
                .append(acceptText)
                .append(Text.of(" - "))
                .append(denyText).build();
    }

    public CommandResult accept(Player player) {
        return accept(player, getCurrentRequest(player).orElse(null)) ? CommandResult.success() : CommandResult.empty();
    }

    private boolean accept(Player player, @Nullable TeleportRequest target) {
        if (target == null) {
            sendMessageTo(player, "command.tpaccept.nothing");
            return false;
        }

        if (!target.isActive()) {
            sendMessageTo(player, "command.tpaccept.expired");
            return false;
        }

        this.activeTeleportRequests.values().remove(target);
        this.activeTeleportRequestsCommand.values().remove(target);
        target.forceExpire(false);

        Task task = Task.builder()
                .delay(target.warmup, TimeUnit.SECONDS)
                .execute(target)
                .submit(Nucleus.getNucleus());
        Nucleus.getNucleus().getWarmupManager().addWarmup(target.toTeleport, task);
        sendMessageTo(player, "command.tpaccept.success");
        return true;
    }

    public CommandResult deny(Player player) {
        return deny(player, getCurrentRequest(player).orElse(null)) ? CommandResult.success() : CommandResult.empty();
    }

    private boolean deny(Player player, @Nullable TeleportRequest target) {
        if (target == null) {
            sendMessageTo(player, "command.tpaccept.nothing");
            return false;
        } else if (!target.isActive()) {
            sendMessageTo(player, "command.tpaccept.expired");
            return false;
        }

        target.forceExpire(true);
        this.activeTeleportRequests.values().remove(target);
        this.activeTeleportRequestsCommand.values().remove(target);
        sendMessageTo(player, "command.tpdeny.deny");
        return true;
    }

    static void onCancel(UUID requester, UUID toTeleport, double cost) {
        MessageProvider provider = Nucleus.getNucleus().getMessageProvider();
        final Text name = Nucleus.getNucleus().getNameUtil().getNameOrConsole(toTeleport);
        if (requester == null) {
            Sponge.getServer().getConsole().sendMessage(
                    provider.getTextMessageWithFormat("command.tpdeny.denyrequester", name)
            );
        } else {
            Optional<Player> op = Sponge.getServer().getPlayer(requester);
            op.ifPresent(x -> x.sendMessage(provider.getTextMessageWithFormat("command.tpdeny.denyrequester", name)));

            if (cost > 0) {
                // refund the cost.
                op.ifPresent(x -> x.sendMessage(
                        provider.getTextMessageWithFormat(
                                "teleport.prep.cancel", Nucleus.getNucleus().getEconHelper().getCurrencySymbol(cost))));

                User user = op.map(x -> (User) x).orElseGet(() -> Sponge.getServiceManager()
                        .provideUnchecked(UserStorageService.class)
                        .get(requester).orElse(null));
                if (user != null) {
                    Nucleus.getNucleus().getEconHelper().depositInPlayer(user, cost);
                }
            }
        }
    }

}
