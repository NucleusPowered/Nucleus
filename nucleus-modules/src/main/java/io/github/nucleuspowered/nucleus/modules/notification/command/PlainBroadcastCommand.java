/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.notification.command;

import io.github.nucleuspowered.nucleus.modules.notification.NotificationPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateMessageSender;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(aliases = { "plainbroadcast", "pbcast", "pbc" },
        basePermission = NotificationPermissions.BASE_PLAINBROADCAST,
        commandDescriptionKey = "plainbroadcast")
public class PlainBroadcastCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.MESSAGE
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        try {
            new NucleusTextTemplateMessageSender(
                    context.getServiceCollection().textTemplateFactory(),
                    context.getServiceCollection().textTemplateFactory()
                        .createFromString(context.requireOne(NucleusParameters.Keys.MESSAGE, String.class)),
                    context.getServiceCollection().placeholderService(),
                    context.getCommandSourceUnchecked())
                    .send(context.getCause());
            
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return context.errorResult("command.plainbroadcast.failed");
        }
        return context.successResult();
    }
}
