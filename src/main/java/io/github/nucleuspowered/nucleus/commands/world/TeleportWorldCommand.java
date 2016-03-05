/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.commands.world;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.CommandBase;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

/**
 * Teleports you to the world specified.
 *
 * Command Usage: /world teleport [world] Permission:
 * nucleus.world.teleport.base
 */
@Permissions(root = "world", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"teleport", "tp"}, subcommandOf = WorldCommand.class)
public class TeleportWorldCommand extends CommandBase<CommandSource> {

    private final String world = "world";
    private final String player = "player";

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().description(Text.of("Teleport World Command"))
                .arguments(GenericArguments.seq(GenericArguments.world(Text.of(world)),
                        GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Text.of(player))))))
                .executor(this).build();
    }

    @Override
    public CommandResult executeCommand(final CommandSource src, CommandContext args) throws Exception {
        Optional<Player> optPlayer = args.<Player>getOne(player);
        WorldProperties worldProperties = args.<WorldProperties>getOne(world).get();
        Player player = null;

        if (!worldProperties.isEnabled()) {
            src.sendMessage(Util.getTextMessageWithFormat("command.world.notenabled", worldProperties.getWorldName()));
            return CommandResult.success();
        }

        if (!optPlayer.isPresent()) {
            if (src instanceof Player) {
                player = (Player) src;
            } else {
                src.sendMessage(Util.getTextMessageWithFormat("command.world.player", worldProperties.getWorldName()));
                return CommandResult.success();
            }
        } else {
            player = optPlayer.get();
        }

        World world = Sponge.getServer().getWorld(worldProperties.getUniqueId()).get();
        player.transferToWorld(world.getUniqueId(), world.getSpawnLocation().getPosition());
        src.sendMessage(Util.getTextMessageWithFormat("command.world.teleport.success", world.getName()));

        return CommandResult.success();
    }
}
