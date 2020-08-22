/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory.commands;

import io.github.nucleuspowered.nucleus.modules.inventory.InventoryPermissions;
import io.github.nucleuspowered.nucleus.modules.inventory.listeners.InvSeeListener;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.IfConditionElseArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = {"enderchest", "ec", "echest"},
        basePermission = InventoryPermissions.BASE_ENDERCHEST,
        commandDescriptionKey = "enderchest",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = InventoryPermissions.EXEMPT_COOLDOWN_ENDERCHEST),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = InventoryPermissions.EXEMPT_WARMUP_ENDERCHEST),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = InventoryPermissions.EXEMPT_COST_ENDERCHEST)
        },
        associatedPermissions = {
                InventoryPermissions.OTHERS_ENDERCHEST,
                InventoryPermissions.ENDERCHEST_EXEMPT_INSPECT,
                InventoryPermissions.ENDERCHEST_EXEMPT_MODIFY,
                InventoryPermissions.ENDERCHEST_MODIFY,
                InventoryPermissions.ENDERCHEST_OFFLINE
        }
)
@EssentialsEquivalent({"enderchest", "echest", "endersee", "ec"})
public class EnderChestCommand implements ICommandExecutor<Player> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.optional(
                        serviceCollection.commandElementSupplier().createPermissionParameter(
                            IfConditionElseArgument.permission(
                                    serviceCollection.permissionService(),
                                    InventoryPermissions.ENDERCHEST_OFFLINE,
                                    NucleusParameters.ONE_USER_PLAYER_KEY.get(serviceCollection), // user if permission
                                    NucleusParameters.ONE_PLAYER.get(serviceCollection) // player if not
                            ),
                            InventoryPermissions.OTHERS_ENDERCHEST, false))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        User target = context.getUserFromArgs();

        if (!context.is(target)) {
            if (context.testPermissionFor(target, InventoryPermissions.ENDERCHEST_EXEMPT_INSPECT)) {
                return context.errorResult("command.enderchest.targetexempt", target.getName());
            }

            Inventory ec = target.getEnderChestInventory();
            Container container = context.getCommandSourceAsPlayerUnchecked()
                    .openInventory(ec)
                    .orElseThrow(() -> context.createException("command.invsee.failed"));

            if (context.testPermissionFor(target, InventoryPermissions.ENDERCHEST_EXEMPT_MODIFY)
                || !context.testPermission(InventoryPermissions.ENDERCHEST_MODIFY)) {
                InvSeeListener.addEntry(context.getCommandSourceAsPlayerUnchecked().getUniqueId(), container);
            }

            return context.successResult();
        } else {
            return context.getCommandSourceAsPlayerUnchecked().openInventory(
                        context.getCommandSourceAsPlayerUnchecked().getEnderChestInventory())
                    .map(x -> context.successResult())
                    .orElseGet(() -> context.errorResult("command.invsee.failed"));
        }

    }

}
