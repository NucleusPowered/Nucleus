/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.modules.teleport.TeleportKeys;
import io.github.nucleuspowered.nucleus.modules.teleport.TeleportPermissions;
import io.github.nucleuspowered.nucleus.modules.teleport.config.TeleportConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusLocationService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserPreferenceService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IWarmupService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.service.permission.Subject;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class PlayerTeleporterService implements ServiceBase, IReloadableService.Reloadable {

    private boolean showAcceptDeny = true;

    private final INucleusServiceCollection serviceCollection;
    private final INucleusLocationService safeTeleportService;
    private final IMessageProviderService messageProviderService;
    private final IUserPreferenceService userPreferenceService;
    private final IPermissionService permissionService;
    private final IWarmupService warmupService;

    private boolean refundOnDeny = false;
    private boolean useRequestLocation = true;
    private boolean useCommandsOnClickAcceptDeny = false;
    private boolean isOnlySameDimension = false;

    @Inject
    public PlayerTeleporterService(final INucleusServiceCollection serviceCollection) {
        this.safeTeleportService = serviceCollection.teleportService();
        this.messageProviderService = serviceCollection.messageProvider();
        this.userPreferenceService = serviceCollection.userPreferenceService();
        this.permissionService = serviceCollection.permissionService();
        this.warmupService = serviceCollection.warmupService();
        this.serviceCollection = serviceCollection;
    }

    public boolean canTeleportTo(final ServerPlayer source, final User to)  {
        return this.canTeleportTo(source, source.user(), to, false);
    }

    public boolean canTeleportTo(final Audience audience, final User source, final User to, final boolean isOther)  {
        if (!this.canBypassTpToggle(source)) {
            if (!isOther && !this.userPreferenceService.get(to.uniqueId(), TeleportKeys.TELEPORT_TOGGLE).orElse(true)) {
                this.messageProviderService.sendMessageTo(audience, "teleport.fail.targettoggle", to.name());
                return false;
            }
        }

        if (!isOther && this.isOnlySameDimension) {
            if (!source.worldKey().equals(to.worldKey())) {
                if (!this.permissionService.hasPermission(source, "nucleus.teleport.exempt.samedimension")) {
                    this.messageProviderService.sendMessageTo(audience, "teleport.fail.samedimension", to.name());
                    return false;
                }
            }
        }


        return true;
    }

    private boolean canBypassTpToggle(final Subject from) {
        return this.permissionService.hasPermission(from, TeleportPermissions.TPTOGGLE_EXEMPT);
    }

    private final Map<UUID, TeleportRequest> activeTeleportRequestsCommand = new HashMap<>();
    private final Multimap<UUID, TeleportRequest> activeTeleportRequests = HashMultimap.create();

    public TeleportResult teleportWithMessage(
            final Audience source,
            final ServerPlayer playerToTeleport,
            final ServerPlayer target,
            final boolean safe,
            final boolean quietSource,
            final boolean quietTarget) {

        final TeleportResult result =
                this.safeTeleportService.teleportPlayerSmart(
                                playerToTeleport,
                                target.serverLocation(),
                                target.rotation(),
                                false,
                                safe,
                                TeleportScanners.NO_SCAN.get()
                        );
        if (result.isSuccessful()) {
            if (!source.equals(target) && !quietSource) {
                this.messageProviderService.sendMessageTo(source, "teleport.success.source",
                        playerToTeleport.name(),
                        target.name());
            }

            this.messageProviderService.sendMessageTo(playerToTeleport, "teleport.to.success", target.name());
            if (!quietTarget) {
                this.messageProviderService.sendMessageTo(target, "teleport.from.success", playerToTeleport.name());
            }
        } else if (!quietSource) {
            this.messageProviderService.sendMessageTo(source, result == TeleportResult.FAIL_NO_LOCATION ? "teleport.nosafe" : "teleport.cancelled");
        }

        return result;
    }

    public boolean requestTeleport(
            @Nullable final Audience requester,
            final ServerPlayer toRequest,
            final double cost,
            final int warmup,
            final ServerPlayer playerToTeleport,
            final ServerPlayer target,
            final boolean safe,
            final boolean silentTarget,
            final boolean silentSource,
            @Nullable final Consumer<Player> successCallback,
            final String messageKey) {
        this.removeExpired();

        if (this.canTeleportTo(playerToTeleport, target.user())) {
            final Audience src = requester == null ? Sponge.systemSubject() : requester;

            final TeleportRequest request = new TeleportRequest(
                    this.serviceCollection,
                    playerToTeleport.uniqueId(),
                    target.uniqueId(),
                    Instant.now().plus(30, ChronoUnit.SECONDS),
                    this.refundOnDeny ? cost : 0,
                    warmup,
                    requester instanceof ServerPlayer ? ((ServerPlayer) requester).uniqueId() : null,
                    safe,
                    silentTarget,
                    silentSource,
                    this.useRequestLocation ? target.serverLocation() : null,
                    target.rotation(),
                    successCallback
            );

            this.activeTeleportRequestsCommand.put(toRequest.uniqueId(), request);
            this.activeTeleportRequests.put(toRequest.uniqueId(), request);

            @Nullable final ServerPlayer requesterAsPlayer = src instanceof ServerPlayer ? (ServerPlayer) src : null;
            final Identity requesterIdentity = requesterAsPlayer == null ? Identity.nil() : requesterAsPlayer.identity();
            this.messageProviderService.sendMessageTo(toRequest, messageKey, src instanceof ServerPlayer ? ((ServerPlayer) src).name() : "Server");
            this.getAcceptDenyMessage(toRequest, request).ifPresent(x -> toRequest.sendMessage(requesterIdentity, x));

            if (!silentSource) {
                this.messageProviderService.sendMessageTo(src, "command.tpask.sent", toRequest.name());
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
    public Optional<TeleportRequest> getCurrentRequest(final Player player) {
        return Optional.ofNullable(this.activeTeleportRequestsCommand.get(player.uniqueId()));
    }

    /**
     * Removes any outstanding requests for the specified player.
     *
     * @param player The player
     */
    public void removeRequestsFor(final Player player) {
        this.activeTeleportRequests.removeAll(player.uniqueId()).forEach(x -> x.forceExpire(true));
        this.activeTeleportRequestsCommand.remove(player.uniqueId());
    }

    public void removeExpired() {
        this.activeTeleportRequests.values().removeIf(x -> !x.isActive());
        this.activeTeleportRequestsCommand.values().removeIf(x -> !x.isActive());
    }

    private Optional<Component> getAcceptDenyMessage(final ServerPlayer forPlayer, final TeleportRequest target) {
        if (this.showAcceptDeny) {
            final UUID uuid = forPlayer.uniqueId();
            final Component acceptTextComponent =
                    Component.text().append(this.messageProviderService.getMessageFor(forPlayer, "standard.accept"))
                        .style(Style.style(TextDecoration.UNDERLINED))
                        .hoverEvent(HoverEvent.showText(
                            this.messageProviderService.getMessageFor(forPlayer, "teleport.accept.hover")))
                        .clickEvent(SpongeComponents.executeCallback(src -> {
                            if (!(src.root() instanceof ServerPlayer) || ((ServerPlayer) src.root()).uniqueId().equals(uuid)) {
                                this.messageProviderService.sendMessageTo(src.audience(), "command.tpaccept.nothing");
                            }
                            final ServerPlayer root = (ServerPlayer) src.root();
                            if (!target.isActive() || !this.permissionService.hasPermission(src, TeleportPermissions.BASE_TPACCEPT)) {
                                this.messageProviderService.sendMessageTo(src.audience(), "command.tpaccept.nothing");
                                return;
                            }
                            if (this.useCommandsOnClickAcceptDeny) {
                                try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
                                    frame.pushCause(root);
                                    Sponge.server().commandManager().process("nucleus:tpaccept");
                                } catch (final CommandException ex) {
                                    src.sendMessage(Identity.nil(), ex.componentMessage());
                                }
                            } else {
                                this.accept(root, target);
                            }
                        })).build();
            final Component denyTextComponent =
                    Component.text().append(this.messageProviderService.getMessageFor(forPlayer, "standard.deny"))
                    .style(Style.style(TextDecoration.UNDERLINED))
                    .hoverEvent(HoverEvent.showText(this.messageProviderService.getMessageFor(forPlayer, "teleport.deny.hover")))
                    .clickEvent(SpongeComponents.executeCallback(src -> {
                        if (!(src.root() instanceof ServerPlayer) || ((ServerPlayer) src.root()).uniqueId().equals(uuid)) {
                            this.messageProviderService.sendMessageTo(src.audience(), "command.tpdeny.fail");
                        }
                        final ServerPlayer root = (ServerPlayer) src.root();
                        if (!target.isActive() || !this.permissionService.hasPermission(src, TeleportPermissions.BASE_TPDENY)) {
                            this.messageProviderService.sendMessageTo(src.audience(), "command.tpdeny.fail");
                            return;
                        }
                        if (this.useCommandsOnClickAcceptDeny) {
                            try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
                                frame.pushCause(root);
                                Sponge.server().commandManager().process("nucleus:tpdeny");
                            } catch (final CommandException ex) {
                                src.sendMessage(Identity.nil(), ex.componentMessage());
                            }
                        } else {
                            this.deny((Player) src, target);
                        }
                    })).build();

            return Optional.of(
                    LinearComponents.linear(
                            acceptTextComponent,
                            Component.text(" - "),
                            denyTextComponent
                    ));
        }

        return Optional.empty();
    }

    public boolean accept(final Player player) {
        return this.accept(player, this.getCurrentRequest(player).orElse(null));
    }

    private boolean accept(final Player player, @Nullable final TeleportRequest target) {
        if (target == null) {
            this.messageProviderService.sendMessageTo(player, "command.tpaccept.nothing");
            return false;
        }

        if (!target.isActive()) {
            this.messageProviderService.sendMessageTo(player, "command.tpaccept.expired");
            return false;
        }

        this.activeTeleportRequests.values().remove(target);
        this.activeTeleportRequestsCommand.values().remove(target);
        target.forceExpire(false);

        final Optional<ServerPlayer> playerToTeleport = target.getToBeTeleported();
        if (!playerToTeleport.isPresent()) {
            // error
            this.messageProviderService.sendMessageTo(player, "command.tpaccept.noplayer");
            return false;
        }
        if (target.warmup == 0) {
            target.run();
        } else {
            this.warmupService.executeAfter(
                    playerToTeleport.get(),
                    Duration.of(target.warmup, ChronoUnit.SECONDS),
                    target::run,
                    true);
        }
        this.messageProviderService.sendMessageTo(player, "command.tpaccept.success");
        return true;
    }

    public boolean deny(final Player player) {
        return this.deny(player, this.getCurrentRequest(player).orElse(null));
    }

    private boolean deny(final Player player, @Nullable final TeleportRequest target) {
        if (target == null) {
            this.messageProviderService.sendMessageTo(player, "command.tpaccept.nothing");
            return false;
        } else if (!target.isActive()) {
            this.messageProviderService.sendMessageTo(player, "command.tpaccept.expired");
            return false;
        }

        target.forceExpire(true);
        this.activeTeleportRequests.values().remove(target);
        this.activeTeleportRequestsCommand.values().remove(target);
        this.messageProviderService.sendMessageTo(player, "command.tpdeny.deny");
        return true;
    }

    static void onCancel(final INucleusServiceCollection serviceCollection, final UUID requester, final UUID toTeleport, final double cost) {
        final Component name = serviceCollection.playerDisplayNameService().getDisplayName(toTeleport);
        if (requester == null) {
            serviceCollection.messageProvider().sendMessageTo(Sponge.systemSubject(), "command.tpdeny.denyrequester", name);
        } else {
            final Optional<ServerPlayer> op = Sponge.server().player(requester);
            op.ifPresent(x -> serviceCollection.messageProvider().sendMessageTo(x, "command.tpdeny.denyrequester", name));

            if (serviceCollection.economyServiceProvider().serviceExists() && cost > 0) {
                // refund the cost.
                op.ifPresent(x ->
                        serviceCollection.messageProvider().sendMessageTo(x,
                                "teleport.prep.cancel",
                                serviceCollection.economyServiceProvider().getCurrencySymbol(cost)));

                final User user = op.map(x -> (User) x).orElseGet(() -> Sponge.server().userManager().load(requester).join().orElse(null));
                if (user != null) {
                    serviceCollection.economyServiceProvider().depositInPlayer(user.uniqueId(), cost);
                }
            }
        }
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        final TeleportConfig config = serviceCollection.configProvider().getModuleConfig(TeleportConfig.class);
        this.useCommandsOnClickAcceptDeny = config.isUseCommandsOnClickAcceptOrDeny();
        this.showAcceptDeny = config.isShowClickableAcceptDeny();
        this.refundOnDeny = config.isRefundOnDeny();
        this.useRequestLocation = config.isUseRequestLocation();
        this.isOnlySameDimension = config.isOnlySameDimension();
    }
}
