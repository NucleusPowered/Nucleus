/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.helpers;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class SpawnHelper {

    private SpawnHelper() {}

    public static Transform<World> getSpawn(@Nonnull WorldProperties wp, @Nullable Player player) throws ReturnMessageException {
        UUID worldUUID = Objects.requireNonNull(wp, "WorldProperties").getUniqueId();
        Optional<World> ow = Sponge.getServer().getWorld(worldUUID);

        if (!ow.isPresent()) {
            throw ReturnMessageException.fromKey("command.spawn.noworld");
        }

        return new Transform<>(ow.get(),
            wp.getSpawnPosition().toDouble().add(0.5, 0, 0.5),
                Nucleus.getNucleus()
                        .getStorageManager()
                        .getWorldService()
                        .getOrNewOnThread(worldUUID)
                        .get(SpawnKeys.WORLD_SPAWN_ROTATION)
                        .orElseGet(() -> player == null ? new Vector3d(0, 0, 0) : player.getRotation()));
    }
}
