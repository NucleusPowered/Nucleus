/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class NicknameConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<NicknameConfig> {

    public NicknameConfigAdapter() {
        super(NicknameConfig.class);
    }
}
