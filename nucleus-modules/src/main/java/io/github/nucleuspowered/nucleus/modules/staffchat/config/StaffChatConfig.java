/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat.config;

import io.github.nucleuspowered.neutrino.annotations.Default;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class StaffChatConfig {

    @Setting(value = "include-standard-chat-formatting", comment = "config.staffchat.includestd")
    private boolean includeStandardChatFormatting = false;

    @Setting(value = "message-template", comment = "config.staffchat.template")
    @Default(value = "&b[STAFF] &r{{displayname}}&b: ", saveDefaultIfNull = true)
    private NucleusTextTemplateImpl messageTemplate;

    @Setting(value = "message-colour", comment = "config.staffchat.colour")
    private String messageColour = "b";

    public NucleusTextTemplateImpl getMessageTemplate() {
        return this.messageTemplate;
    }

    public String getMessageColour() {
        if (this.messageColour.isEmpty() || !this.messageColour.matches("^[0-9a-f]")) {
            return "b";
        }

        return this.messageColour.substring(0, 1);
    }

    public boolean isIncludeStandardChatFormatting() {
        return this.includeStandardChatFormatting;
    }
}
