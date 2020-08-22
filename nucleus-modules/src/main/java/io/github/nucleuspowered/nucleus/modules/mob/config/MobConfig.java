/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mob.config;

import com.google.common.collect.ImmutableMap;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.world.World;
import uk.co.drnaylor.quickstart.config.NoMergeIfPresent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ConfigSerializable
public class MobConfig {

    @Setting(value = "max-mobs-to-spawn", comment = "config.mobspawn.maxamt")
    private int maxMobsToSpawn = 20;

    @NoMergeIfPresent
    @Setting(value = "spawning-blocks", comment = "config.blockspawn.category")
    private Map<String, BlockSpawnsConfig> blockSpawnsConfig = new HashMap<String, BlockSpawnsConfig>() {{
        put("world", new BlockSpawnsConfig());
        put("DIM-1", new BlockSpawnsConfig());
        put("DIM1", new BlockSpawnsConfig());
    }};

    @Setting(value = "separate-mob-spawning-permissions", comment = "config.mobspawn.permob")
    private boolean perMobPermission = false;

    public int getMaxMobsToSpawn() {
        return Math.max(1, this.maxMobsToSpawn);
    }

    public Map<String, BlockSpawnsConfig> getBlockSpawnsConfig() {
        return ImmutableMap.copyOf(this.blockSpawnsConfig);
    }

    public Optional<BlockSpawnsConfig> getBlockSpawnsConfigForWorld(World world) {
        return Optional.ofNullable(this.blockSpawnsConfig.get(world.getName()));
    }

    public boolean isPerMobPermission() {
        return this.perMobPermission;
    }
}
