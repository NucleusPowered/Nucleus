/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class MuteConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<MuteConfig> {

    public MuteConfigAdapter() {
        super(MuteConfig.class);
    }
}
