/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connection.config;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.internal.qsml.NucleusConfigAdapter;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class ConnectionConfigAdapter extends NucleusConfigAdapter<ConnectionConfig> {
    @Override
    protected ConnectionConfig getDefaultObject() {
        return new ConnectionConfig();
    }

    @Override
    protected ConnectionConfig convertFromConfigurateNode(ConfigurationNode node) throws ObjectMappingException {
        return node.getValue(TypeToken.of(ConnectionConfig.class), new ConnectionConfig());
    }

    @Override
    protected ConfigurationNode insertIntoConfigurateNode(ConnectionConfig data) throws ObjectMappingException {
        return SimpleCommentedConfigurationNode.root().setValue(TypeToken.of(ConnectionConfig.class), data);
    }
}
