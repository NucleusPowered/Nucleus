/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.services;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.scaffold.task.CancellableTask;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.INucleusTeleportService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.World;

import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

public class TeleportTask implements CancellableTask {

    final UUID toTeleport;
    protected final UUID target;
    protected final double cost;
    protected final boolean safe;
    protected final int warmup;
    @Nullable private final UUID requester;
    private final boolean silentSource;
    private final boolean silentTarget;
    @Nullable private final Transform<World> requestLocation;
    @Nullable private Consumer<Player> successCallback;
    private final INucleusServiceCollection serviceCollection;

    public TeleportTask(
            INucleusServiceCollection serviceCollection,
            UUID toTeleport,
            UUID target,
            double cost,
            int warmup,
            boolean safe,
            boolean silentSource,
            boolean silentTarget,
            @Nullable Transform<World> requestLocation,
            @Nullable UUID requester,
            @Nullable Consumer<Player> successCallback) {
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
        this.serviceCollection = serviceCollection;
    }

    @Override
    public void onCancel() {
        PlayerTeleporterService.onCancel(this.serviceCollection, this.requester, this.toTeleport, this.cost);
    }

    @Override
    public void accept(Task task) {
        run();
    }

    public void run() {
        // Teleport them
        Player teleportingPlayer = Sponge.getServer().getPlayer(this.toTeleport).orElse(null);
        Player targetPlayer = Sponge.getServer().getPlayer(this.target).orElse(null);
        @Nullable User source = Util.getUserFromUUID(this.requester).orElse(null);
        CommandSource receiver = source != null && source.isOnline() ? source.getPlayer().get() : Sponge.getServer().getConsole();
        if (teleportingPlayer != null && targetPlayer != null) {
            // If safe, get the teleport mode
            INucleusTeleportService tpHandler = this.serviceCollection.teleportService();
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                if (source == null) {
                    frame.pushCause(Sponge.getServer().getConsole());
                } else {
                    frame.pushCause(source);
                }

                TeleportResult result = tpHandler.teleportPlayerSmart(
                        teleportingPlayer,
                        this.requestLocation == null ? targetPlayer.getTransform() : this.requestLocation,
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

                    onCancel();
                    return;
                }

                if (!this.toTeleport.equals(this.requester) && !this.silentSource) {
                    this.serviceCollection.messageProvider()
                        .sendMessageTo(receiver, "teleport.success.source", teleportingPlayer.getName(), targetPlayer.getName());
                }

                this.serviceCollection.messageProvider().sendMessageTo(teleportingPlayer, "teleport.to.success", targetPlayer.getName());
                if (!this.silentTarget) {
                    this.serviceCollection.messageProvider().sendMessageTo(targetPlayer,"teleport.from.success", teleportingPlayer.getName());
                }

                if (this.successCallback != null && source != null) {
                    source.getPlayer().ifPresent(x -> this.successCallback.accept(x));
                }
            }
        } else {
            if (!this.silentSource) {
                this.serviceCollection.messageProvider().sendMessageTo(receiver, "teleport.fail.offline");
            }

            onCancel();
        }
    }
}
