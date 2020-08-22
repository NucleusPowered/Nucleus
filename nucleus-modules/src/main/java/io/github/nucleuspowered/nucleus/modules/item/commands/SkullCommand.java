/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.modules.item.ItemPermissions;
import io.github.nucleuspowered.nucleus.modules.item.config.ItemConfig;
import io.github.nucleuspowered.nucleus.modules.item.config.SkullConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.PositiveIntegerArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;

@NonnullByDefault
@EssentialsEquivalent({"skull", "playerskull", "head"})
@Command(
        aliases = {"skull"},
        basePermission = ItemPermissions.BASE_SKULL,
        commandDescriptionKey = "skull",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = ItemPermissions.EXEMPT_COOLDOWN_SKULL),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = ItemPermissions.EXEMPT_WARMUP_SKULL),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = ItemPermissions.EXEMPT_COST_SKULL)
        },
        associatedPermissions = ItemPermissions.OTHERS_SKULL
)
public class SkullCommand implements ICommandExecutor<Player>, IReloadableService.Reloadable {

    private final String amountKey = "amount";

    private int amountLimit = Integer.MAX_VALUE;
    private boolean isUseMinecraftCommand = false;

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        SkullConfig config = serviceCollection.moduleDataProvider().getModuleConfig(ItemConfig.class).getSkullConfig();
        this.isUseMinecraftCommand = config.isUseMinecraftCommand();
        this.amountLimit = config.getSkullLimit();
    }

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier().createOtherUserPermissionElement(false, ItemPermissions.OTHERS_SKULL),
                GenericArguments.optional(new PositiveIntegerArgument(Text.of(this.amountKey), serviceCollection))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        User user = context.getUserFromArgs();
        Player player = context.getIfPlayer();
        int amount = context.getOne(this.amountKey, Integer.class).orElse(1);

        if (amount > this.amountLimit && !(context.isConsoleAndBypass() || context.testPermission(ItemPermissions.SKULL_EXEMPT_LIMIT))) {
            // fail
            return context.errorResult("command.skull.limit", this.amountLimit);
        }

        if (this.isUseMinecraftCommand) {
            CommandResult result = Sponge.getCommandManager().process(Sponge.getServer().getConsole(),
                String.format("minecraft:give %s skull %d 3 {SkullOwner:%s}", player.getName(), amount, user.getName()));
            if (result.getSuccessCount().orElse(0) > 0) {
                context.sendMessage("command.skull.success.plural", String.valueOf(amount), user.getName());
                return context.successResult();
            }

            return context.errorResult("command.skull.error", user.getName());
        }

        int fullStacks = amount / 64;
        int partialStack = amount % 64;

        // Create the Skull
        ItemStack skullStack = ItemStack.builder().itemType(ItemTypes.SKULL).quantity(64).build();

        // Set it to subject skull type and set the owner to the specified subject
        if (skullStack.offer(Keys.SKULL_TYPE, SkullTypes.PLAYER).isSuccessful()
                && skullStack.offer(Keys.REPRESENTED_PLAYER, user.getProfile()).isSuccessful()) {
            List<ItemStack> itemStackList = Lists.newArrayList();

            // If there were stacks, create as many as needed.
            if (fullStacks > 0) {
                itemStackList.add(skullStack);
                for (int i = 2; i <= fullStacks; i++) {
                    itemStackList.add(skullStack.copy());
                }
            }

            // Same with the partial stacks.
            if (partialStack > 0) {
                ItemStack is = skullStack.copy();
                is.setQuantity(partialStack);
                itemStackList.add(is);
            }

            int accepted = 0;
            int failed = 0;

            Inventory inventoryToOfferTo = player.getInventory()
                    .query(
                            QueryOperationTypes.INVENTORY_TYPE.of(Hotbar.class),
                            QueryOperationTypes.INVENTORY_TYPE.of(GridInventory.class));
            for (ItemStack itemStack : itemStackList) {
                int stackSize = itemStack.getQuantity();
                InventoryTransactionResult itr = inventoryToOfferTo.offer(itemStack);
                int currentFail = itr.getRejectedItems().stream().mapToInt(ItemStackSnapshot::getQuantity).sum();
                failed += currentFail;
                accepted += stackSize - currentFail;
            }

            // What was accepted?
            if (accepted > 0) {
                if (failed > 0) {
                    context.sendMessage("command.skull.semifull", failed);
                }

                if (accepted == 1) {
                    context.sendMessage("command.skull.success.single", user.getName());
                } else {
                    context.sendMessage("command.skull.success.plural", accepted, user.getName());
                }

                return context.successResult();
            }

            return context.errorResult("command.skull.full", user.getName());
        } else {
            return context.errorResult("command.skull.error", user.getName());
        }
    }
}
