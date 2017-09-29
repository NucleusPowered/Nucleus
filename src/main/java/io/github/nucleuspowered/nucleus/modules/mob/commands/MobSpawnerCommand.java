/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mob.commands;

import io.github.nucleuspowered.nucleus.argumentparsers.ImprovedCatalogTypeArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.PositiveIntegerArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.CatalogTypes;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.HashMap;
import java.util.Map;

@Permissions
@RegisterCommand("mobspawner")
@NonnullByDefault
public class MobSpawnerCommand extends AbstractCommand<CommandSource> {

    private final String playerKey = "player";
    private final String mobTypeKey = "mob";
    private final String amountKey = "amount";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.requiringPermission(GenericArguments.optionalWeak(GenericArguments.player(Text.of(playerKey))), permissions.getPermissionWithSuffix("others")),
                new ImprovedCatalogTypeArgument(Text.of(mobTypeKey), CatalogTypes.ENTITY_TYPE),
                GenericArguments.optional(new PositiveIntegerArgument(Text.of(amountKey)), 1)
        };
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.mobspawner.other"), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Player player = this.getUserFromArgs(Player.class, src, playerKey, args);

        EntityType et = args.<EntityType>getOne(mobTypeKey).get();
        if (!Living.class.isAssignableFrom(et.getEntityClass())) {
            throw ReturnMessageException.fromKey("args.entityparser.livingonly", et.getTranslation().get());
        }

        int amt = args.<Integer>getOne(amountKey).orElse(1);

        ItemStack mobSpawnerStack = ItemStack.builder().itemType(ItemTypes.MOB_SPAWNER).quantity(amt).build();

        if (!mobSpawnerStack.offer(Keys.SPAWNABLE_ENTITY_TYPE, et).isSuccessful()) {
            throw ReturnMessageException.fromKey("command.mobspawner.failed", et.getTranslation().get());
        }

        InventoryTransactionResult itr = player.getInventory().offer(mobSpawnerStack);
        int given = amt;
        if (!itr.getRejectedItems().isEmpty()) {
            ItemStackSnapshot iss = itr.getRejectedItems().stream().findFirst().get();
            if (iss.getQuantity() == amt) {
                // Failed.
                throw ReturnMessageException.fromKey("command.mobspawner.rejected");
            }

            given = amt - iss.getQuantity();
        }

        if (!src.equals(player)) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.mobspawner.givenother", String.valueOf(given), et.getTranslation().get(), player.getName()));
        }

        player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.mobspawner.given", String.valueOf(given), et.getTranslation().get()));
        return CommandResult.success();
    }
}
