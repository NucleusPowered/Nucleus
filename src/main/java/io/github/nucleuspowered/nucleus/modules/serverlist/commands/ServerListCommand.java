/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.text.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.modules.serverlist.config.ServerListConfig;
import io.github.nucleuspowered.nucleus.modules.serverlist.config.ServerListConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.serverlist.services.ServerListService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;

@NoModifiers
@RunAsync
@NonnullByDefault
@RegisterCommand(value = {"serverlist", "sl"})
public class ServerListCommand extends AbstractCommand<CommandSource> implements Reloadable {

    private ServerListConfig slc = new ServerListConfig();

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.flags()
                    .flag("m", "-messages")
                    .flag("w", "-whitelist")
                    .buildWith(GenericArguments.none())
        };
    }

    @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        // Display current information
        if (args.hasAny("m")) {
            onMessage(src, this.slc.getMessages(), "command.serverlist.head.messages");
            return CommandResult.success();
        } else if (args.hasAny("w")) {
            onMessage(src, this.slc.getWhitelist(), "command.serverlist.head.whitelist");
            return CommandResult.success();
        }

        MessageProvider messageProvider = Nucleus.getNucleus().getMessageProvider();

        if (this.slc.isModifyServerList()) {
            src.sendMessage(messageProvider.getTextMessageWithFormat("command.serverlist.modify.true"));
            if (!this.slc.getMessages().isEmpty()) {
                src.sendMessage(
                    messageProvider.getTextMessageWithFormat("command.serverlist.messages.click")
                        .toBuilder().onClick(TextActions.runCommand("/nucleus:serverlist -m")).toText());
            }

            if (!this.slc.getWhitelist().isEmpty()) {
                src.sendMessage(
                    messageProvider.getTextMessageWithFormat("command.serverlist.whitelistmessages.click")
                        .toBuilder().onClick(TextActions.runCommand("/nucleus:serverlist -w")).toText());
            }
        } else if (this.slc.getModifyServerList() == ServerListConfig.ServerListSelection.WHITELIST) {
            src.sendMessage(messageProvider.getTextMessageWithFormat("command.serverlist.modify.whitelist"));

            if (!this.slc.getWhitelist().isEmpty()) {
                src.sendMessage(
                        messageProvider.getTextMessageWithFormat("command.serverlist.whitelistmessages.click")
                                .toBuilder().onClick(TextActions.runCommand("/nucleus:serverlist -w")).toText());
            }
        } else {
            src.sendMessage(messageProvider.getTextMessageWithFormat("command.serverlist.modify.false"));
        }

        ServerListService ss = getServiceUnchecked(ServerListService.class);
        ss.getMessage().ifPresent(
                t -> {
                    src.sendMessage(Util.SPACE);
                    src.sendMessage(messageProvider.getTextMessageWithFormat("command.serverlist.tempheader"));
                    src.sendMessage(t);
                    src.sendMessage(messageProvider.getTextMessageWithFormat("command.serverlist.message.expiry",
                            Util.getTimeToNow(ss.getExpiry().get())));
                }
            );

        if (this.slc.isHidePlayerCount()) {
            src.sendMessage(messageProvider.getTextMessageWithFormat("command.serverlist.hideplayers"));
        } else if (this.slc.isHideVanishedPlayers()) {
            src.sendMessage(messageProvider.getTextMessageWithFormat("command.serverlist.hidevanished"));
        }

        return CommandResult.success();
    }

    private void onMessage(CommandSource source, List<NucleusTextTemplateImpl> messages, String key) throws Exception {
        if (messages.isEmpty()) {
            throw ReturnMessageException.fromKey("command.serverlist.nomessages");
        }

        List<Text> m = Lists.newArrayList();
        messages.stream().map(x -> x.getForCommandSource(source)).forEach(x -> {
            if (!m.isEmpty()) {
                m.add(Util.SPACE);
            }

            m.add(x);
        });

        Util.getPaginationBuilder(source).contents(m)
                .title(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat(key)).sendTo(source);
    }

    @Override public void onReload() {
        this.slc = getServiceUnchecked(ServerListConfigAdapter.class).getNodeOrDefault();
    }
}
