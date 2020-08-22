/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.lists;

import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.modules.world.commands.WorldCommand;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.WorldArchetype;

@NonnullByDefault
@Command(
        aliases = {"presets", "listpresets"},
        basePermission = WorldPermissions.BASE_WORLD_CREATE,
        commandDescriptionKey = "world.presets",
        parentCommand = WorldCommand.class
)
public class AvailablePresetsCommand extends AvailableBaseCommand {

    public AvailablePresetsCommand() {
        super(WorldArchetype.class, "command.world.presets.title");
    }

}
