/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.BoundedIntegerArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.ImprovedCatalogTypeArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.manipulator.mutable.item.EnchantmentData;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.Enchantment;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Permissions
@RegisterCommand("enchant")
public class EnchantCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    private final String enchantmentKey = "enchantment";
    private final String levelKey = "level";

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> msp = Maps.newHashMap();
        msp.put("unsafe", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.enchant.unsafe"), SuggestedLevel.ADMIN));
        return msp;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            new ImprovedCatalogTypeArgument(Text.of(enchantmentKey), Enchantment.class),
            new BoundedIntegerArgument(Text.of(levelKey), 1, Short.MAX_VALUE),
            GenericArguments.flags()
                    .permissionFlag(permissions.getPermissionWithSuffix("unsafe"), "u", "-unsafe")
                    .flag("o", "-overwrite")
                    .buildWith(GenericArguments.none())
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        // Check for item in hand
        if (!src.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.enchant.noitem"));
            return CommandResult.empty();
        }

        // Get the arguments
        ItemStack itemInHand = src.getItemInHand(HandTypes.MAIN_HAND).get();
        Enchantment enchantment = args.<Enchantment>getOne(enchantmentKey).get();
        int level = args.<Integer>getOne(levelKey).get();
        boolean allowUnsafe = args.hasAny("u");
        boolean allowOverwrite = args.hasAny("o");

        // Can we apply the enchantment?
        if (!allowUnsafe) {
            if (!enchantment.canBeAppliedToStack(itemInHand)) {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.enchant.nounsafe.enchant", itemInHand.getTranslation().get()));
                return CommandResult.empty();
            }

            if (level > enchantment.getMaximumLevel()) {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.enchant.nounsafe.level", itemInHand.getTranslation().get()));
                return CommandResult.empty();
            }
        }

        // We know this should exist.
        EnchantmentData ed = itemInHand.getOrCreate(EnchantmentData.class).get();

        // Get all the enchantments.
        List<ItemEnchantment> currentEnchants = ed.getListValue().get();
        List<ItemEnchantment> enchantmentsToRemove = currentEnchants.stream()
                .filter(x -> !x.getEnchantment().isCompatibleWith(enchantment) || x.getEnchantment().equals(enchantment))
                .collect(Collectors.toList());

        if (!allowOverwrite && !enchantmentsToRemove.isEmpty()) {
            // Build the list of the enchantment names, and send it.
            final StringBuilder sb = new StringBuilder();
            enchantmentsToRemove.forEach(x -> {
                if (sb.length() > 0) {
                    sb.append(", ");
                }

                sb.append(Util.getTranslatableIfPresent(x.getEnchantment()));
            });

            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.enchant.overwrite", sb.toString()));
            return CommandResult.empty();
        }

        // Remove all enchants that cannot co-exist.
        currentEnchants.removeIf(enchantmentsToRemove::contains);

        // Create the enchantment
        currentEnchants.add(new ItemEnchantment(enchantment, level));
        ed.setElements(currentEnchants);

        // Offer it to the item.
        DataTransactionResult dtr = itemInHand.offer(ed);
        if (dtr.isSuccessful()) {
            // If successful, we need to put the item in the player's hand for it to actually take effect.
            src.setItemInHand(HandTypes.MAIN_HAND, itemInHand);
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.enchant.success", Util.getTranslatableIfPresent(enchantment), String.valueOf(level)));
            return CommandResult.success();
        }

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.enchant.error", Util.getTranslatableIfPresent(enchantment), String.valueOf(level)));
        return CommandResult.empty();
    }
}
