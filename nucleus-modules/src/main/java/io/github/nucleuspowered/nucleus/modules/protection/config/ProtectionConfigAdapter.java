/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.protection.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class ProtectionConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<ProtectionConfig> {

    public ProtectionConfigAdapter() {
        super(ProtectionConfig.class);
    }
}
