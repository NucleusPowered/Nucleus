/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.ticket.config.TicketConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.ticket.handlers.TicketHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MutableMessageChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@NoWarmup
@NoCooldown
@NoCost
@RunAsync
@Permissions(prefix = "ticket", suggestedLevel = SuggestedLevel.MOD)
@RegisterCommand(value = {"create", "new"}, subcommandOf = TicketCommand.class)
public class CreateTicketCommand extends AbstractCommand<CommandSource> {
    @Inject private TicketConfigAdapter tca;
    @Inject private TicketHandler ticketHandler;

    private final String initialMessageKey = "message";

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("notify", PermissionInformation.getWithTranslation("permission.ticket.notify", SuggestedLevel.MOD));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[]{
                GenericArguments.onlyOne(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Text.of(initialMessageKey))))};
    }

    @Override
    protected CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        String initialMessage = args.<String>getOne(initialMessageKey).get();

        Optional<User> creator = Util.getUserFromUUID(Util.getUUID(src));
        if (!creator.isPresent()) {
            throw ReturnMessageException.fromKey("command.ticket.nouser");
        }

        boolean success = ticketHandler.createTicket(creator.get(), initialMessage, tca.getNodeOrDefault().getDefaultAssignee().isPresent() ? UUID.fromString(tca.getNodeOrDefault().getDefaultAssignee().get()) : null).get();
        if (success) {
            MutableMessageChannel messageChannel = MessageChannel.permission(permissions.getPermissionWithSuffix("notify")).asMutable();

            messageChannel.send(plugin.getMessageProvider().getTextMessageWithFormat("command.ticket.create.notify", creator.get().getName(), initialMessage));
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.ticket.create.success"));
        } else {
            throw ReturnMessageException.fromKey("command.ticket.create.failure");
        }

        return CommandResult.success();
    }
}
