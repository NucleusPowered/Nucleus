/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.commands.world;

import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.annotations.Permissions;
import io.github.essencepowered.essence.internal.annotations.RegisterCommand;
import io.github.essencepowered.essence.internal.permissions.SuggestedLevel;
import org.spongepowered.api.CatalogTypes;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

/**
 * Sets gamemode of world.
 *
 * Command Usage: /world setgamemode [gamemode] [world] 
 * Permission: essence.world.setgamemode.base
 */
@Permissions(root = "world", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"setgamemode", "setgm"}, subcommandOf = WorldCommand.class)
public class SetGamemodeWorldCommand extends CommandBase<CommandSource> {

    private final String gamemode = "gamemode";
    private final String world = "world";

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().description(Text.of("Set Gamemode World Command"))
                .arguments(
                        GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of(gamemode), CatalogTypes.GAME_MODE))),
                        GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.world(Text.of(world)))))
                .executor(this).build();
    }

    @Override
    public CommandResult executeCommand(final CommandSource src, CommandContext args) throws Exception {
        GameMode gamemodeInput = args.<GameMode>getOne(gamemode).get();
        Optional<WorldProperties> optWorldProperties = args.<WorldProperties>getOne(world);

        if (optWorldProperties.isPresent()) {
            optWorldProperties.get().setGameMode(gamemodeInput);
            src.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("command.world.setgamemode.success")));
        } else {
            if (src instanceof Player) {
                Player player = (Player) src;
                player.getWorld().getProperties().setGameMode(gamemodeInput);
                src.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("command.world.setgamemode.success")));
            } else {
                src.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("command.world.player")));
                return CommandResult.empty();
            }
        }

        return CommandResult.success();
    }
}
