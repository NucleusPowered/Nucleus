/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.config;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.internal.qsml.NucleusConfigAdapter;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class NoteConfigAdapter extends NucleusConfigAdapter<NoteConfig> {
    @Override
    protected NoteConfig getDefaultObject() {
        return new NoteConfig();
    }

    @Override
    protected NoteConfig convertFromConfigurateNode(ConfigurationNode node) throws ObjectMappingException {
        return node.getValue(TypeToken.of(NoteConfig.class), new NoteConfig());
    }

    @Override
    protected ConfigurationNode insertIntoConfigurateNode(NoteConfig data) throws ObjectMappingException {
        return SimpleCommentedConfigurationNode.root().setValue(TypeToken.of(NoteConfig.class), data);
    }
}