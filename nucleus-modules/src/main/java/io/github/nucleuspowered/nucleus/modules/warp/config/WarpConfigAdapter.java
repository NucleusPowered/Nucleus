/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class WarpConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<WarpConfig> {

    public WarpConfigAdapter() {
        super(WarpConfig.class);
    }
}
