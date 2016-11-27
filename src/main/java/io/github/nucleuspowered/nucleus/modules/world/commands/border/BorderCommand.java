/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.border;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.argumentparsers.NucleusWorldPropertiesArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.WorldCommand;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.List;

@Permissions(prefix = "world")
@RegisterCommand(value = {"border"}, subcommandOf = WorldCommand.class)
public class BorderCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<CommandSource> {

    private final String worldKey = "world";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.optional(GenericArguments.onlyOne(new NucleusWorldPropertiesArgument(Text.of(worldKey), NucleusWorldPropertiesArgument.Type.ENABLED_ONLY)))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        WorldProperties wp = getWorldFromUserOrArgs(src, worldKey, args);
        List<Text> worldBorderInfo = Lists.newArrayList();

        Vector3d centre = wp.getWorldBorderCenter();
        int currentDiameter = (int)wp.getWorldBorderDiameter();
        int targetDiameter = (int)wp.getWorldBorderTargetDiameter();

        // Border centre
        worldBorderInfo.add(plugin.getMessageProvider().getTextMessageWithFormat("command.world.border.centre", String.valueOf(centre.getFloorX()), String.valueOf(centre.getFloorZ())));
        worldBorderInfo.add(plugin.getMessageProvider().getTextMessageWithFormat("command.world.border.currentdiameter", String.valueOf(wp.getWorldBorderDiameter())));

        if (currentDiameter != targetDiameter) {
            worldBorderInfo.add(plugin.getMessageProvider().getTextMessageWithFormat("command.world.border.targetdiameter", String.valueOf(targetDiameter), String.valueOf(wp.getWorldBorderTimeRemaining() / 1000)));
        }

        PaginationList.Builder pb = Sponge.getServiceManager().provideUnchecked(PaginationService.class).builder().contents(worldBorderInfo)
                .title(plugin.getMessageProvider().getTextMessageWithFormat("command.world.border.title", wp.getWorldName())).padding(Text.of(TextColors.GREEN, "="));
        if (src instanceof ConsoleSource) {
            pb.linesPerPage(-1);
        }

        pb.sendTo(src);
        return CommandResult.success();
    }
}
