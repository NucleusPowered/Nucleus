/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate.typeserialisers;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.util.TimeValue;
import io.github.nucleuspowered.nucleus.util.WarningTimeList;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages serialization of a WarningTimeList
 */
public class WarningTimeListSerialiser implements TypeSerializer<WarningTimeList> {

    @Override
    public WarningTimeList deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        if (value.getValue() instanceof String) {
            String node = value.getString();
            if (node.trim().isEmpty()) {
                return new WarningTimeList();
            }
            if (!node.contains(",")) {
                throw new ObjectMappingException("Cannot find split character for WarningTimeList");
            }
            String[] split = node.split(",");
            List<TimeValue> timeValues = new ArrayList<>();
            for (String potentialTimeValue : split) {
                try {
                    TimeValue tv = TimeValue.fromString(potentialTimeValue);
                    timeValues.add(tv);
                } catch (Exception e1) { }
            }
            return new WarningTimeList(timeValues);
        }

        if (value.getValue() instanceof List) {
            List<String> asList = value.getList(new TypeToken<String>() {});
            List<TimeValue> timeValues = new ArrayList<>();
            asList.forEach((potentialTimeValue) -> {
                try {
                    TimeValue tv = TimeValue.fromString(potentialTimeValue);
                    timeValues.add(tv);
                } catch (Exception e1) { }
            });
            return new WarningTimeList(timeValues);
        }

        throw new ObjectMappingException("No valid type to parse warning list from");
    }

    @Override
    public void serialize(TypeToken<?> type, WarningTimeList obj, ConfigurationNode value) throws ObjectMappingException {
        List<String> toStore = obj.asList();
        if (toStore != null) {
            value.setValue(toStore);
        } else {
            value.setValue("");
        }
    }
}
