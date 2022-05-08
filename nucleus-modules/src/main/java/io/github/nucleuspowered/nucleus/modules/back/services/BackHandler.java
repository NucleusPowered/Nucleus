/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.services;

import io.github.nucleuspowered.nucleus.api.module.back.NucleusBackService;
import io.github.nucleuspowered.nucleus.api.util.WorldTransform;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.core.util.WorldTransformImpl;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@APIService(NucleusBackService.class)
public class BackHandler implements NucleusBackService, ServiceBase {

    private final Map<UUID, WorldTransform> lastLocation = new HashMap<>();
    private final Set<UUID> preventLogLastLocation = new HashSet<>();

    @Override
    public Optional<WorldTransform> getLastLocation(final UUID uuid) {
        return Optional.ofNullable(this.lastLocation.get(uuid));
    }

    @Override
    public void setLastLocation(final UUID user, final ServerLocation location, final Vector3d rotation) {
        this.lastLocation.put(user, new WorldTransformImpl(location.position(), rotation, location.worldKey()));
    }

    @Override
    public void removeLastLocation(final UUID user) {
        this.lastLocation.remove(user);
    }

    @Override
    public boolean isLoggingLastLocation(final UUID user) {
        return !this.preventLogLastLocation.contains(user);
    }

    @Override
    public void setLoggingLastLocation(final UUID user, final boolean log) {
        if (log) {
            this.preventLogLastLocation.remove(user);
        } else {
            this.preventLogLastLocation.add(user);
        }
    }

}
