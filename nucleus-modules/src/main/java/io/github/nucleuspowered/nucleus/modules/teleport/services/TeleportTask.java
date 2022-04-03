/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.services;

import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.core.scaffold.task.CancellableTask;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusLocationService;
import net.kyori.adventure.audience.Audience;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;

import java.util.UUID;
import java.util.function.Consumer;

public class TeleportTask implements CancellableTask {

    final UUID toTeleport;
    protected final UUID target;
    protected final double cost;
    protected final boolean safe;
    protected final int warmup;
    @Nullable private final UUID requester;
    private final boolean silentSource;
    private final boolean silentTarget;
    @Nullable private final ServerLocation requestLocation;
    @Nullable private final Vector3d rotation;
    @Nullable private final Consumer<Player> successCallback;
    private final INucleusServiceCollection serviceCollection;

    public TeleportTask(
            final INucleusServiceCollection serviceCollection,
            final UUID toTeleport,
            final UUID target,
            final double cost,
            final int warmup,
            final boolean safe,
            final boolean silentSource,
            final boolean silentTarget,
            @Nullable final ServerLocation requestLocation,
            @Nullable final Vector3d rotation,
            @Nullable final UUID requester,
            @Nullable final Consumer<Player> successCallback) {
        this.toTeleport = toTeleport;
        this.target = target;
        this.cost = cost;
        this.warmup = warmup;
        this.safe = safe;
        this.silentSource = silentSource;
        this.silentTarget = silentTarget;
        this.requester = requester;
        this.successCallback = successCallback;
        this.requestLocation = requestLocation;
        this.rotation = rotation;
        this.serviceCollection = serviceCollection;
    }

    @Override
    public void onCancel() {
        PlayerTeleporterService.onCancel(this.serviceCollection, this.requester, this.toTeleport, this.cost);
    }

    @Override
    public void accept(final ScheduledTask task) {
        this.run();
    }

    public void run() {
        // Teleport them
        final ServerPlayer teleportingPlayer = Sponge.server().player(this.toTeleport).orElse(null);
        final ServerPlayer targetPlayer = Sponge.server().player(this.target).orElse(null);
        @Nullable final User source = Sponge.server().userManager().load(this.requester).join().orElse(null);
        final Audience receiver = source != null ? source.player().map(x -> (Audience) x).orElseGet(Sponge::systemSubject) : Sponge.systemSubject();
        if (teleportingPlayer != null && targetPlayer != null) {
            // If safe, get the teleport mode
            final INucleusLocationService tpHandler = this.serviceCollection.teleportService();
            try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
                if (source == null) {
                    frame.pushCause(Sponge.systemSubject());
                } else {
                    frame.pushCause(source);
                }

                final TeleportResult result = tpHandler.teleportPlayerSmart(
                        teleportingPlayer,
                        this.requestLocation == null ? targetPlayer.serverLocation() : this.requestLocation,
                        this.rotation,
                        false,
                        this.safe,
                        TeleportScanners.NO_SCAN.get()
                );

                if (!result.isSuccessful()) {
                    if (!this.silentSource) {
                        this.serviceCollection.messageProvider()
                                .sendMessageTo(receiver, result == TeleportResult.FAIL_NO_LOCATION ?
                                        "teleport.nosafe" : "teleport.cancelled");
                    }

                    this.onCancel();
                    return;
                }

                if (!this.toTeleport.equals(this.requester) && !this.silentSource) {
                    this.serviceCollection.messageProvider()
                        .sendMessageTo(receiver, "teleport.success.source", teleportingPlayer.name(), targetPlayer.name());
                }

                this.serviceCollection.messageProvider().sendMessageTo(teleportingPlayer, "teleport.to.success", targetPlayer.name());
                if (!this.silentTarget) {
                    this.serviceCollection.messageProvider().sendMessageTo(targetPlayer,"teleport.from.success", teleportingPlayer.name());
                }

                if (this.successCallback != null && source != null) {
                    source.player().ifPresent(this.successCallback);
                }
            }
        } else {
            if (!this.silentSource) {
                this.serviceCollection.messageProvider().sendMessageTo(receiver, "teleport.fail.offline");
            }

            this.onCancel();
        }
    }
}
