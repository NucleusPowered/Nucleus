/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal;

import io.github.nucleuspowered.nucleus.mixins.config.NucleusMixinConfig;

public class MixinConfigProxy {

    public NucleusMixinConfig get() {
        return NucleusMixinConfig.INSTANCE;
    }
}
