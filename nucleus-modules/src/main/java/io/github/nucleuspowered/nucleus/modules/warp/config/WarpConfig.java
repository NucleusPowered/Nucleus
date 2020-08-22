/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class WarpConfig {

    @Setting(value = "default-warp-cost", comment = "config.warps.cost")
    private double defaultWarpCost = 0;

    @Setting(value = "separate-permissions", comment = "config.warps.separate")
    private boolean separatePermissions = false;

    @Setting(value = "use-safe-warp", comment = "config.warps.safe")
    private boolean safeTeleport = true;

    @Setting(value = "list-warps-by-category", comment = "config.warps.categories")
    private boolean categoriseWarps = false;

    @Setting(value = "default-category-name")
    private String defaultName = "Uncategorised";

    @Setting(value = "show-warp-description-in-list", comment = "config.warps.descinlist")
    private boolean descriptionInList = false;

    public boolean isSeparatePermissions() {
        return this.separatePermissions;
    }

    public double getDefaultWarpCost() {
        return Math.max(0, this.defaultWarpCost);
    }

    public boolean isSafeTeleport() {
        return this.safeTeleport;
    }

    public boolean isCategoriseWarps() {
        return this.categoriseWarps;
    }

    public String getDefaultName() {
        return this.defaultName == null || this.defaultName.isEmpty() ? "Uncategorised" : this.defaultName;
    }

    public boolean isDescriptionInList() {
        return this.descriptionInList;
    }
}
