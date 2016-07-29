/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban.config;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.internal.qsml.NucleusConfigAdapter;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class BanConfigAdapter extends NucleusConfigAdapter<BanConfig> {
    @Override
    protected BanConfig getDefaultObject() {
        return new BanConfig();
    }

    @Override
    protected BanConfig convertFromConfigurateNode(ConfigurationNode node) throws ObjectMappingException {
        return node.getValue(TypeToken.of(BanConfig.class), new BanConfig());
    }

    @Override
    protected ConfigurationNode insertIntoConfigurateNode(BanConfig data) throws ObjectMappingException {
        return SimpleCommentedConfigurationNode.root().setValue(TypeToken.of(BanConfig.class), data);
    }
}
