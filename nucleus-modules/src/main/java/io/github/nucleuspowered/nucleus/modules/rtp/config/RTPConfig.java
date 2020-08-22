/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.config;

import com.flowpowered.math.GenericMath;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.github.nucleuspowered.neutrino.annotations.ProcessSetting;
import io.github.nucleuspowered.nucleus.configurate.settingprocessor.LowercaseMapKeySettingProcessor;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.biome.BiomeTypes;
import org.spongepowered.api.world.storage.WorldProperties;
import uk.co.drnaylor.quickstart.config.NoMergeIfPresent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

@ConfigSerializable
public class RTPConfig {

    @Setting(value = "attempts", comment = "config.rtp.attempts")
    private int noOfAttempts = 10;

    @Setting(value = "radius", comment = "config.rtp.radius")
    private int radius = 30000;

    @Setting(value = "min-radius", comment = "config.rtp.minradius")
    private int minRadius = 0;

    @Setting(value = "minimum-y", comment = "config.rtp.min-y")
    private int minY = 0;

    @Setting(value = "maximum-y", comment = "config.rtp.max-y")
    private int maxY = 255;

    @Setting(value = "default-method", comment = "config.rtp.defaultmethod")
    private String defaultRTPKernel = "nucleus:default";

    @Setting(value = "per-world-permissions", comment = "config.rtp.perworldperms")
    private boolean perWorldPermissions = false;

    @Setting(value = "world-overrides", comment = "config.rtp.perworldsect")
    @ProcessSetting(LowercaseMapKeySettingProcessor.class)
    private Map<String, PerWorldRTPConfig> perWorldRTPConfigList = new HashMap<String, PerWorldRTPConfig>() {{
        put("example", new PerWorldRTPConfig());
    }};

    @Setting(value = "default-world", comment = "config.rtp.defaultworld")
    private String defaultWorld = "";

    @NoMergeIfPresent
    @Setting(value = "prohibited-biomes", comment = "config.rtp.prohibitedbiomes")
    private Set<String> prohibitedBiomes = Sets.newHashSet(
            BiomeTypes.OCEAN.getId(),
            BiomeTypes.DEEP_OCEAN.getId(),
            BiomeTypes.FROZEN_OCEAN.getId()
    );

    private ImmutableSet<BiomeType> lazyLoadProhbitedBiomes;

    public int getNoOfAttempts() {
        return this.noOfAttempts;
    }

    public Optional<PerWorldRTPConfig> get(@Nullable String worldName) {
        if (worldName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.perWorldRTPConfigList.get(worldName.toLowerCase()));
    }

    public int getMinRadius(@Nullable String worldName) {
        return get(worldName).map(x -> x.minRadius).orElse(this.minRadius);
    }

    public int getRadius(@Nullable String worldName) {
        return get(worldName).map(x -> x.radius).orElse(this.radius);
    }

    public int getMinY(@Nullable String worldName) {
        return get(worldName).map(x -> GenericMath.clamp(x.minY, 0, Math.min(255, x.maxY)))
                .orElseGet(() -> GenericMath.clamp(this.minY, 0, Math.min(255, this.maxY)));
    }

    public int getMaxY(@Nullable String worldName) {
        return get(worldName).map(x -> GenericMath.clamp(x.maxY, Math.max(0, x.minY), 255))
                .orElseGet(() -> GenericMath.clamp(this.maxY, Math.max(0, this.minY), 255));
    }

    public boolean isPerWorldPermissions() {
        return this.perWorldPermissions;
    }

    public Optional<WorldProperties> getDefaultWorld() {
        if (this.defaultWorld == null || this.defaultWorld.equalsIgnoreCase("")) {
            return Optional.empty();
        }

        return Sponge.getServer().getWorldProperties(this.defaultWorld).filter(WorldProperties::isEnabled);
    }

    public ImmutableSet<BiomeType> getProhibitedBiomes() {
        if (this.lazyLoadProhbitedBiomes == null) {
            this.lazyLoadProhbitedBiomes = this.prohibitedBiomes.stream()
                    .map(x -> x.contains(":") ? x : "minecraft:" + x)
                    .map(x -> Sponge.getRegistry().getType(BiomeType.class, x).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(ImmutableSet.toImmutableSet());
        }

        return this.lazyLoadProhbitedBiomes;
    }

    public String getDefaultRTPKernel() {
        return this.defaultRTPKernel;
    }

    @ConfigSerializable
    public static class PerWorldRTPConfig {
        @Setting(value = "radius")
        private int radius = 30000;

        @Setting(value = "min-radius")
        private int minRadius = 0;

        @Setting(value = "minimum-y")
        private int minY = 0;

        @Setting(value = "maximum-y")
        private int maxY = 255;

        @Setting(value = "default-method", comment = "config.rtp.defaultmethod")
        private String defaultRTPKernel = "nucleus:default";

        public String getDefaultRTPKernel() {
            return this.defaultRTPKernel;
        }
    }
}
