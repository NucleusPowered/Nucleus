/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.configurate.typeserialisers;

import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.core.datatypes.NucleusNamedLocation;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.math.vector.Vector3d;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NamedLocationSerialiser implements TypeSerializer<NamedLocation> {

    private static final Map<TypeToken<? extends NamedLocation>, TypeSerializer<? extends NamedLocation>> providers = new HashMap<>();

    public static <T extends NamedLocation> void register(final TypeToken<T> clazz, final TypeSerializer<T> typeSerializer) {
        if (!NamedLocationSerialiser.providers.containsKey(clazz)) {
            NamedLocationSerialiser.providers.put(clazz, typeSerializer);
        }
    }

    @Nullable
    @Override
    public NamedLocation deserialize(@NonNull final Type type, @NonNull final ConfigurationNode value) throws SerializationException {
        final Vector3d pos = getPosition(value);
        final Vector3d rot = getRotation(value);

        return new NucleusNamedLocation(
                NamedLocationSerialiser.getName(value),
                NamedLocationSerialiser.getWorldResourceKey(value),
                pos,
                rot
        );
    }

    @Override
    public void serialize(@NonNull final Type type, @Nullable final NamedLocation obj, @NonNull final ConfigurationNode value) throws SerializationException {
        if (obj == null) {
            return;
        }

        NamedLocationSerialiser.serializeLocation(obj, value);
    }

    public static String getName(final ConfigurationNode value) {
        return value.node("name").getString(String.valueOf(value.key()));
    }

    @SuppressWarnings("deprecation")
    public static ResourceKey convertUUID(final UUID value, final SerializationException ex) throws SerializationException {
        return Sponge.server().worldManager().worldKey(value)
                .orElseThrow(() -> ex);
    }

    public static ResourceKey getWorldResourceKey(final ConfigurationNode value) throws SerializationException {
        try {
            return value.node("world").get(TypeTokens.RESOURCE_KEY);
        } catch (final SerializationException e) {
            final UUID uuid = value.node("world").get(TypeTokens.UUID);
            return NamedLocationSerialiser.convertUUID(uuid, e);
        }
    }

    public static Vector3d getPosition(final ConfigurationNode value) {
        return new Vector3d(
                value.node("x").getDouble(),
                value.node("y").getDouble(),
                value.node("z").getDouble()
        );
    }

    public static Vector3d getRotation(final ConfigurationNode value) {
        return new Vector3d(
                value.node("rotx").getDouble(),
                value.node("roty").getDouble(),
                value.node("rotz").getDouble()
        );
    }

    public static void serializeLocation(final NamedLocation obj, final ConfigurationNode value) throws SerializationException {
        value.node("world").set(TypeTokens.RESOURCE_KEY, obj.getWorldResourceKey());

        value.node("x").set(obj.getPosition().x());
        value.node("y").set(obj.getPosition().y());
        value.node("z").set(obj.getPosition().z());

        value.node("rotx").set(obj.getRotation().x());
        value.node("roty").set(obj.getRotation().y());
        value.node("rotz").set(obj.getRotation().z());

        value.node("name").set(obj.getName());
    }
}
