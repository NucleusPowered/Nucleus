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
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.PositiveIntegerArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

@NonnullByDefault
@Command(
        aliases = { "set" },
        basePermission = WorldPermissions.BASE_BORDER_SET,
        commandDescriptionKey = "world.border.set",
        parentCommand = BorderCommand.class
)
public class SetBorderCommand implements ICommandExecutor<CommandSource> {

    private final String xKey = "x";
    private final String zKey = "z";
    private final String diameter = "diameter";
    private final String delayKey = "delay";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.firstParsing(
                // Console + player
                GenericArguments.seq(
                    NucleusParameters.OPTIONAL_WORLD_PROPERTIES_ALL.get(serviceCollection),
                    GenericArguments.onlyOne(GenericArguments.integer(Text.of(this.xKey))),
                    GenericArguments.onlyOne(GenericArguments.integer(Text.of(this.zKey))),
                    GenericArguments.onlyOne(new PositiveIntegerArgument(Text.of(this.diameter), serviceCollection)),
                    GenericArguments.onlyOne(GenericArguments.optional(GenericArguments.onlyOne(new PositiveIntegerArgument(Text.of(this.delayKey),
                            serviceCollection))))
                ),

                // Player only
                GenericArguments.seq(
                    GenericArguments.onlyOne(new PositiveIntegerArgument(Text.of(this.diameter), serviceCollection)),
                    GenericArguments.onlyOne(GenericArguments.optional(GenericArguments.onlyOne(new PositiveIntegerArgument(Text.of(this.delayKey),
                            serviceCollection)))))
            )
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WorldProperties wp = context.getWorldPropertiesOrFromSelf(NucleusParameters.Keys.WORLD)
                .orElseThrow(() -> context.createException("command.world.player"));
        int x;
        int z;
        int dia = context.requireOne(this.diameter, Integer.class);
        int delay = context.getOne(this.delayKey, Integer.class).orElse(0);

        if (context.is(Locatable.class)) {
            Location<World> lw = ((Locatable) context.getCommandSource()).getLocation();
            if (context.hasAny(this.zKey)) {
                x = context.requireOne(this.xKey, Integer.class);
                z = context.requireOne(this.zKey, Integer.class);
            } else {
                x = lw.getBlockX();
                z = lw.getBlockZ();
            }
        } else {
            x = context.requireOne(this.xKey, Integer.class);
            z = context.requireOne(this.zKey, Integer.class);
        }

        // Now, if we have an x and a z key, get the centre from that.
        wp.setWorldBorderCenter(x, z);
        Optional<World> world = Sponge.getServer().getWorld(wp.getUniqueId());
        world.ifPresent(w -> w.getWorldBorder().setCenter(x, z));

        wp.setWorldBorderCenter(x, z);

        if (delay == 0) {
            world.ifPresent(w -> w.getWorldBorder().setDiameter(dia));
            wp.setWorldBorderDiameter(dia);
            context.sendMessage("command.world.setborder.set",
                    wp.getWorldName(),
                    String.valueOf(x),
                    String.valueOf(z),
                    String.valueOf(dia));
        } else {
            world.ifPresent(w -> w.getWorldBorder().setDiameter(dia, delay * 1000L));
            wp.setWorldBorderTimeRemaining(delay * 1000L);
            wp.setWorldBorderTargetDiameter(dia);
            context.sendMessage("command.world.setborder.setdelay",
                    wp.getWorldName(),
                    String.valueOf(x),
                    String.valueOf(z),
                    String.valueOf(dia),
                    String.valueOf(delay));
        }


        return context.successResult();
    }


}
