/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class WarpConfig {

    @Setting(value = "default-warp-cost", comment = "loc:config.warps.cost")
    private int defaultWarpCost = 0;

    @Setting(value = "separate-permissions", comment = "loc:config.warps.separate")
    private boolean separate_permissions = false;

    @Setting(value = "enable-warp-signs", comment = "loc:config.warps.signs")
    private boolean enable_warp_signs = true;

    public boolean isSeparatePermissions() {
        return separate_permissions;
    }

    public int getDefaultWarpCost() {
        return Math.max(0, defaultWarpCost);
    }

    public boolean areWarpSignsEnabled() {
        return enable_warp_signs;
    }
}
