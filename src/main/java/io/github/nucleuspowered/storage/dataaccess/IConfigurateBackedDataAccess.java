/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataaccess;

import com.google.gson.JsonObject;
import io.github.nucleuspowered.storage.dataaccess.configurate.ConfigurationNodeJsonTranslator;
import io.github.nucleuspowered.storage.dataobjects.IConfigurateBackedDataObject;
import ninja.leaping.configurate.ConfigurationNode;

@FunctionalInterface
public interface IConfigurateBackedDataAccess<R extends IConfigurateBackedDataObject> extends IDataAccess<R> {

    @Override
    default R fromJsonObject(JsonObject object) {
        // Get the ConfigNode from the JsonObject
        ConfigurationNode node = ConfigurationNodeJsonTranslator.INSTANCE.from(object);
        R obj = createNew();
        obj.setBackingNode(node);
        return obj;
    }

    @Override
    default JsonObject toJsonObject(R object) {
        ConfigurationNode node = object.getBackingNode();
        return ConfigurationNodeJsonTranslator.INSTANCE.jsonFrom(node);
    }

}
