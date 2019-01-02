/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.datamodules;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.api.nucleusdata.NamedLocation;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.IGeneralDataModule;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.LocationDataModule;
import io.github.nucleuspowered.storage.dataobjects.modules.DataKey;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Map;
import java.util.Optional;

public class JailGeneralDataModule extends LocationDataModule implements IGeneralDataModule {

    @DataKey("jails")
    private Map<String, LocationNode> jails = Maps.newHashMap();

    public Optional<NamedLocation> getJailLocation(String name) {
        return get(this.jails, this.getLocationData, name);
    }

    public Map<String, NamedLocation> getJails() {
        return convert(this.jails, this.getLocationData);
    }

    public boolean addJail(String name, Location<World> loc, Vector3d rot) {
        return addLocation(name, loc, rot, this.jails);
    }

    public boolean removeJail(String name) {
        return removeLocation(name, this.jails);
    }

}
