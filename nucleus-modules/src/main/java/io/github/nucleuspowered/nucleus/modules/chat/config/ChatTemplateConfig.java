/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chat.config;

import io.github.nucleuspowered.neutrino.annotations.Default;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ChatTemplateConfig {

    @Setting(value = "default-chatcolour", comment = "config.chat.template.chatcolour")
    private String chatcolour = "";

    @Setting(value = "default-chatstyle", comment = "config.chat.template.chatstyle")
    private String chatstyle = "";

    @Setting(value = "default-namecolour", comment = "config.chat.template.namecolour")
    private String namecolour = "";

    @Setting(value = "default-namestyle", comment = "config.chat.template.namestyle")
    private String namestyle = "";

    @Setting(comment = "config.chat.template.prefix")
    @Default(value = "{{prefix:s}}{{displayname}}{{suffix}}&f: ", saveDefaultIfNull = true)
    private NucleusTextTemplateImpl prefix;

    @Setting(comment = "config.chat.template.suffix")
    private NucleusTextTemplateImpl suffix = NucleusTextTemplateImpl.Empty.INSTANCE;

    public NucleusTextTemplateImpl getPrefix() {
        return this.prefix;
    }

    public NucleusTextTemplateImpl getSuffix() {
        return this.suffix;
    }

    public String getChatcolour() {
        return this.chatcolour;
    }

    public String getChatstyle() {
        return this.chatstyle;
    }

    public String getNamecolour() {
        return this.namecolour;
    }

    public String getNamestyle() {
        return this.namestyle;
    }
}
