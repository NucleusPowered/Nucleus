/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class MiscConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<MiscConfig> {

    public MiscConfigAdapter() {
        super(MiscConfig.class);
    }
}
