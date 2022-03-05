/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jail;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.parameter.JailParameter;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;

@Command(
        aliases = "tp",
        basePermission = JailPermissions.BASE_JAILS_TP,
        commandDescriptionKey = "jails.tp",
        parentCommand = JailsCommand.class
)
public class JailTeleportCommand implements ICommandExecutor {

    private final Parameter.Value<Jail> parameter;

    @Inject
    public JailTeleportCommand(final INucleusServiceCollection serviceCollection) {
        final JailService handler = serviceCollection.getServiceUnchecked(JailService.class);
        this.parameter = Parameter.builder(Jail.class)
                .key("jail")
                .addParser(new JailParameter(handler, serviceCollection.messageProvider()))
                .build();
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.parameter
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final Jail location = context.requireOne(this.parameter);
        final ServerLocation serverLocation = location.getLocation().getLocation().orElseThrow(() -> context.createException("command.jails.tp.noworld",
                location.getLocation().getName()));

        final ServerPlayer player = context.getIfPlayer();
        player.setLocation(serverLocation);
        player.setRotation(location.getLocation().getRotation());
        context.sendMessage("command.jails.tp.success", location.getLocation().getName());
        return context.successResult();
    }
}
