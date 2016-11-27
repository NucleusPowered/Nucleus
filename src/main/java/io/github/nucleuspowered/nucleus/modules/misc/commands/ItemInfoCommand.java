/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.argumentparsers.ItemAliasArgument;
import io.github.nucleuspowered.nucleus.configurate.datatypes.ItemDataNode;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.internal.DataScanner;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.servershop.ServerShopModule;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.*;

@Permissions
@RegisterCommand({"iteminfo", "itemdb"})
public class ItemInfoCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject private ItemDataService itemDataService;
    @Inject private EconHelper econHelper;

    private final String key = "key";
    private final Text comma = Text.of(TextColors.GREEN, ", ");

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.flags().permissionFlag(permissions.getPermissionWithSuffix("extended"), "e", "-extended")
            .buildWith(GenericArguments.optional(new ItemAliasArgument(Text.of(key), itemDataService)))
        };
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("extended", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.iteminfo.extended"), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandResult executeCommand(Player player, CommandContext args) throws Exception {
        Optional<CatalogType> catalogTypeOptional = args.getOne(key);
        ItemStack it;
        if (catalogTypeOptional.isPresent()) {
            CatalogType ct = catalogTypeOptional.get();
            if (ct instanceof ItemType) {
                it = ((ItemType) ct).getTemplate().createStack();
            } else {
                BlockState bs = ((BlockState) ct);
                it = bs.getType().getItem().orElseThrow(() -> new CommandException(plugin.getMessageProvider().getTextMessageWithFormat("command.iteminfo.invalidblockstate"))).getTemplate().createStack();
                it.offer(Keys.ITEM_BLOCKSTATE, bs);
            }
        } else if (player.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            it = player.getItemInHand(HandTypes.MAIN_HAND).get();
        } else {
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.iteminfo.none"));
            return CommandResult.empty();
        }

        final List<Text> lt = new ArrayList<>();
        String id = it.getItem().getId().toLowerCase();
        lt.add(plugin.getMessageProvider().getTextMessageWithFormat("command.iteminfo.id", it.getItem().getId(), it.getTranslation().get()));

        Optional<BlockState> obs = it.get(Keys.ITEM_BLOCKSTATE);
        if (obs.isPresent()) {
            lt.add(plugin.getMessageProvider().getTextMessageWithFormat("command.iteminfo.extendedid", obs.get().getId()));
            id = obs.get().getId().toLowerCase();
        }

        if (args.hasAny("e") || args.hasAny("extended")) {
            // For each key, see if the item supports it. If so, get and
            // print the value.
            DataScanner.getInstance().getKeysForHolder(it).entrySet().stream().filter(x -> x.getValue() != null).filter(x -> {
                // Work around a Sponge bug.
                try {
                    return it.supports(x.getValue());
                } catch (Exception e) {
                    return false;
                }
            }).forEach(x -> {
                Key<? extends BaseValue<Object>> k = (Key<? extends BaseValue<Object>>) x.getValue();
                if (it.get(k).isPresent()) {
                    DataScanner.getText(player, "command.iteminfo.key", x.getKey(), it.get(k).get()).ifPresent(lt::add);
                }
            });
        }

        ItemDataNode itemDataNode = itemDataService.getDataForItem(id);

        // /buy and /sell prices
        if (econHelper.economyServiceExists() && plugin.getModuleContainer().isModuleLoaded(ServerShopModule.ID)) {
            boolean space = false;
            int buyPrice = itemDataNode.getServerBuyPrice();
            if (buyPrice > -1) {
                lt.add(Text.EMPTY);
                lt.add(plugin.getMessageProvider().getTextMessageWithFormat("command.iteminfo.buyprice", econHelper.getCurrencySymbol(buyPrice)));
                space = true;
            }

            int sellPrice = itemDataNode.getServerSellPrice();
            if (sellPrice > -1) {
                if (!space) {
                    lt.add(Text.EMPTY);
                }

                lt.add(plugin.getMessageProvider().getTextMessageWithFormat("command.iteminfo.sellprice", econHelper.getCurrencySymbol(sellPrice)));
            }
        }

        List<String> aliases = itemDataNode.getAliases();
        if (!aliases.isEmpty()) {
            lt.add(Text.EMPTY);
            lt.add(plugin.getMessageProvider().getTextMessageWithFormat("command.iteminfo.list.aliases"));

            Text.Builder tb = Text.builder();
            Iterator<String> iterator = aliases.iterator();
            tb.append(Text.of(TextColors.YELLOW, iterator.next()));

            while (iterator.hasNext()) {
                tb.append(comma, Text.of(TextColors.YELLOW, iterator.next()));
            }

            lt.add(tb.build());
        }

        Sponge.getServiceManager().provideUnchecked(PaginationService.class).builder().contents(lt).padding(Text.of(TextColors.GREEN, "-"))
                .title(plugin.getMessageProvider().getTextMessageWithFormat("command.iteminfo.list.header")).sendTo(player);
        return CommandResult.success();
    }
}
