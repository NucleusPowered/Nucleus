/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.mute.MutePermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@NonnullByDefault
@Command(aliases = "checkmuted", basePermission = MutePermissions.BASE_CHECKMUTED, commandDescriptionKey = "checkmuted", async = true)
public class CheckMutedCommand implements ICommandExecutor<CommandSource> {

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {

        // Using the cache, tell us who is jailed.
        List<UUID> usersInMute = context.getServiceCollection().userCacheService().getMuted();

        if (usersInMute.isEmpty()) {
            context.sendMessage("command.checkmuted.none");
            return context.successResult();
        }

        // Get the users in this jail, or all jails
        Util.getPaginationBuilder(context.getCommandSource())
            .title(context.getMessage("command.checkmuted.header"))
            .contents(usersInMute.stream().map(x -> {
                Text name = context.getServiceCollection().playerDisplayNameService().getDisplayName(x);
                return name.toBuilder()
                    .onHover(TextActions.showText(context.getMessage("command.checkmuted.hover")))
                    .onClick(TextActions.runCommand("/nucleus:checkmute " + x.toString()))
                    .build();
            }).collect(Collectors.toList())).sendTo(context.getCommandSource());
        return context.successResult();
    }
}
