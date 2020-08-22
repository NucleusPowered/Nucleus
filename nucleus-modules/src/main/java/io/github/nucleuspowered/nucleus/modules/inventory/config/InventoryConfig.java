/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory.config;

import io.github.nucleuspowered.neutrino.annotations.DoNotGenerate;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class InventoryConfig {

    // For use in testing, normally doesn't need to be exposed to users.
    @Setting("allow-invsee-on-self")
    @DoNotGenerate
    private boolean allowInvseeOnSelf = false;

    public boolean isAllowInvseeOnSelf() {
        return this.allowInvseeOnSelf;
    }
}
