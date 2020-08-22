/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.notification.config;

import io.github.nucleuspowered.neutrino.annotations.Default;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class BroadcastConfig {

    @Setting
    @Default(value = "&a[Broadcast] ", saveDefaultIfNull = true)
    private NucleusTextTemplateImpl prefix;

    @Setting
    @Default(value = "", saveDefaultIfNull = true)
    private NucleusTextTemplateImpl suffix;

    public NucleusTextTemplateImpl getPrefix() {
        return this.prefix;
    }

    public NucleusTextTemplateImpl getSuffix() {
        return this.suffix;
    }
}
