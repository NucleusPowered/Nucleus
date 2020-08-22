/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.serverlist.ServerListPermissions;
import io.github.nucleuspowered.nucleus.modules.serverlist.config.ServerListConfig;
import io.github.nucleuspowered.nucleus.modules.serverlist.services.ServerListService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;

@NonnullByDefault
@Command(aliases = {"serverlist", "sl"}, basePermission = ServerListPermissions.BASE_SERVERLIST, commandDescriptionKey = "serverlist", async = true)
public class ServerListCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private ServerListConfig slc = new ServerListConfig();

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags()
                    .flag("m", "-messages")
                    .flag("w", "-whitelist")
                    .buildWith(GenericArguments.none())
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        // Display current information
        if (context.hasAny("m")) {
            onMessage(context, this.slc.getMessages(), "command.serverlist.head.messages");
            return context.successResult();
        } else if (context.hasAny("w")) {
            onMessage(context, this.slc.getWhitelist(), "command.serverlist.head.whitelist");
            return  context.successResult();
        }

        if (this.slc.isModifyServerList()) {
            context.sendMessage("command.serverlist.modify.true");
            if (!this.slc.getMessages().isEmpty()) {
                context.sendMessageText(context.getMessage("command.serverlist.messages.click")
                        .toBuilder().onClick(TextActions.runCommand("/nucleus:serverlist -m")).toText());
            }

            if (!this.slc.getWhitelist().isEmpty()) {
                context.sendMessageText(context.getMessage("command.serverlist.whitelistmessages.click")
                        .toBuilder().onClick(TextActions.runCommand("/nucleus:serverlist -w")).toText());
            }
        } else if (this.slc.getModifyServerList() == ServerListConfig.ServerListSelection.WHITELIST) {
            context.sendMessage("command.serverlist.modify.whitelist");

            if (!this.slc.getWhitelist().isEmpty()) {
                context.sendMessageText(context.getMessage("command.serverlist.whitelistmessages.click")
                                .toBuilder().onClick(TextActions.runCommand("/nucleus:serverlist -w")).toText());
            }
        } else {
            context.sendMessage("command.serverlist.modify.false");
        }

        ServerListService ss = context.getServiceCollection().getServiceUnchecked(ServerListService.class);
        ss.getMessage().ifPresent(
                t -> {
                    context.sendMessageText(Util.SPACE);
                    context.sendMessage("command.serverlist.tempheader");
                    context.sendMessageText(t);
                    context.sendMessage("command.serverlist.message.expiry",
                            context.getTimeToNowString(ss.getExpiry().get()));
                }
            );

        if (this.slc.isHidePlayerCount()) {
            context.sendMessage("command.serverlist.hideplayers");
        } else if (this.slc.isHideVanishedPlayers()) {
            context.sendMessage("command.serverlist.hidevanished");
        }

        return context.successResult();
    }

    private void onMessage(ICommandContext<? extends CommandSource> context, List<NucleusTextTemplateImpl> messages, String key) throws CommandException {
        if (messages.isEmpty()) {
            throw context.createException("command.serverlist.nomessages");
        }

        CommandSource source = context.getCommandSource();
        List<Text> m = Lists.newArrayList();
        messages.stream().map(x -> x.getForCommandSource(source)).forEach(x -> {
            if (!m.isEmpty()) {
                m.add(Util.SPACE);
            }

            m.add(x);
        });

        Util.getPaginationBuilder(source).contents(m).title(context.getMessage(key)).sendTo(source);
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.slc = serviceCollection.moduleDataProvider().getModuleConfig(ServerListConfig.class);
    }
}
