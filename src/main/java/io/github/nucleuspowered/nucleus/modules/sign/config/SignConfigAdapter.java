/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.sign.config;

import io.github.nucleuspowered.nucleus.internal.qsml.NucleusConfigAdapter;

public class SignConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<SignConfig> {

    public SignConfigAdapter() {
        super(SignConfig.class);
    }
}
