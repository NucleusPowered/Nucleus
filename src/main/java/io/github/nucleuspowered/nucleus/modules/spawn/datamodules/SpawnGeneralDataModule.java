/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.datamodules;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.api.exceptions.NoSuchWorldException;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.dataservices.modular.DataKey;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.IGeneralDataModule;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

import javax.annotation.Nullable;

public class SpawnGeneralDataModule implements IGeneralDataModule {

    @DataKey("firstspawn")
    @Nullable
    private LocationNode firstspawn = null;

    public Optional<Transform<World>> getFirstSpawn() {
        if (this.firstspawn != null) {
            try {
                Transform<World> lwr =
                        new Transform<>(this.firstspawn.getLocation().getExtent(), this.firstspawn.getLocation().getPosition(), this.firstspawn.getRotation());
                return Optional.of(lwr);
            } catch (NoSuchWorldException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    public void setFirstSpawn(Location<World> location, Vector3d rot) {
        this.firstspawn = new LocationNode(location, rot);
    }

    public void removeFirstSpawn() {
        this.firstspawn = null;
    }
}
