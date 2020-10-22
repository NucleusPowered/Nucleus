/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate.typeserialisers;

import com.google.common.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.modules.warp.data.WarpData;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

public class WarpSerialiser implements TypeSerializer<Warp> {

    private final Logger logger;

    public WarpSerialiser(final Logger logger) {
        this.logger = logger;
    }

    @Nullable
    @Override
    public Warp deserialize(@NonNull TypeToken<?> type, @NonNull ConfigurationNode value) throws ObjectMappingException {
        String desc = value.getNode("description").getString();
        Text res = null;
        if (desc != null) {
            try {
                res = TextSerializers.JSON.deserialize(desc);
            } catch (final RuntimeException ex) {
                this.logger.error("Could not deserialise description for warp {}, skipping description...", value.getKey(), ex);
            }
        }

        return new WarpData(
                value.getNode("category").getString(),
                value.getNode("cost").getDouble(0d),
                res,
                NamedLocationSerialiser.getWorldUUID(value),
                NamedLocationSerialiser.getPosition(value),
                NamedLocationSerialiser.getRotation(value),
                NamedLocationSerialiser.getName(value)
        );
    }

    @Override
    public void serialize(@NonNull TypeToken<?> type, @Nullable Warp obj, @NonNull ConfigurationNode value) throws ObjectMappingException {
        if (obj == null) {
            return;
        }
        NamedLocationSerialiser.serializeLocation(obj, value);
        obj.getCategory().ifPresent(x -> value.getNode("category").setValue(x));
        obj.getCost().ifPresent(x -> value.getNode("cost").setValue(x));
    }
}
