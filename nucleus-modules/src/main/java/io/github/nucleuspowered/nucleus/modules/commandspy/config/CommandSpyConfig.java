/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandspy.config;

import io.github.nucleuspowered.neutrino.annotations.Default;
import io.github.nucleuspowered.neutrino.annotations.ProcessSetting;
import io.github.nucleuspowered.neutrino.settingprocessor.RemoveFirstSlashIfExistsSettingProcessor;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class CommandSpyConfig {

    @Setting(comment = "config.commandspy.template")
    @Default(value = "&7[CS: {{name}}]: ", saveDefaultIfNull = true)
    private NucleusTextTemplateImpl prefix;

    // use-whitelist
    @Setting(value = "filter-is-whitelist", comment = "config.commandspy.usewhitelist")
    private boolean useWhitelist = true;

    // Was whitelisted-commands-to-spy-on
    // Removes the first "/" if it exists.
    @ProcessSetting(RemoveFirstSlashIfExistsSettingProcessor.class)
    @Setting(value = "command-filter", comment = "config.commandspy.filter")
    private List<String> commands = new ArrayList<>();

    public NucleusTextTemplateImpl getTemplate() {
        return this.prefix;
    }

    public List<String> getCommands() {
        return this.commands;
    }

    public boolean isUseWhitelist() {
        return this.useWhitelist;
    }
}
