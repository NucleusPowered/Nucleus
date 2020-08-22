/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.properties;

import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

@NonnullByDefault
abstract class AbstractPropertiesSetCommand implements ICommandExecutor<CommandSource> {

    private final String name;

    AbstractPropertiesSetCommand(String name) {
        this.name = name;
    }

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_WORLD_PROPERTIES_ALL.get(serviceCollection),
                NucleusParameters.ONE_TRUE_FALSE
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WorldProperties worldProperties = context.getWorldPropertiesOrFromSelf(NucleusParameters.Keys.WORLD)
                .orElseThrow(() -> context.createException("command.world.player"));;
        boolean set = context.requireOne(NucleusParameters.Keys.BOOL, Boolean.class);
        setter(worldProperties, set);
        context.sendMessage("command.world.setproperty.success", this.name, worldProperties.getWorldName(), String.valueOf(set));
        extraLogic(context, worldProperties, set);
        return context.successResult();
    }

    protected abstract void setter(WorldProperties worldProperties, boolean set) throws CommandException;

    protected void extraLogic(ICommandContext<? extends CommandSource> context, WorldProperties worldProperties, boolean set) throws CommandException {
        // noop
    }

}
