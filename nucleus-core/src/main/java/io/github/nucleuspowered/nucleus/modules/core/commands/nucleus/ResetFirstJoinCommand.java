/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands.nucleus;

import io.github.nucleuspowered.nucleus.modules.core.CoreKeys;
import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.modules.core.commands.NucleusCommand;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.User;

@Command(
        aliases = { "resetfirstjoin" },
        basePermission = CorePermissions.BASE_RESET_FIRST_JOIN,
        commandDescriptionKey = "nucleus.resetfirstjoin",
        parentCommand = NucleusCommand.class
)
public final class ResetFirstJoinCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private boolean useSponge;

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.ONE_USER.get(serviceCollection)
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        final User targetUser = context.requireOne(NucleusParameters.Keys.USER, User.class);
        context.getServiceCollection().storageManager().getUserService()
                .setAndSave(targetUser.getUniqueId(), CoreKeys.FIRST_JOIN_PROCESSED, false)
                .handle((result, exception) -> {
                    if (exception == null) {
                        // all okay
                        context.sendMessage("command.nucleus.firstjoin.success", targetUser.getName());
                        if (context.testPermissionFor(targetUser, CorePermissions.EXEMPT_FIRST_JOIN)) {
                            context.sendMessage("command.nucleus.firstjoin.perm", targetUser.getName(), CorePermissions.EXEMPT_FIRST_JOIN);
                        }
                        if (this.useSponge && targetUser.get(Keys.FIRST_DATE_PLAYED).isPresent()) {
                            context.sendMessage("command.nucleus.firstjoin.date", targetUser.getName());
                        }
                    } else {
                        context.sendMessage("command.nucleus.firstjoin.error", targetUser.getName(), exception.getMessage());
                        exception.printStackTrace();
                    }
                    return (Void) null;
                });
        return context.successResult();
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.useSponge = serviceCollection.moduleDataProvider().getModuleConfig(CoreConfig.class).isCheckFirstDatePlayed();
    }

}
