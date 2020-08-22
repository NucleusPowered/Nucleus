/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kick.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class KickConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<KickConfig> {

    public KickConfigAdapter() {
        super(KickConfig.class);
    }

}
