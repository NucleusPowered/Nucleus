/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.properties;

import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.modules.world.commands.WorldCommand;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

@NonnullByDefault
@Command(
        aliases = {"setpvpenabled", "setpvp"},
        basePermission = WorldPermissions.BASE_WORLD_SETPVPENABLED,
        commandDescriptionKey = "world.setpvpenabled",
        parentCommand = WorldCommand.class
)
public class SetPvpEnabled extends AbstractPropertiesSetCommand {

    public SetPvpEnabled() {
        super("pvp");
    }

    @Override protected void setter(WorldProperties worldProperties, boolean set) {
        worldProperties.setPVPEnabled(set);
    }
}
