/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.servershop.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.ItemAliasArgument;
import io.github.nucleuspowered.nucleus.configurate.datatypes.ItemDataNode;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.annotations.RequiresEconomy;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.query.QueryOperation;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RunAsync
@NoModifiers
@NonnullByDefault
@RequiresEconomy
@Permissions(suggestedLevel = SuggestedLevel.USER)
@RegisterCommand({"itemsellall", "sellall"})
public class SellAllCommand extends AbstractCommand<Player> {

    private final ItemDataService itemDataService = Nucleus.getNucleus().getItemDataService();
    private final EconHelper econHelper = Nucleus.getNucleus().getEconHelper();

    private final String itemKey = "item";

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.flags().flag("a", "-accept").buildWith(GenericArguments.none()),
            GenericArguments.optional(
                new ItemAliasArgument(Text.of(this.itemKey))
            )
        };
    }

    @Override
    public CommandResult executeCommand(final Player src, CommandContext args) throws Exception {
        boolean accepted = args.hasAny("a");
        CatalogType ct = getCatalogTypeFromHandOrArgs(src, this.itemKey, args);
        String name = Util.getTranslatableIfPresentOnCatalogType(ct);
        String id = ct.getId();

        QueryOperation<?> query;
        if (ct instanceof BlockState) {
            query = QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(ItemStack.builder().fromBlockState((BlockState)ct).quantity(1).build());
        } else {
            query = QueryOperationTypes.ITEM_TYPE.of((ItemType) ct);
        }

        ItemDataNode node = this.itemDataService.getDataForItem(id);
        final double sellPrice = node.getServerSellPrice();
        if (sellPrice < 0) {
            throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.itemsell.notforselling"));
        }

        Iterable<Slot> slots = Util.getStandardInventory(src).query(query).slots();
        List<ItemStack> itemsToSell = StreamSupport.stream(Util.getStandardInventory(src)
                .query(query).slots().spliterator(), false)
            .map(Inventory::peek).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

        // Get the cost.
        final int amt = itemsToSell.stream().mapToInt(ItemStack::getQuantity).sum();
        if (amt <= 0) {
            throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithTextFormat("command.itemsellall.none",
                    Text.of(ct.getName())));
        }

        final double overallCost = sellPrice * amt;

        if (accepted) {
            if (this.econHelper.depositInPlayer(src, overallCost, false)) {
                slots.forEach(Inventory::clear);
                src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithTextFormat("command.itemsell.summary",
                        Text.of(amt), Text.of(name), Text.of(this.econHelper.getCurrencySymbol(overallCost))));
                return CommandResult.success();
            }

            throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithTextFormat("command.itemsell.error", Text.of(ct.getName())));
        }

        src.sendMessage(
                Nucleus.getNucleus().getMessageProvider()
                .getTextMessageWithTextFormat("command.itemsellall.summary",
                    Text.of(amt), Text.of(name), Text.of(this.econHelper.getCurrencySymbol(overallCost)), Text.of(id))
                .toBuilder().onClick(TextActions.runCommand("/nucleus:itemsellall -a " + id)).build()
        );

        return CommandResult.success();
    }
}
