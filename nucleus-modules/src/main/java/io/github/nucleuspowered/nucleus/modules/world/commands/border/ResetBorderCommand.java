/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.border;

import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

@NonnullByDefault
@Command(
        aliases = { "reset" },
        basePermission = WorldPermissions.BASE_BORDER_SET,
        commandDescriptionKey = "world.border.reset",
        parentCommand = BorderCommand.class
)
public class ResetBorderCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_WORLD_PROPERTIES_ALL.get(serviceCollection),
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WorldProperties wp = context.getWorldPropertiesOrFromSelf(NucleusParameters.Keys.WORLD)
                .orElseThrow(() -> context.createException("command.world.player"));

        wp.setWorldBorderCenter(0, 0);
        Optional<World> world = Sponge.getServer().getWorld(wp.getUniqueId());

        // A world to get defaults from.
        World toDiameter = world.orElseGet(() -> Sponge.getServer().getWorld(
            Sponge.getServer().getDefaultWorld().orElseThrow(IllegalStateException::new).getUniqueId()).orElseThrow(IllegalStateException::new));

        // +1 includes the final block (1 -> -1 would otherwise give 2, not 3).
        final long diameter = Math.abs(toDiameter.getBiomeMax().getX() - toDiameter.getBiomeMin().getX()) + 1;
        wp.setWorldBorderDiameter(diameter);

        world.ifPresent(w -> {
            w.getWorldBorder().setCenter(0, 0);
            w.getWorldBorder().setDiameter(diameter);
        });

        context.sendMessage("command.world.setborder.set",
                wp.getWorldName(),
                "0",
                "0",
                String.valueOf(diameter));

        return context.successResult();
    }
}
