/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.config;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.core.configurate.typeserialisers.NamedLocationSerialiser;
import io.github.nucleuspowered.nucleus.modules.warp.data.NucleusWarp;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public final class WarpSerialiser implements TypeSerializer<Warp> {

    public static final WarpSerialiser INSTANCE = new WarpSerialiser();

    private WarpSerialiser() {
        NamedLocationSerialiser.register(TypeToken.get(Warp.class), this);
    }

    @Override
    public Warp deserialize(@NonNull final Type type, @NonNull final ConfigurationNode value) throws SerializationException {
        final String desc = value.node("description").getString();
        Component res = null;
        if (desc != null) {
            res = GsonComponentSerializer.gson().deserialize(desc);
        }

        return new NucleusWarp(
                value.node("category").getString(),
                value.node("cost").getDouble(0d),
                res,
                NamedLocationSerialiser.getWorldResourceKey(value),
                NamedLocationSerialiser.getPosition(value),
                NamedLocationSerialiser.getRotation(value),
                NamedLocationSerialiser.getName(value)
        );
    }

    @Override
    public void serialize(@NonNull final Type type, @Nullable final Warp obj, @NonNull final ConfigurationNode value) throws SerializationException {
        if (obj == null) {
            return;
        }
        NamedLocationSerialiser.serializeLocation(obj, value);
        if (obj.getCategory().isPresent()) {
            value.node("category").set(obj.getCategory().get());
        }
        if (obj.getCost().isPresent()) {
            value.node("cost").set(obj.getCost().get());
        }
    }
}
