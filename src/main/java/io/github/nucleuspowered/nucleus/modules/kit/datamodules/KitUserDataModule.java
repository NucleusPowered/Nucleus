/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.datamodules;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.IUserDataModule;

import java.time.Instant;
import java.util.Map;

public class KitUserDataModule implements IUserDataModule {

    private Map<String, Long> kitLastUsedTime = Maps.newHashMap();

    public Instant getLastRedeemedTime(String name) {
        if (!this.kitLastUsedTime.containsKey(name.toLowerCase())) {
            return null;
        }

        return Instant.ofEpochSecond(this.kitLastUsedTime.get(name.toLowerCase()));
    }

    public void addKitLastUsedTime(String kitName, Instant lastTime) {
        this.kitLastUsedTime.put(kitName.toLowerCase(), lastTime.getEpochSecond());
    }

    public void removeKitLastUsedTime(String kitName) {
        this.kitLastUsedTime.remove(kitName.toLowerCase());
    }
}
