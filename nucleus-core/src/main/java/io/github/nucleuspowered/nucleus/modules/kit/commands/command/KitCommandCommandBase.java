/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands.command;

import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;

public abstract class KitCommandCommandBase implements ICommandExecutor<CommandSource> {

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        if (context.getServiceCollection().moduleDataProvider().getModuleConfig(KitConfig.class).isEnableKitCommands()) {
            return this.execute0(context);
        }
        return context.errorResult("command.kit.command.disabled");
    }

    protected abstract ICommandResult execute0(ICommandContext<? extends CommandSource> context) throws CommandException;

}
