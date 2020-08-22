/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.misc.MiscPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.ImprovedCatalogTypeArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.util.DataScanner;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NonnullByDefault
@EssentialsEquivalent(value = {"itemdb", "itemno", "durability", "dura"}, isExact = false, notes = "Nucleus tries to provide much more info!")
@Command(
        aliases = { "iteminfo", "itemdb" },
        basePermission = MiscPermissions.BASE_ITEMINFO,
        commandDescriptionKey = "iteminfo",
        associatedPermissions = MiscPermissions.ITEMINFO_EXTENDED
)
public class ItemInfoCommand implements ICommandExecutor<CommandSource> {

    private final String key = "key";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags().permissionFlag(MiscPermissions.ITEMINFO_EXTENDED, "e", "-extended")
                    .buildWith(GenericArguments.optional(new ImprovedCatalogTypeArgument(Text.of(this.key), ItemType.class, serviceCollection)))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Optional<CatalogType> catalogTypeOptional = context.getOne(this.key, CatalogType.class);
        ItemStack it;
        if (catalogTypeOptional.isPresent()) {
            CatalogType ct = catalogTypeOptional.get();
            if (ct instanceof ItemType) {
                it = ((ItemType) ct).getTemplate().createStack();
            } else {
                BlockState bs = ((BlockState) ct);
                it = bs.getType().getItem().orElseThrow(() -> context.createException("command.iteminfo.invalidblockstate")).getTemplate().createStack();
                it.offer(Keys.ITEM_BLOCKSTATE, bs);
            }
        } else if (context.is(Player.class) && context.getIfPlayer().getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            it = context.getIfPlayer().getItemInHand(HandTypes.MAIN_HAND).get();
        } else {
            return context.errorResult("command.iteminfo.none");
        }

        final List<Text> lt = new ArrayList<>();
        lt.add(context.getMessage("command.iteminfo.id", it.getType().getId(), it.getTranslation().get()));

        Optional<BlockState> obs = it.get(Keys.ITEM_BLOCKSTATE);
        obs.ifPresent(blockState -> lt.add(context.getMessage("command.iteminfo.extendedid", blockState.getId())));

        if (context.hasAny("e") || context.hasAny("extended")) {
            // For each key, see if the item supports it. If so, get and
            // print the value.
            DataScanner.getInstance(context.getServiceCollection().messageProvider())
                    .getKeysForHolder(it).entrySet().stream().filter(x -> x.getValue() != null).filter(x -> {
                // Work around a Sponge bug.
                try {
                    return it.supports(x.getValue());
                } catch (Exception e) {
                    return false;
                }
            }).forEach(x -> {
                Key<? extends BaseValue<Object>> k = (Key<? extends BaseValue<Object>>) x.getValue();
                if (it.get(k).isPresent()) {
                    DataScanner.getInstance(context.getServiceCollection().messageProvider())
                            .getText(context.getCommandSourceUnchecked(),
                                "command.iteminfo.key",
                                x.getKey(),
                                it.get(k).get()).ifPresent(lt::add);
                }
            });
        }

        Util.getPaginationBuilder(context.getCommandSource()).contents(lt).padding(Text.of(TextColors.GREEN, "-"))
                .title(context.getMessage("command.iteminfo.list.header")).sendTo(context.getCommandSource());
        return context.successResult();
    }
}
