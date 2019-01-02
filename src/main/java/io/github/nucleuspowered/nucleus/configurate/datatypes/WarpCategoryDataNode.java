/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate.datatypes;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import javax.annotation.Nullable;
import java.util.Optional;

@ConfigSerializable
public class WarpCategoryDataNode {

    public WarpCategoryDataNode() {
    }

    public WarpCategoryDataNode(@Nullable String displayName, @Nullable String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @Setting
    @Nullable
    private String displayName = null;

    @Setting
    @Nullable
    private String description = null;

    public Optional<Text> getDisplayName() {
        if (this.displayName == null) {
            return Optional.empty();
        }
        return Optional.of(TextSerializers.JSON.deserialize(this.displayName));
    }

    public void setDisplayName(@Nullable Text displayName) {
        this.displayName = displayName == null ? null : TextSerializers.JSON.serialize(displayName);
    }

    public Optional<Text> getDescription() {
        if (this.description == null) {
            return Optional.empty();
        }
        return Optional.of(TextSerializers.JSON.deserialize(this.description));
    }

    public void setDescription(@Nullable Text description) {
        this.description = description == null ? null : TextSerializers.JSON.serialize(description);
    }
}
