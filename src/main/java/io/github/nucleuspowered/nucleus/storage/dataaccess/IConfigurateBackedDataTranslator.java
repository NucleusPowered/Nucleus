/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.storage.dataaccess;

import com.google.gson.JsonObject;
import io.github.nucleuspowered.nucleus.storage.dataobjects.configurate.IConfigurateBackedDataObject;
import io.github.nucleuspowered.storage.dataaccess.IDataTranslator;
import ninja.leaping.configurate.ConfigurationNode;

@FunctionalInterface
public interface IConfigurateBackedDataTranslator<R extends IConfigurateBackedDataObject> extends IDataTranslator<R, JsonObject> {

    @Override
    default R fromDataAccessObject(JsonObject object) {
        // Get the ConfigNode from the JsonObject
        ConfigurationNode node = ConfigurationNodeJsonTranslator.INSTANCE.from(object);
        R obj = createNew();
        obj.setBackingNode(node);
        return obj;
    }

    @Override
    default JsonObject toDataAccessObject(R object) {
        ConfigurationNode node = object.getBackingNode();
        return ConfigurationNodeJsonTranslator.INSTANCE.jsonFrom(node);
    }

}
