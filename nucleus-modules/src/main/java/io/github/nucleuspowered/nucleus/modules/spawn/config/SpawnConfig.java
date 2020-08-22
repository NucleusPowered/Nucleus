/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.List;

@ConfigSerializable
public class SpawnConfig {

    @Setting(value = "spawn-on-login", comment = "config.spawn.onlogin")
    private boolean spawnOnLogin = false;

    @Setting(value = "use-safe-spawn", comment = "config.spawn.safe")
    private boolean safeTeleport = true;

    @Setting(value = "force-first-spawn", comment = "config.spawn.forcefirstspawn")
    private boolean forceFirstSpawn = false;

    @Setting(value = "global-spawn", comment = "config.spawn.global.base")
    private GlobalSpawnConfig globalSpawn = new GlobalSpawnConfig();

    @Setting(value = "affect-bed-spawn", comment = "config.spawn.bedspawn")
    private boolean redirectBedSpawn = true;

    @Setting(value = "spawn-on-login-exempt-worlds", comment = "config.spawn.onloginsameworld")
    private List<String> spawnOnLoginExemptWorld = Lists.newArrayList();

    public boolean isSpawnOnLogin() {
        return this.spawnOnLogin;
    }

    public boolean isSafeTeleport() {
        return this.safeTeleport;
    }

    public boolean isForceFirstSpawn() {
        return this.forceFirstSpawn;
    }

    public GlobalSpawnConfig getGlobalSpawn() {
        return this.globalSpawn;
    }

    public boolean isRedirectBedSpawn() {
        return this.redirectBedSpawn;
    }

    public List<String> getOnLoginExemptWorlds() {
        return this.spawnOnLoginExemptWorld == null ? ImmutableList.of() : this.spawnOnLoginExemptWorld;
    }
}
