/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.servershop.commands;

import io.github.nucleuspowered.nucleus.configurate.datatypes.ItemDataNode;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;

import javax.inject.Inject;
import java.util.Optional;

@RunAsync
@NoCost
@NoCooldown
@NoWarmup
@RequiresEconomy
@Permissions(suggestedLevel = SuggestedLevel.USER)
@RegisterCommand({"itemsell", "sell"})
public class SellCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject private ItemDataService itemDataService;
    @Inject private EconHelper econHelper;

    @Override
    public CommandResult executeCommand(final Player src, CommandContext args) throws Exception {
        // Get the item in the hand.
        ItemStack is = src.getItemInHand(HandTypes.MAIN_HAND).orElseThrow(() -> new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.generalerror.handempty")));
        String id;
        Optional<BlockState> blockState = is.get(Keys.ITEM_BLOCKSTATE);
        if (blockState.isPresent()) {
            id = blockState.get().getId().toLowerCase();
        } else {
            id = is.getItem().getId();
        }

        ItemDataNode node = itemDataService.getDataForItem(id);
        final int sellPrice = node.getServerSellPrice();
        if (sellPrice < 0) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.itemsell.notforselling"));
            return CommandResult.empty();
        }

        // Get the cost.
        final int amt = is.getQuantity();
        final int overallCost = sellPrice * amt;
        if (econHelper.depositInPlayer(src, overallCost, false)) {
            src.setItemInHand(HandTypes.MAIN_HAND, null);
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.itemsell.summary", String.valueOf(amt), is.getTranslation().get(), econHelper.getCurrencySymbol(overallCost)));
            return CommandResult.success();
        }

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.itemsell.error", is.getTranslation().get()));
        return CommandResult.empty();
    }
}
