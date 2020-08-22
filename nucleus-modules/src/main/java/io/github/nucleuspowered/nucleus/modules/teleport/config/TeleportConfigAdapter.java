/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class TeleportConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<TeleportConfig> {

    public TeleportConfigAdapter() {
        super(TeleportConfig.class);
    }
}
