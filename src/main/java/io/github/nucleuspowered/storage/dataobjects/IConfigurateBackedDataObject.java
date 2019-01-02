/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataobjects;

import ninja.leaping.configurate.ConfigurationNode;

public interface IConfigurateBackedDataObject extends IDataObject {

    ConfigurationNode getBackingNode();

    void setBackingNode(ConfigurationNode node);
}
