/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.config;

import io.github.nucleuspowered.neutrino.annotations.Default;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class KickOnStopConfig {
    @Setting(value = "enabled", comment = "config.core.kickonstop.flag")
    private boolean kickOnStop = false;

    @Default("Server closed")
    @Setting(value = "message", comment = "config.core.kickonstop.message")
    private NucleusTextTemplateImpl kickOnStopMessage;

    public boolean isKickOnStop() {
        return this.kickOnStop;
    }

    public NucleusTextTemplateImpl getKickOnStopMessage() {
        return this.kickOnStopMessage;
    }
}
