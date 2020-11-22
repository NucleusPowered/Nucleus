/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.helpers;

import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.math.vector.Vector3d;

import java.util.Objects;
import java.util.Optional;

public final class SpawnHelper {

    private SpawnHelper() {}

    public static Tuple<ServerLocation, Vector3d> getSpawn(@NonNull final WorldProperties wp, @Nullable final ServerPlayer player,
            final ICommandContext context) throws CommandException {
        final Optional<ServerWorld> ow = wp.getWorld();

        if (!ow.isPresent()) {
            throw context.createException("command.spawn.noworld");
        }

        final ResourceKey worldKey = Objects.requireNonNull(wp, "WorldProperties").getKey();
        return new Tuple<>(
                ServerLocation.of(worldKey, wp.getSpawnPosition().toDouble().add(0.5, 0, 0.5)),
                context.getServiceCollection()
                        .storageManager()
                        .getWorldService()
                        .getOrNewOnThread(worldKey)
                        .get(SpawnKeys.WORLD_SPAWN_ROTATION)
                        .orElseGet(() -> player == null ? new Vector3d(0, 0, 0) : player.getRotation()));
    }
}
