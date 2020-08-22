/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class CoreConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<CoreConfig> {

    public CoreConfigAdapter() {
        super(CoreConfig.class);
    }
}
