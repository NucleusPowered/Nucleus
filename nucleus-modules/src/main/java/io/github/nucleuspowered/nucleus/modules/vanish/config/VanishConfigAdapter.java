/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class VanishConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<VanishConfig> {

    public VanishConfigAdapter() {
        super(VanishConfig.class);
    }
}
