/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.inventory.InventoryPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.vavr.control.Either;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.service.permission.Subject;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

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
public class EnderChestCommand implements ICommandExecutor {

    private final Parameter.Value<ServerPlayer> onlinePlayerParameter;
    private final Parameter.Value<UUID> offlinePlayerParameter;

    @Inject
    public EnderChestCommand(final IPermissionService permissionService) {
        this.onlinePlayerParameter = Parameter.player()
                .key(NucleusParameters.Keys.PLAYER)
                .requirements(cause -> permissionService.hasPermission(cause, InventoryPermissions.OTHERS_ENDERCHEST))
                .build();
        this.offlinePlayerParameter = Parameter.user()
                .key(NucleusParameters.Keys.USER)
                .requirements(cause -> permissionService.hasPermission(cause, InventoryPermissions.OTHERS_ENDERCHEST) &&
                        permissionService.hasPermission(cause, InventoryPermissions.ENDERCHEST_OFFLINE))
                .build();
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                Parameter.firstOfBuilder(this.onlinePlayerParameter).or(this.offlinePlayerParameter).optional().build()
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerPlayer currentPlayer = context.requirePlayer();
        final Either<ServerPlayer, User> either;
        final Optional<User> userTarget = context.getOptionalUserFromUUID(this.offlinePlayerParameter);
        if (userTarget.isPresent()) {
            either = Either.right(userTarget.get());
        } else {
            final Optional<ServerPlayer> playerTarget = context.getOne(this.onlinePlayerParameter);
            either = Either.left(playerTarget.orElse(currentPlayer));
        }

        if (!either.fold(context::is, context::is)) {
            final Subject subject = either.fold(Function.identity(), Function.identity());
            if (context.testPermissionFor(subject, InventoryPermissions.ENDERCHEST_EXEMPT_INSPECT)) {
                return context.errorResult("command.enderchest.targetexempt", either.fold(Function.identity(), Function.identity()).name());
            }

            final Inventory ec = either.fold(ServerPlayer::enderChestInventory, User::enderChestInventory);
            if (!context.testPermission(InventoryPermissions.ENDERCHEST_MODIFY)
                    || context.testPermissionFor(subject, InventoryPermissions.ENDERCHEST_EXEMPT_MODIFY)) {
                final UUID uuid = UUID.randomUUID();
                final InventoryMenu menu =
                        ViewableInventory.builder().type(ContainerTypes.GENERIC_9X5).slots(ec.slots(), 0).completeStructure()
                                .identity(uuid).build().asMenu();
                menu.setReadOnly(true);
                return menu.open(currentPlayer)
                        .map(x -> context.successResult())
                        .orElseGet(() -> context.errorResult("command.invsee.failed"));
            } else {
                return currentPlayer.openInventory(ec)
                        .map(x -> context.successResult())
                        .orElseGet(() -> context.errorResult("command.invsee.failed"));
            }
        } else {
            return context.requirePlayer().openInventory(currentPlayer.enderChestInventory())
                    .map(x -> context.successResult())
                    .orElseGet(() -> context.errorResult("command.invsee.failed"));
        }

    }

}
