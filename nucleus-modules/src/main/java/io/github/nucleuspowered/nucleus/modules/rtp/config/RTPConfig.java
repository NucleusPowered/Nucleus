/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.config;

import io.github.nucleuspowered.nucleus.core.services.interfaces.annotation.configuratehelper.LocalisedComment;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.math.GenericMath;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigSerializable
public class RTPConfig {

    @Setting(value = "attempts")
    @LocalisedComment("config.rtp.attempts")
    private int noOfAttempts = 10;

    @Setting(value = "radius")
    @LocalisedComment("config.rtp.radius")
    private int radius = 30000;

    @Setting(value = "min-radius")
    @LocalisedComment("config.rtp.minradius")
    private int minRadius = 0;

    @Setting(value = "minimum-y")
    @LocalisedComment("config.rtp.min-y")
    private int minY = 0;

    @Setting(value = "maximum-y")
    @LocalisedComment("config.rtp.max-y")
    private int maxY = 255;

    @Setting(value = "default-method")
    @LocalisedComment("config.rtp.defaultmethod")
    private String defaultRTPKernel = "nucleus:default";

    @Setting(value = "per-world-permissions")
    @LocalisedComment("config.rtp.perworldperms")
    private boolean perWorldPermissions = false;

    @Setting(value = "world-overrides")
    @LocalisedComment("config.rtp.perworldsect")
    private Map<String, PerWorldRTPConfig> perWorldRTPConfigList;

    @Setting(value = "default-world")
    @LocalisedComment("config.rtp.defaultworld")
    private String defaultWorld = "";

    @Setting(value = "prohibited-biomes")
    @LocalisedComment("config.rtp.prohibitedbiomes")
    private Set<String> prohibitedBiomes;

    public RTPConfig() {
        this.prohibitedBiomes = new HashSet<>();
        this.prohibitedBiomes.add("minecraft:ocean");
        this.prohibitedBiomes.add("minecraft:deep_ocean");
        this.prohibitedBiomes.add("minecraft:frozen_ocean");
        this.perWorldRTPConfigList = new HashMap<>();
        this.perWorldRTPConfigList.put("example", new PerWorldRTPConfig());
    }

    private transient Set<Biome> lazyLoadProhbitedBiomes;

    public int getNoOfAttempts() {
        return this.noOfAttempts;
    }

    public Optional<PerWorldRTPConfig> get(@Nullable final String worldName) {
        if (worldName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.perWorldRTPConfigList.get(worldName.toLowerCase()));
    }

    public int getMinRadius(@Nullable final String worldName) {
        return this.get(worldName).map(x -> x.minRadius).orElse(this.minRadius);
    }

    public int getRadius(@Nullable final String worldName) {
        return this.get(worldName).map(x -> x.radius).orElse(this.radius);
    }

    public int getMinY(@Nullable final String worldName) {
        return this.get(worldName).map(x -> GenericMath.clamp(x.minY, 0, Math.min(255, x.maxY)))
                .orElseGet(() -> GenericMath.clamp(this.minY, 0, Math.min(255, this.maxY)));
    }

    public int getMaxY(@Nullable final String worldName) {
        return this.get(worldName).map(x -> GenericMath.clamp(x.maxY, Math.max(0, x.minY), 255))
                .orElseGet(() -> GenericMath.clamp(this.maxY, Math.max(0, this.minY), 255));
    }

    public boolean isPerWorldPermissions() {
        return this.perWorldPermissions;
    }

    public Optional<ServerWorld> getDefaultWorld() {
        if (this.defaultWorld == null || this.defaultWorld.equalsIgnoreCase("")) {
            return Optional.empty();
        }

        return Sponge.server().worldManager().world(ResourceKey.resolve(this.defaultWorld));
    }

    public Set<Biome> getProhibitedBiomes() {
        if (this.lazyLoadProhbitedBiomes == null) {
            this.lazyLoadProhbitedBiomes = this.prohibitedBiomes.stream()
                    .map(ResourceKey::resolve)
                    .map(x -> Sponge.server().findRegistry(RegistryTypes.BIOME).flatMap(r -> (Optional<Biome>) r.findValue(x)).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
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

        @Setting(value = "default-method")
        @LocalisedComment("config.rtp.defaultmethod")
        private String defaultRTPKernel = "nucleus:default";

        public String getDefaultRTPKernel() {
            return this.defaultRTPKernel;
        }
    }
}
