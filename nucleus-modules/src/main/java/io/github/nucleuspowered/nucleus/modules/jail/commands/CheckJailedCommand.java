/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.modules.jail.JailParameters;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserCacheService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@NonnullByDefault
@Command(
        aliases = "checkjailed",
        basePermission = JailPermissions.BASE_CHECKJAILED,
        async = true,
        commandDescriptionKey = "checkjailed"
)
public class CheckJailedCommand implements ICommandExecutor<CommandSource> {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                JailParameters.OPTIONAL_JAIL.get(serviceCollection)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        // Using the cache, tell us who is jailed.
        Optional<NamedLocation> jail = context.getOne(JailParameters.JAIL_KEY, NamedLocation.class);

        //
        IUserCacheService userCacheService = context.getServiceCollection().userCacheService();
        List<UUID> usersInJail = jail.map(x -> userCacheService.getJailedIn(x.getName()))
                .orElseGet(userCacheService::getJailed);
        //

        String jailName = jail.map(NamedLocation::getName).orElseGet(() -> context.getMessageString("standard.alljails"));

        if (usersInJail.isEmpty()) {
            context.sendMessage("command.checkjailed.none", jailName);
            return context.successResult();
        }

        CommandSource src = context.getCommandSource();
        // Get the users in this jail, or all jails
        Util.getPaginationBuilder(src)
            .title(context.getMessage("command.checkjailed.header", jailName))
            .contents(usersInJail.stream().map(x -> {
                Text name;
                        try {
                            name = context.getDisplayName(x);
                        } catch (IllegalArgumentException ex) {
                            name = Text.of("unknown: ", x.toString());
                        }
                return name.toBuilder()
                    .onHover(TextActions.showText(context.getMessage("command.checkjailed.hover")))
                    .onClick(TextActions.runCommand("/nucleus:checkjail " + x.toString()))
                    .build();
            }).collect(Collectors.toList())).sendTo(src);
        return context.successResult();
    }
}
