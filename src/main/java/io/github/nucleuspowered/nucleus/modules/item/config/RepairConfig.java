/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.item.ItemType;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class RepairConfig {

    @Setting(value = "use-whitelist", comment = "config.item.repair.whitelist")
    private boolean useWhitelist = false;

    @Setting(value = "restrictions", comment = "config.item.repair.restrictions")
    private List<ItemType> restrictions = new ArrayList<>();

    public boolean isWhitelist() {
        return useWhitelist;
    }

    public List<ItemType> getRestrictions() {
        return restrictions;
    }
}
