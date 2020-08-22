/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

@ConfigSerializable
public class GlobalSpawnConfig {

    @Setting(value = "on-respawn", comment = "config.spawn.global.onrespawn")
    private boolean onRespawn = false;

    @Setting(value = "on-spawn-command", comment = "config.spawn.global.oncommand")
    private boolean onSpawnCommand = false;

    @Setting(value = "on-login", comment = "config.spawn.global.onlogin")
    private boolean onLogin = false;

    @Setting(value = "target-spawn-world", comment = "config.spawn.global.target")
    private String spawnWorld = "world";

    public boolean isOnRespawn() {
        return this.onRespawn;
    }

    public boolean isOnSpawnCommand() {
        return this.onSpawnCommand;
    }

    public boolean isOnLogin() {
        return this.onLogin;
    }

    public Optional<WorldProperties> getWorld() {
        Optional<WorldProperties> ow = Sponge.getServer().getWorldProperties(this.spawnWorld);
        if (!ow.isPresent()) {
            ow = Sponge.getServer().getWorldProperties(Sponge.getServer().getDefaultWorldName());
        }

        return ow;
    }
}
