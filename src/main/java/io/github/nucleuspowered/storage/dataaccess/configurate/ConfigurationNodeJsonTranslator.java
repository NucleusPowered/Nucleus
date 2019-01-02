/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataaccess.configurate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.nucleuspowered.nucleus.configurate.ConfigurateHelper;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.ValueType;

import java.util.List;
import java.util.Map;

public class ConfigurationNodeJsonTranslator {

    public static ConfigurationNodeJsonTranslator INSTANCE = new ConfigurationNodeJsonTranslator();

    private ConfigurationNodeJsonTranslator() {}

    public ConfigurationNode from(JsonObject object) {
        ConfigurationNode node = createNode();
        parseObject(object, node);
        return node;
    }

    // The node has map
    public JsonObject jsonFrom(ConfigurationNode node) {
        JsonObject object = new JsonObject();
        if (!node.hasMapChildren()) {
            // nope.
            return object;
        }

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.getChildrenMap().entrySet()) {
            ConfigurationNode value = entry.getValue();
            if (value.isVirtual()) {
                continue;
            }

            String key = String.valueOf(entry.getKey());
            JsonElement element = fromNode(value);

            if (element != null) {
                object.add(key, element);
            }
        }

        return object;
    }

    private JsonElement fromNode(ConfigurationNode value) {
        JsonElement element = null;
        if (value.getValueType() == ValueType.MAP) {
            element = jsonFrom(value);
        } else if (value.getValueType() == ValueType.LIST) {
            element = jsonFromList(value.getChildrenList());
        } else if (value.getValueType() == ValueType.SCALAR) {
            element = jsonFromScalar(value.getValue());
        }

        return element;
    }

    private JsonPrimitive jsonFromScalar(Object value) {
        JsonPrimitive primitive;
        if (value instanceof Number) {
            primitive = new JsonPrimitive((Number) value);
        } else if (value instanceof Boolean) {
            primitive = new JsonPrimitive((Boolean) value);
        } else {
            primitive = new JsonPrimitive(value.toString());
        }

        return primitive;
    }

    private JsonArray jsonFromList(List<? extends ConfigurationNode> listNode) {
        return listNode.stream().map(this::fromNode).collect(
                JsonArray::new,
                JsonArray::add,
                JsonArray::addAll
        );
    }

    private ConfigurationNode createNode() {
        return SimpleConfigurationNode.root(ConfigurateHelper.setOptions(ConfigurationOptions.defaults()));
    }

    private void parseArray(JsonArray array, ConfigurationNode node) {
        for (JsonElement element : array) {
            if (!element.isJsonNull()) {
                parseElement(element, node.getAppendedNode());
            }
        }
    }

    private void parseObject(JsonObject object, ConfigurationNode node) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement element = entry.getValue();
            parseElement(element, node.getNode(entry.getKey()));
        }
    }

    private void parsePrimitive(JsonPrimitive primitive, ConfigurationNode node) {
        if (primitive.isBoolean()) {
            node.setValue(primitive.getAsBoolean());
        } else if (primitive.isString()) {
            node.setValue(primitive.getAsString());
        } else if (primitive.isNumber()) {
            double d = primitive.getAsDouble();
            long l = primitive.getAsLong();
            if (d == l) {
                int i = primitive.getAsInt();
                if (i == l) {
                    node.setValue(i);
                } else {
                    node.setValue(l);
                }
            } else {
                node.setValue(d);
            }
        }
    }

    private void parseElement(JsonElement element, ConfigurationNode node) {
        if (element.isJsonObject()) {
            node.setValue(from(element.getAsJsonObject()));
        } else if (element.isJsonArray()) {
            parseArray(element.getAsJsonArray(), node);
        } else if (element.isJsonPrimitive()) {
            parsePrimitive(element.getAsJsonPrimitive(), node);
        }
    }

}
