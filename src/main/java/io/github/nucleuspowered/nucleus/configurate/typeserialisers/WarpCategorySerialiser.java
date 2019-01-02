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

    private static final String DESCRIPTION_ID = "description";
    private static final String DISPLAY_NAME_ID = "displayName";

    @Nullable
    @Override
    public WarpCategory deserialize(@NonNull TypeToken<?> type, @NonNull ConfigurationNode value) {
        String description = value.getNode(DESCRIPTION_ID).getString();
        String displayName = value.getNode(DISPLAY_NAME_ID).getString();
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
        obj.getDescription().ifPresent(x -> value.getNode(DESCRIPTION_ID).setValue(TextSerializers.JSON.serialize(x)));
        value.getNode(DISPLAY_NAME_ID).setValue(TextSerializers.JSON.serialize(obj.getDisplayName()));
    }
}
