/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.commands.world;

import com.google.common.collect.Lists;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.annotations.Permissions;
import io.github.essencepowered.essence.internal.annotations.RegisterCommand;
import io.github.essencepowered.essence.internal.permissions.SuggestedLevel;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.service.pagination.PaginationBuilder;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lists all the worlds.
 *
 * Command Usage: /world list Permission: essence.world.list.base
 */
@Permissions(root = "world", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"list", "ls"}, subcommandOf = WorldCommand.class)
public class ListWorldCommand extends CommandBase<CommandSource> {

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().executor(this).description(Text.of("Lists worlds.")).build();
    }

    @Override
    public CommandResult executeCommand(final CommandSource src, CommandContext args) throws Exception {
        List<String> worlds = Sponge.getServer().getWorlds().stream().filter(world -> world.getProperties().isEnabled()).map(World::getName)
                .collect(Collectors.toList());

        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        ArrayList<Text> worldText = Lists.newArrayList();

        for (String name : worlds) {
            Text item = Text.builder(name).onClick(TextActions.runCommand("/world tp " + name))
                    .onHover(TextActions.showText(Text.of(TextColors.WHITE, "Teleport to world ", TextColors.GOLD, name))).color(TextColors.DARK_AQUA)
                    .style(TextStyles.UNDERLINE).build();

            worldText.add(item);
        }

        PaginationBuilder paginationBuilder =
                paginationService.builder().contents(worldText).title(Text.of(TextColors.GREEN, "Showing Worlds")).paddingString("-");
        paginationBuilder.sendTo(src);

        return CommandResult.success();
    }
}
