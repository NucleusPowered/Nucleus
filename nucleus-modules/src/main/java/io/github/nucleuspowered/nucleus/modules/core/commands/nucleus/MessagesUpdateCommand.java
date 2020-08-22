/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands.nucleus;

import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.messageprovider.repository.ConfigFileMessagesRepository;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.util.List;

@Command(
        aliases = "update-messages",
        basePermission = CorePermissions.BASE_NUCLEUS_UPDATE_MESSAGES,
        commandDescriptionKey = "nucleus.update-messages",
        async = true
)
public class MessagesUpdateCommand implements ICommandExecutor<CommandSource> {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.flags().flag("y").buildWith(GenericArguments.none())
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        // First, reload the messages.
        IMessageProviderService messageProviderService = context.getServiceCollection().messageProvider();
        boolean reload = messageProviderService.reloadMessageFile(); //Nucleus.getNucleus().reloadMessages();
        if (!reload) { // only false if we can't read the custom messages file.
            // There was a failure loading a custom file
            context.errorResult("command.nucleus.messageupdate.notfile");
        }

        ConfigFileMessagesRepository messagesRepository = messageProviderService.getConfigFileMessageRepository();
        List<String> mismatched = messagesRepository.walkThroughForMismatched();
        context.sendMessage("command.nucleus.messageupdate.reloaded");
        if (mismatched.isEmpty()) {
            return context.successResult();
        }

        if (context.hasAny("y")) {
            messagesRepository.fixMismatched(mismatched);
            context.sendMessage("command.nucleus.messageupdate.reset");
        } else {
            context.sendMessage("command.nucleus.messageupdate.sometoupdate", String.valueOf(mismatched.size()));
            mismatched.forEach(x -> context.sendMessageText(Text.of(TextColors.YELLOW, x)));
            messageProviderService
                    .getMessageFor(context.getCommandSource().getLocale(),
                            "command.nucleus.messageupdate.confirm",
                            "/nucleus update-messages -y").toBuilder()
                            .onClick(TextActions.runCommand("/nucleus update-messages -y")).build();
        }

        return context.successResult();
    }
}
