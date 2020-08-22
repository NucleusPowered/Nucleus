/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kick.commands;

import io.github.nucleuspowered.nucleus.modules.kick.KickPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.stream.Collectors;

@NonnullByDefault
@EssentialsEquivalent("kickall")
@Command(
        aliases = "kickall",
        basePermission = KickPermissions.BASE_KICKALL,
        commandDescriptionKey = "kickall",
        associatedPermissionLevelKeys = KickPermissions.KICKALL_WHITELIST
)
public class KickAllCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags()
                        .permissionFlag(KickPermissions.KICKALL_WHITELIST, "w", "f")
                        .buildWith(NucleusParameters.OPTIONAL_REASON)
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        String r = context.getOne(NucleusParameters.Keys.REASON, String.class)
                .orElseGet(() -> context.getMessageString("command.kick.defaultreason"));
        boolean f = context.getOne("w", Boolean.class).orElse(false);

        if (f) {
            Sponge.getServer().setHasWhitelist(true);
        }

        // Don't kick self
        Sponge.getServer().getOnlinePlayers().stream()
                .filter(context::is)
                .collect(Collectors.toList())
                .forEach(x -> x.kick(TextSerializers.FORMATTING_CODE.deserialize(r)));

        // MessageChannel mc = MessageChannel.fixed(Sponge.getServer().getConsole(), src);
        ConsoleSource console = Sponge.getServer().getConsole();
        context.sendMessage("command.kickall.message");
        context.sendMessageTo(console, "command.kickall.message");
        context.sendMessage("command.reason", r);
        context.sendMessageTo(console, "command.reason", r);
        if (f) {
            context.sendMessage("command.kickall.whitelist");
            context.sendMessageTo(console, "command.kickall.whitelist");
        }

        return context.successResult();
    }
}
