/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.storage.dataobjects.modules;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.NamedLocation;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.internal.LocationData;
import io.github.nucleuspowered.storage.dataobjects.modules.IDataModule;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public abstract class LocationDataModule implements IDataModule {

    protected final BiFunction<String, LocationNode, NamedLocation> getLocationData = (s, l) ->
            new LocationData(s, l.getWorld(), l.getPosition(), l.getRotation());

    // Helper methods for warp based systems
    protected final boolean addLocation(String name, Location<World> loc, Vector3d rot, Map<String, LocationNode> m) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(loc);
        Preconditions.checkNotNull(rot);
        if (Util.getKeyIgnoreCase(m, name).isPresent()) {
            return false;
        }

        m.put(name, new LocationNode(loc, rot));
        return true;
    }

    protected final boolean removeLocation(String name, Map<String, LocationNode> m) {
        Optional<Map.Entry<String, LocationNode>> o = m.entrySet().stream().filter(k -> k.getKey().equalsIgnoreCase(name)).findFirst();
        return o.isPresent() && m.remove(o.get().getKey()) != null;
    }

    protected final <S extends LocationNode, T extends NamedLocation> Optional<T> get(Map<String, S> input, BiFunction<String, S, T> convert, String name) {
        return Util.getValueIgnoreCase(convert(input, convert), name);
    }

    protected final <S extends LocationNode, T extends NamedLocation> Map<String, T> convert(Map<String, S> input, BiFunction<String, S, T> convert) {
        return input.entrySet().stream()
                .map(x -> convert.apply(x.getKey(), x.getValue()))
                .filter(Objects::nonNull).collect(Collectors.toMap(T::getName, x -> x));
    }
}
