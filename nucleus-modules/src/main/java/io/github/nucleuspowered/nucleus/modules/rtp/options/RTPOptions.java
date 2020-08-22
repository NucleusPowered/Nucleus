/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.options;

import com.google.common.collect.ImmutableSet;
import io.github.nucleuspowered.nucleus.api.module.rtp.NucleusRTPService;
import io.github.nucleuspowered.nucleus.modules.rtp.config.RTPConfig;
import org.spongepowered.api.world.biome.BiomeType;

import java.util.Set;

import javax.annotation.Nullable;

public class RTPOptions implements NucleusRTPService.RTPOptions {

    private final int min;
    private final int max;
    private final int minHeight;
    private final int maxHeight;
    private final ImmutableSet<BiomeType> prohibitedBiomes;

    public RTPOptions(RTPConfig config, @Nullable String worldName) {
        this.min = config.getMinRadius(worldName);
        this.max = config.getRadius(worldName);
        this.maxHeight = config.getMaxY(worldName);
        this.minHeight = config.getMinY(worldName);
        this.prohibitedBiomes = config.getProhibitedBiomes();
    }

    RTPOptions(RTPOptionsBuilder builder) {
        this.min = builder.min;
        this.max = builder.max;
        this.minHeight = builder.minheight;
        this.maxHeight = builder.maxheight;
        this.prohibitedBiomes = ImmutableSet.copyOf(builder.prohibitedBiomes);
    }

    @Override
    public int maxRadius() {
        return this.max;
    }

    @Override
    public int minRadius() {
        return this.min;
    }

    @Override public int minHeight() {
        return this.minHeight;
    }

    @Override public int maxHeight() {
        return this.maxHeight;
    }

    @Override public Set<BiomeType> prohibitedBiomes() {
        return this.prohibitedBiomes;
    }
}
