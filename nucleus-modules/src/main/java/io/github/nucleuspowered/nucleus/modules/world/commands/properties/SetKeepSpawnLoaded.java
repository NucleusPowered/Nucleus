/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.properties;

import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.modules.world.commands.WorldCommand;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

@NonnullByDefault
@Command(
        aliases = {"setkeepspawnloaded"},
        basePermission = WorldPermissions.BASE_WORLD_SETKEEPSPAWNLOADED,
        commandDescriptionKey = "world.setkeepspawnloaded",
        parentCommand = WorldCommand.class
)
public class SetKeepSpawnLoaded extends AbstractPropertiesSetCommand {

    public SetKeepSpawnLoaded() {
        super("keep spawn loaded");
    }

    @Override protected void setter(WorldProperties worldProperties, boolean set)
            throws CommandException {
        worldProperties.setKeepSpawnLoaded(set);
    }
}
