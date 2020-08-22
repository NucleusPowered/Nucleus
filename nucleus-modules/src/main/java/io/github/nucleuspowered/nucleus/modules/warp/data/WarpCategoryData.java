/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.data;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.api.module.warp.data.WarpCategory;
import org.spongepowered.api.text.Text;

import java.util.Collection;
import java.util.Optional;

public class WarpCategoryData implements WarpCategory {

    public WarpCategoryData(String id, Text displayName, Text description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    private final String id;
    private final Text displayName;
    private final Text description;

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Text getDisplayName() {
        return this.displayName;
    }

    @Override
    public Optional<Text> getDescription() {
        return Optional.ofNullable(this.description);
    }

    @Override
    public Collection<Warp> getWarps() {
        return null;
    }
}
