/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.BoundedIntegerArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.modules.serverlist.services.ServerListService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Permissions(prefix = "serverlist")
@RunAsync
@RegisterCommand(value = { "message", "m" }, subcommandOf = ServerListCommand.class)
@NonnullByDefault
public class TemporaryMessageCommand extends AbstractCommand<CommandSource> {

    private final String line = "line";

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.flags()
                .flag("r", "-remove")
                .valueFlag(new BoundedIntegerArgument(Text.of(this.line), 1, 2),"l", "-line")
                .valueFlag(NucleusParameters.DURATION, "t", "-time")
                .buildWith(NucleusParameters.OPTIONAL_MESSAGE)
        };
    }

    @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        // Get the temporary message item.
        ServerListService mod = getServiceUnchecked(ServerListService.class);

        if (args.hasAny("r")) {
            if (mod.getMessage().isPresent()) {
                // Remove
                mod.clearMessage();

                // Send message.
                src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.serverlist.message.removed"));
                return CommandResult.success();
            }

            throw ReturnMessageException.fromKey("command.serverlist.message.noremoved");
        }

        // Which line?
        boolean linetwo = args.<Integer>getOne(this.line).map(x -> x == 2).orElse(false);

        Optional<String> onMessage = args.getOne(NucleusParameters.Keys.MESSAGE);

        if (!onMessage.isPresent()) {
            boolean isValid = mod.getExpiry().map(x -> x.isAfter(Instant.now())).orElse(false);
            if (!isValid) {
                throw ReturnMessageException.fromKey("command.serverlist.message.isempty");
            }

            if (linetwo) {
                mod.updateLineTwo(null);
            } else {
                mod.updateLineOne(null);
            }

            Optional<Text> newMessage = mod.getMessage();

            if (newMessage.isPresent()) {
                // Send message
                src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.serverlist.message.set"));
                src.sendMessage(newMessage.get());
            } else {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.serverlist.message.empty"));
            }

            return CommandResult.success();
        }

        String nMessage = onMessage.get();

        // If the expiry is null or before now, and there is no timespan, then it's an hour.
        Instant endTime = args.<Long>getOne(NucleusParameters.Keys.DURATION).map(x -> Instant.now().plus(x, ChronoUnit.SECONDS))
                .orElseGet(() -> mod.getExpiry().map(x -> x.isBefore(Instant.now()) ? x.plusSeconds(3600) : x)
                .orElseGet(() -> Instant.now().plusSeconds(3600)));

        // Set the expiry.
        if (linetwo) {
            mod.setMessage(null, nMessage, endTime);
        } else {
            mod.setMessage(nMessage, null, endTime);
        }

        Optional<Text> newMessage = mod.getMessage();

        if (newMessage.isPresent()) {
            // Send message
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.serverlist.message.set"));
            src.sendMessage(newMessage.get());
            src.sendMessage(Nucleus.getNucleus().getMessageProvider()
                    .getTextMessageWithFormat("command.serverlist.message.expiry", Util.getTimeToNow(endTime)));
            return CommandResult.success();
        }


        throw ReturnMessageException.fromKey("command.serverlist.message.notset");
    }
}
