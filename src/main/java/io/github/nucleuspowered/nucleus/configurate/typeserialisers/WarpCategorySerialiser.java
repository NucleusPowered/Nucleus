/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate.typeserialisers;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.api.nucleusdata.WarpCategory;
import io.github.nucleuspowered.nucleus.modules.warp.data.WarpCategoryData;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

public class WarpCategorySerialiser implements TypeSerializer<WarpCategory>  {

    @Nullable
    @Override
    public WarpCategory deserialize(@NonNull TypeToken<?> type, @NonNull ConfigurationNode value) {
        @Nullable String description = value.getNode("description").getString();
        @Nullable String displayName = value.getNode("displayName").getString();
        return new WarpCategoryData(
                String.valueOf(value.getKey()),
                displayName == null ? Text.of(value.getKey()) : TextSerializers.JSON.deserialize(displayName),
                description == null ? null : TextSerializers.JSON.deserialize(description)
        );
    }

    @Override
    public void serialize(@NonNull TypeToken<?> type, @Nullable WarpCategory obj, @NonNull ConfigurationNode value) {
        if (obj == null) {
            return;
        }
        obj.getDescription().ifPresent(x -> value.getNode("description").setValue(TextSerializers.JSON.serialize(x)));
        value.getNode("displayName").setValue(TextSerializers.JSON.serialize(obj.getDisplayName()));
    }
}
