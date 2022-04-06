/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jail;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.parameter.JailParameter;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserCacheService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Command(
        aliases = "checkjailed",
        basePermission = JailPermissions.BASE_CHECKJAILED,
        commandDescriptionKey = "checkjailed"
)
public class CheckJailedCommand implements ICommandExecutor {

    private final Parameter.Value<Jail> parameter;

    @Inject
    public CheckJailedCommand(final INucleusServiceCollection serviceCollection) {
        this.parameter = Parameter.builder(Jail.class)
                .key("jail")
                .optional()
                .addParser(new JailParameter(serviceCollection.getServiceUnchecked(JailService.class), serviceCollection.messageProvider()))
                .build();
    }


    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.parameter
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        // Using the cache, tell us who is jailed.
        final Optional<Jail> jail = context.getOne(this.parameter);

        //
        final IUserCacheService userCacheService = context.getServiceCollection().userCacheService();
        final List<UUID> usersInJail = jail.map(x -> userCacheService.getJailedIn(x.getLocation().getName()))
                .orElseGet(userCacheService::getJailed);
        //

        final String jailName = jail.map(x -> x.getLocation().getName()).orElseGet(() -> context.getMessageString("standard.alljails"));

        if (usersInJail.isEmpty()) {
            context.sendMessage("command.checkjailed.none", jailName);
            return context.successResult();
        }

        // Get the users in this jail, or all jails
        Util.getPaginationBuilder(context.audience())
            .title(context.getMessage("command.checkjailed.header", jailName))
            .contents(usersInJail.stream().map(x -> {
                Component name;
                        try {
                            name = context.getDisplayName(x);
                        } catch (final IllegalArgumentException ex) {
                            name = Component.text("unknown: " + x.toString());
                        }
                return name.hoverEvent(HoverEvent.showText(context.getMessage("command.checkjailed.hover")))
                        .clickEvent(ClickEvent.runCommand("/nucleus:checkjail " + x.toString()));
                }).collect(Collectors.toList())).sendTo(context.audience());
        return context.successResult();
    }
}
