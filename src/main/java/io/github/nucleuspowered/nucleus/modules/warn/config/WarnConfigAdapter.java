/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.config;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.internal.qsml.NucleusConfigAdapter;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class WarnConfigAdapter extends NucleusConfigAdapter<WarnConfig> {
    @Override
    protected WarnConfig getDefaultObject() {
        return new WarnConfig();
    }

    @Override
    protected WarnConfig convertFromConfigurateNode(ConfigurationNode node) throws ObjectMappingException {
        return node.getValue(TypeToken.of(WarnConfig.class), new WarnConfig());
    }

    @Override
    protected ConfigurationNode insertIntoConfigurateNode(WarnConfig data) throws ObjectMappingException {
        return SimpleCommentedConfigurationNode.root().setValue(TypeToken.of(WarnConfig.class), data);
    }
}
