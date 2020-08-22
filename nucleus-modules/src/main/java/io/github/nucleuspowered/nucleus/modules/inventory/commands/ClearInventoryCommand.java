/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.inventory.InventoryPermissions;
import io.github.nucleuspowered.nucleus.modules.inventory.events.ClearInventoryEvent;
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
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = {"clear", "clearinv", "clearinventory", "ci", "clearinvent"},
        basePermission = InventoryPermissions.BASE_CLEAR,
        commandDescriptionKey = "clear",
        associatedPermissions = InventoryPermissions.OTHERS_CLEAR
)
@EssentialsEquivalent({"clearinventory", "ci", "clean", "clearinvent"})
public class ClearInventoryCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.flags().flag("a", "-all").buildWith(
                GenericArguments.optional(
                        serviceCollection.commandElementSupplier().createPermissionParameter(
                                NucleusParameters.ONE_USER.get(serviceCollection),
                                InventoryPermissions.OTHERS_CLEAR,
                                false
                        ))
            )
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User user = context.getUserFromArgs();
        boolean all = context.hasAny("a");
        User target;
        if (user.getPlayer().isPresent()) {
            target = user.getPlayer().get();
        } else {
            target = user;
        }

        try {
            return clear(context, target, all);
        } catch (UnsupportedOperationException ex) {
            return context.errorResult("command.clearinventory.offlinenotsupported");
        }
    }

    private ICommandResult clear(ICommandContext<? extends CommandSource> context, User target, boolean all) {
        if (Sponge.getEventManager().post(new ClearInventoryEvent.Pre(Sponge.getCauseStackManager().getCurrentCause(), target, all))) {
            return context.errorResult("command.clearinventory.cancelled", target.getName());
        }
        if (all) {
            target.getInventory().clear();
        } else {
            Util.getStandardInventory(target).clear();
        }
        Sponge.getEventManager().post(new ClearInventoryEvent.Post(Sponge.getCauseStackManager().getCurrentCause(), target, all));
        context.sendMessage("command.clearinventory.success", target.getName());
        return context.successResult();
    }
}
