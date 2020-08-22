/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.border;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.modules.world.commands.WorldCommand;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.List;

@NonnullByDefault
@Command(
        aliases = { "border" },
        basePermission = WorldPermissions.BASE_WORLD_BORDER,
        commandDescriptionKey = "world.border",
        parentCommand = WorldCommand.class
)
public class BorderCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_WORLD_PROPERTIES_ENABLED_ONLY.get(serviceCollection)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WorldProperties wp = context.getWorldPropertiesOrFromSelf(NucleusParameters.Keys.WORLD)
                .orElseThrow(() -> context.createException("command.world.player"));
        List<Text> worldBorderInfo = Lists.newArrayList();

        Vector3d centre = wp.getWorldBorderCenter();
        int currentDiameter = (int)wp.getWorldBorderDiameter();
        int targetDiameter = (int)wp.getWorldBorderTargetDiameter();

        // Border centre
        worldBorderInfo.add(context.getMessage("command.world.border.centre", String.valueOf(centre.getFloorX()), String.valueOf(centre.getFloorZ())));
        worldBorderInfo.add(context.getMessage("command.world.border.currentdiameter", String.valueOf(wp.getWorldBorderDiameter())));

        if (currentDiameter != targetDiameter) {
            worldBorderInfo.add(context.getMessage("command.world.border.targetdiameter", String.valueOf(targetDiameter), String.valueOf(wp.getWorldBorderTimeRemaining() / 1000)));
        }

        Util.getPaginationBuilder(context.getCommandSourceUnchecked())
                .contents(worldBorderInfo)
                .title(context.getMessage("command.world.border.title", wp.getWorldName()))
                .padding(Text.of(TextColors.GREEN, "="))
                .sendTo(context.getCommandSourceUnchecked());
        return context.successResult();
    }
}
