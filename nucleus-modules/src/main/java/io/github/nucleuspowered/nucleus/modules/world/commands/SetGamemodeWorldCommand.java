/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.ImprovedGameModeArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

@NonnullByDefault
@Command(
        aliases = {"setgamemode", "setgm", "gamemode", "gm"},
        basePermission = WorldPermissions.BASE_WORLD_SETGAMEMODE,
        commandDescriptionKey = "world.setgamemode",
        parentCommand = WorldCommand.class,
        associatedPermissions = WorldPermissions.WORLD_FORCE_GAMEMODE_OVERRIDE
)
public class SetGamemodeWorldCommand implements ICommandExecutor<CommandSource> {

    private final String gamemode = "gamemode";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.onlyOne(new ImprovedGameModeArgument(Text.of(this.gamemode), serviceCollection)),
                NucleusParameters.OPTIONAL_WORLD_PROPERTIES_ALL.get(serviceCollection)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        GameMode gamemodeInput = context.requireOne(this.gamemode, GameMode.class);
        WorldProperties worldProperties = context.getWorldPropertiesOrFromSelf(NucleusParameters.Keys.WORLD)
                .orElseThrow(() -> context.createException("command.world.player"));

        worldProperties.setGameMode(gamemodeInput);
        context.sendMessage("command.world.setgamemode.success",
            worldProperties.getWorldName(),
            Util.getTranslatableIfPresent(gamemodeInput));

        return context.successResult();
    }
}
