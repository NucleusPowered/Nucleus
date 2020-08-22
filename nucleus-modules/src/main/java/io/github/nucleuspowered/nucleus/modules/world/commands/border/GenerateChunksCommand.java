/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.border;

import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.modules.world.services.WorldHelper;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.BoundedIntegerArgument;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.TimespanArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

@NonnullByDefault
@Command(
        aliases = { "gen", "genchunks", "generatechunks", "chunkgen" },
        basePermission = WorldPermissions.BASE_BORDER_GEN,
        commandDescriptionKey = "world.border.gen",
        parentCommand = BorderCommand.class,
        associatedPermissions = WorldPermissions.WORLD_BORDER_GEN_NOTIFY
)
public class GenerateChunksCommand implements ICommandExecutor<CommandSource> {

    private static final String ticksKey = "tickPercent";
    private static final String tickFrequency = "tickFrequency";
    private static final String saveTimeKey = "time between saves";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags()
                    .flag("a")
                    .flag("r")
                    .valueFlag(new TimespanArgument(Text.of(saveTimeKey), serviceCollection), "-save")
                    .valueFlag(new BoundedIntegerArgument(Text.of(ticksKey), 0, 100, serviceCollection), "t", "-tickpercent")
                    .valueFlag(new BoundedIntegerArgument(Text.of(tickFrequency), 1, 100, serviceCollection), "f", "-frequency")
                    .buildWith(NucleusParameters.OPTIONAL_WORLD_PROPERTIES_ENABLED_ONLY.get(serviceCollection))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WorldProperties wp = context.getWorldPropertiesOrFromSelf(NucleusParameters.Keys.WORLD)
                .orElseThrow(() -> context.createException("command.world.player"));

        WorldHelper worldHelper = context.getServiceCollection().getServiceUnchecked(WorldHelper.class);
        if (worldHelper.isPregenRunningForWorld(wp.getUniqueId())) {
            return context.errorResult("command.world.gen.alreadyrunning", wp.getWorldName());
        }

        World w = Sponge.getServer().getWorld(wp.getUniqueId())
                .orElseThrow(() -> context.createException("command.world.gen.notloaded", wp.getWorldName()));
        worldHelper.startPregenningForWorld(w,
                context.hasAny("a"),
                context.getOne(GenerateChunksCommand.saveTimeKey, Long.class).orElse(20L) * 1000L,
                context.getOne(ticksKey, Integer.class).orElse(null),
                context.getOne(tickFrequency, Integer.class).orElse(null),
                context.hasAny("r"));

        context.sendMessage("command.world.gen.started", wp.getWorldName());
        return context.successResult();
    }
}
