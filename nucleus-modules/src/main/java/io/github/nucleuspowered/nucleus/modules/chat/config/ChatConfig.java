/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chat.config;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.neutrino.annotations.Default;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.Map;

@ConfigSerializable
public class ChatConfig {

    @Setting(value = "modify-chat", comment = "config.chat.modify")
    private boolean modifychat = true;

    @Setting(value = "templates")
    private TemplateConfig templates = new TemplateConfig();

    @Setting(value = "modify-main-message", comment = "config.chat.main")
    private boolean modifyMainMessage = true;

    @Setting(value = "overwrite-early-prefixes", comment = "config.chat.includeprefix")
    private boolean overwriteEarlyPrefixes = false;

    @Setting(value = "try-to-remove-minecraft-prefix", comment = "config.chat.hardertoremove")
    private boolean tryRemoveMinecraftPrefix = true;

    @Setting(value = "overwrite-early-suffixes", comment = "config.chat.includesuffix")
    private boolean overwriteEarlySuffixes = false;

    @Setting(value = "check-body-for-minecraft-prefix", comment = "config.chat.checkbody")
    private boolean checkBody = false;

    @Setting(value = "remove-link-underlines", comment = "config.chat.removeunderlines")
    private boolean removeBlueUnderline = true;

    @Setting(value = "me-prefix", comment = "config.chat.meprefix")
    @Default(value = "&7* {{displayName}} ", saveDefaultIfNull = true)
    private NucleusTextTemplateImpl mePrefix;

    public NucleusTextTemplateImpl getMePrefix() {
        return this.mePrefix;
    }

    public boolean isCheckBody() {
        return this.checkBody;
    }

    public boolean isModifychat() {
        return this.modifychat;
    }

    public boolean isUseGroupTemplates() {
        return this.templates.isUseGroupTemplates();
    }

    public ChatTemplateConfig getDefaultTemplate() {
        return this.templates.getDefaultTemplate();
    }

    public Map<String, ChatTemplateConfig> getGroupTemplates() {
        return ImmutableMap.copyOf(this.templates.getGroupTemplates());
    }

    public boolean isModifyMainMessage() {
        return this.modifyMainMessage;
    }

    public boolean isOverwriteEarlyPrefixes() {
        return this.overwriteEarlyPrefixes;
    }

    public boolean isOverwriteEarlySuffixes() {
        return this.overwriteEarlySuffixes;
    }

    public boolean isRemoveBlueUnderline() {
        return this.removeBlueUnderline;
    }

    public boolean isTryRemoveMinecraftPrefix() {
        return tryRemoveMinecraftPrefix;
    }

}
