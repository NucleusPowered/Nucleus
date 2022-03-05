/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.module.home.exception.HomeException;
import io.github.nucleuspowered.nucleus.modules.home.HomePermissions;
import io.github.nucleuspowered.nucleus.modules.home.parameters.HomeParameter;
import io.github.nucleuspowered.nucleus.modules.home.services.HomeService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;

import java.util.Optional;
import java.util.UUID;

@Command(
        aliases = {"delete", "del", "#deletehome", "#delhome"},
        basePermission = HomePermissions.BASE_HOME,
        commandDescriptionKey = "home.delete",
        parentCommand = HomeCommand.class,
        associatedPermissions = HomePermissions.BASE_HOME_DELETEOTHER
)
@EssentialsEquivalent({"delhome", "remhome", "rmhome"})
public class DeleteHomeCommand implements ICommandExecutor {

    private final Parameter.Value<Home> parameter;
    private final Parameter.Value<UUID> userParameter;

    @Inject
    public DeleteHomeCommand(final INucleusServiceCollection serviceCollection) {
        final IPermissionService permissionService = serviceCollection.permissionService();
        this.userParameter =
                Parameter.user().optional()
                        .requirements(x -> permissionService.hasPermission(x, HomePermissions.BASE_HOME_DELETEOTHER))
                        .key(HomeParameter.OTHER_PLAYER_KEY)
                        .build();
        this.parameter = Parameter.builder(Home.class)
                .addParser(new HomeParameter(serviceCollection.getServiceUnchecked(HomeService.class), serviceCollection.messageProvider()))
                .key("home")
                .build();
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.userParameter,
                this.parameter
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final Home wl = context.requireOne(this.parameter);
        final Optional<User> target = context.getOne(this.userParameter)
                .flatMap(x -> Sponge.server().userManager().load(x).join());

        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            frame.pushCause(context.getCommandSourceRoot());
            context.getServiceCollection().getServiceUnchecked(HomeService.class).removeHome(wl.getOwnersUniqueId(), wl.getLocation().getName());
            if (target.isPresent()) {
                context.sendMessage("command.home.delete.other.success", target.get().name(), wl.getLocation().getName());
            } else {
                context.sendMessage("command.home.delete.success", wl.getLocation().getName());
            }
        } catch (final HomeException e) {
            return context.errorResultLiteral(Component.text(e.getMessage() == null ? "null" : e.getMessage()));
        }

        return context.successResult();
    }

}
