/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rules.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class RulesConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<RulesConfig> {

    public RulesConfigAdapter() {
        super(RulesConfig.class);
    }
}
