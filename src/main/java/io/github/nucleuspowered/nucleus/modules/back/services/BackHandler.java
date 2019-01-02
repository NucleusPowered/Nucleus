/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.services;

import io.github.nucleuspowered.nucleus.api.service.NucleusBackService;
import io.github.nucleuspowered.nucleus.internal.annotations.APIService;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.internal.pojo.UUIDTransform;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@APIService(NucleusBackService.class)
public class BackHandler implements NucleusBackService, ServiceBase {

    private final Map<UUID, UUIDTransform> lastLocation = new HashMap<>();
    private final Set<UUID> preventLogLastLocation = new HashSet<>();

    @Override
    public Optional<Transform<World>> getLastLocation(User user) {
        UUIDTransform transform = this.lastLocation.get(user.getUniqueId());
        if (transform != null) {
            return transform.getTransform();
        }

        return Optional.empty();
    }

    @Override
    public void setLastLocation(User user, Transform<World> location) {
        this.lastLocation.put(user.getUniqueId(), new UUIDTransform(location));
    }

    @Override
    public void removeLastLocation(User user) {
        this.lastLocation.remove(user.getUniqueId());
    }

    @Override
    public boolean isLoggingLastLocation(User user) {
        return !this.preventLogLastLocation.contains(user.getUniqueId());
    }

    @Override
    public void setLoggingLastLocation(User user, boolean log) {
        if (log) {
            this.preventLogLastLocation.remove(user.getUniqueId());
        } else {
            this.preventLogLastLocation.add(user.getUniqueId());
        }
    }

}
