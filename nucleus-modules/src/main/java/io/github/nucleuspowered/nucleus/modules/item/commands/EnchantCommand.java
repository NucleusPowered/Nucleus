/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.item.ItemPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.BoundedIntegerArgument;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.ImprovedCatalogTypeArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.manipulator.mutable.item.EnchantmentData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.stream.Collectors;

@NonnullByDefault
@EssentialsEquivalent({"enchant", "enchantment"})
@Command(
        aliases = { "enchant", "enchantment" },
        basePermission = ItemPermissions.BASE_ENCHANT,
        commandDescriptionKey = "enchant",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = ItemPermissions.EXEMPT_COOLDOWN_ENCHANT),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = ItemPermissions.EXEMPT_WARMUP_ENCHANT),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = ItemPermissions.EXEMPT_COST_ENCHANT)
        },
        associatedPermissions = ItemPermissions.ENCHANT_UNSAFE
)
public class EnchantCommand implements ICommandExecutor<Player> {

    private final String enchantmentKey = "enchantment";
    private final String levelKey = "level";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            new ImprovedCatalogTypeArgument(Text.of(this.enchantmentKey), EnchantmentType.class, serviceCollection),
            new BoundedIntegerArgument(Text.of(this.levelKey), 0, Short.MAX_VALUE, serviceCollection),
            GenericArguments.flags()
                    .valueFlag(serviceCollection.commandElementSupplier()
                            .createPermissionParameter(GenericArguments.none(), ItemPermissions.ENCHANT_UNSAFE, false), "u", "-unsafe")
                    .flag("o", "-overwrite")
                    .buildWith(GenericArguments.none())
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        Player src = context.getIfPlayer();
        // Check for item in hand
        if (!src.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            return context.errorResult("command.enchant.noitem");
        }

        // Get the arguments
        ItemStack itemInHand = src.getItemInHand(HandTypes.MAIN_HAND).get();
        EnchantmentType enchantment = context.requireOne(this.enchantmentKey, EnchantmentType.class);
        int level = context.requireOne(this.levelKey, Integer.class);
        boolean allowUnsafe = context.hasAny("u");
        boolean allowOverwrite = context.hasAny("o");

        // Can we apply the enchantment?
        if (!allowUnsafe) {
            if (!enchantment.canBeAppliedToStack(itemInHand)) {
                return context.errorResult("command.enchant.nounsafe.enchant", itemInHand);
            }

            if (level > enchantment.getMaximumLevel()) {
                return context.errorResult("command.enchant.nounsafe.level", itemInHand);
            }
        }

        // We know this should exist.
        EnchantmentData ed = itemInHand.getOrCreate(EnchantmentData.class).get();

        // Get all the enchantments.
        List<Enchantment> currentEnchants = ed.getListValue().get();

        if (level == 0) {
            // we want to remove only.
            if (!currentEnchants.removeIf(x -> x.getType().getId().equals(enchantment.getId()))) {
                return context.errorResult("command.enchant.noenchantment", enchantment);
            }
        } else {

            List<Enchantment> enchantmentsToRemove = currentEnchants.stream()
                    .filter(x -> !x.getType().isCompatibleWith(enchantment) || x.getType().equals(enchantment))
                    .collect(Collectors.toList());

            if (!allowOverwrite && !enchantmentsToRemove.isEmpty()) {
                // Build the list of the enchantment names, and send it.
                final StringBuilder sb = new StringBuilder();
                enchantmentsToRemove.forEach(x -> {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }

                    sb.append(Util.getTranslatableIfPresent(x.getType()));
                });

                return context.errorResult("command.enchant.overwrite", sb.toString());
            }

            // Remove all enchants that cannot co-exist.
            currentEnchants.removeIf(enchantmentsToRemove::contains);

            // Create the enchantment
            currentEnchants.add(Enchantment.of(enchantment, level));
        }

        ed.setElements(currentEnchants);

        // Offer it to the item.
        DataTransactionResult dtr = itemInHand.offer(ed);
        if (dtr.isSuccessful()) {
            // If successful, we need to put the item in the player's hand for it to actually take effect.
            src.setItemInHand(HandTypes.MAIN_HAND, itemInHand);
            if (level == 0) {
                context.sendMessage("command.enchant.removesuccess", enchantment);
            } else {
                context.sendMessage("command.enchant.success", enchantment, level);
            }
            return context.successResult();
        }

        return context.errorResult("command.enchant.error", enchantment, level);
    }
}
