/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rules.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class RulesConfig {

    @Setting(value = "rules-title", comment = "config.rules.title")
    private String rulesTitle = "&6Server Rules";

    public String getRulesTitle() {
        return this.rulesTitle;
    }
}
