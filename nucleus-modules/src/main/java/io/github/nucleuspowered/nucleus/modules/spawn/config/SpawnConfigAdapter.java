/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class SpawnConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<SpawnConfig> {

    public SpawnConfigAdapter() {
        super(SpawnConfig.class);
    }
}
