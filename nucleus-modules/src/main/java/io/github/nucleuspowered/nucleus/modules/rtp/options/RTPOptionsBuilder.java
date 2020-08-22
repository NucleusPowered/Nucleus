/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.options;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.api.module.rtp.NucleusRTPService;
import org.spongepowered.api.world.biome.BiomeType;

import java.util.HashSet;
import java.util.Set;

public class RTPOptionsBuilder implements NucleusRTPService.RTPOptions.Builder {

    int max = 30000;
    int min = 0;
    int minheight = 1;
    int maxheight = 255;
    final Set<BiomeType> prohibitedBiomes = new HashSet<>();

    @Override public NucleusRTPService.RTPOptions.Builder setMaxRadius(int max) {
        Preconditions.checkArgument(max > 0, "Max must be positive");
        this.max = max;
        return this;
    }

    @Override public NucleusRTPService.RTPOptions.Builder setMinRadius(int min) {
        Preconditions.checkArgument(min >= 0, "Min cannot be negative");
        this.min = min;
        return this;
    }

    @Override public NucleusRTPService.RTPOptions.Builder setMinHeight(int min) throws IllegalArgumentException {
        Preconditions.checkArgument(min >= 0, "Min must be non-negative");
        this.minheight = min;
        return this;
    }

    @Override public NucleusRTPService.RTPOptions.Builder setMaxHeight(int max) throws IllegalArgumentException {
        Preconditions.checkArgument(min <= 255, "Max must be less than 255");
        this.maxheight = max;
        return this;
    }

    @Override public NucleusRTPService.RTPOptions.Builder prohibitedBiome(BiomeType biomeType) {
        this.prohibitedBiomes.add(Preconditions.checkNotNull(biomeType));
        return this;
    }

    @Override public NucleusRTPService.RTPOptions.Builder from(NucleusRTPService.RTPOptions options) {
        return setMinRadius(options.minRadius())
                .setMaxRadius(options.maxRadius())
                .setMaxHeight(options.maxHeight())
                .setMinHeight(options.minHeight());
    }

    @Override public NucleusRTPService.RTPOptions build() {
        Preconditions.checkState(this.min < this.max, "Minimum is bigger than maximum");
        Preconditions.checkState(this.minheight < this.maxheight, "Minimum height is bigger than maximum height");
        return new RTPOptions(this);
    }

    @Override public NucleusRTPService.RTPOptions.Builder reset() {
        this.max = 30000;
        this.min = 0;
        this.minheight = 1;
        this.maxheight = 255;
        this.prohibitedBiomes.clear();
        return this;
    }
}
