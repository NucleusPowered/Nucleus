/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory.commands;

import io.github.nucleuspowered.nucleus.modules.inventory.InventoryPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;

import java.util.UUID;

@EssentialsEquivalent("invsee")
@Command(
        aliases = {"invsee"},
        basePermission = InventoryPermissions.BASE_INVSEE,
        commandDescriptionKey = "invsee",
        associatedPermissions = {
                InventoryPermissions.INVSEE_EXEMPT_INSPECT,
                InventoryPermissions.INVSEE_EXEMPT_INTERACT,
                InventoryPermissions.INVSEE_MODIFY,
                InventoryPermissions.INVSEE_OFFLINE
        }
)
public class InvSeeCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.ONE_USER
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final User target = context.getOptionalUserFromUUID(NucleusParameters.ONE_USER)
                .orElseThrow(() -> new IllegalStateException("This shouldn't happen"));

        if (!target.isOnline() && !context.testPermission(InventoryPermissions.INVSEE_OFFLINE)) {
            return context.errorResult("command.invsee.nooffline");
        }

        if (!context.is(target)) {
            return context.errorResult("command.invsee.self");
        }

        if (context.testPermissionFor(target, InventoryPermissions.INVSEE_EXEMPT_INSPECT)) {
            return context.errorResult("command.invsee.targetexempt", target.name());
        }

        // Just in case, get the subject inventory if they are online.
        final ServerPlayer src = context.requirePlayer();
        final Inventory targetInv = target.isOnline() ? target.player().get().inventory() : target.inventory();
        if (!context.testPermission(InventoryPermissions.INVSEE_MODIFY)
                || context.testPermissionFor(target, InventoryPermissions.INVSEE_EXEMPT_INTERACT)) {
            final UUID uuid = UUID.randomUUID();
            final InventoryMenu menu =
                    ViewableInventory.builder().type(ContainerTypes.GENERIC_9X5).slots(targetInv.slots(), 0).completeStructure()
                            .identity(uuid).build().asMenu();
            menu.setReadOnly(true);
            return menu.open(src)
                    .map(x -> context.successResult())
                    .orElseGet(() -> context.errorResult("command.invsee.failed"));
        } else {
            return src.openInventory(targetInv)
                    .map(x -> context.successResult())
                    .orElseGet(() -> context.errorResult("command.invsee.failed"));
        }
    }

}
