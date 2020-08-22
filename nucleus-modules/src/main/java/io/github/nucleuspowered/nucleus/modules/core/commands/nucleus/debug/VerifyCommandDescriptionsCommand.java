/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands.nucleus.debug;

import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.modules.core.commands.nucleus.DebugCommand;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.interfaces.ICommandMetadataService;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.stream.Collectors;

@Command(
        aliases = "verifycmds",
        basePermission = CorePermissions.BASE_NUCLEUS_DEBUG_VERIFY,
        commandDescriptionKey = "nucleus.debug.verifycmds",
        parentCommand = DebugCommand.class
)
public class VerifyCommandDescriptionsCommand implements ICommandExecutor<CommandSource> {

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        ICommandMetadataService commandMetadataService = context.getServiceCollection().commandMetadataService();
        IMessageProviderService messageProviderService = context.getServiceCollection().messageProvider();
        List<Text> messages = commandMetadataService.getCommands()
                .stream()
                .filter(x -> {
                    String key = x.getMetadata().getCommandAnnotation().commandDescriptionKey() + ".desc";
                    return !messageProviderService.hasKey(key);
                })
                .map(x -> Text.of("Command /", x.getCommand(),
                        " missing key \"" + x.getMetadata().getCommandAnnotation().commandDescriptionKey() + ".desc\""))
                .collect(Collectors.toList());

        if (messages.isEmpty()) {
            context.sendMessageText(Text.of("All commands have valid description keys."));
        } else {
            context.sendMessageText(Text.of("Some commands do not have valid description keys:"));
            messages.forEach(context::sendMessageText);
        }

        return context.successResult();
    }
}
