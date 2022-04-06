/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.config;

import io.github.nucleuspowered.nucleus.core.Registry;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class StorageConfig {

    @Setting("user-data")
    private String userData = Registry.Keys.FLAT_FILE_STORAGE_KEY.asString();

    @Setting("world-data")
    private String worldData = Registry.Keys.FLAT_FILE_STORAGE_KEY.asString();

    @Setting("general-data")
    private String generalData = Registry.Keys.FLAT_FILE_STORAGE_KEY.asString();

    public String getUserData() {
        return this.userData;
    }

    public String getWorldData() {
        return this.worldData;
    }

    public String getGeneralData() {
        return this.generalData;
    }
}

