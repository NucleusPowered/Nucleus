/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.BoundedIntegerArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.ImprovedCatalogTypeArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.NucleusRequirePermissionArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.manipulator.mutable.item.EnchantmentData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NonnullByDefault
@Permissions
@RegisterCommand("enchant")
@EssentialsEquivalent({"enchant", "enchantment"})
public class EnchantCommand extends AbstractCommand<Player> {

    private final String enchantmentKey = "enchantment";
    private final String levelKey = "level";

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> msp = Maps.newHashMap();
        msp.put("unsafe", PermissionInformation.getWithTranslation("permission.enchant.unsafe", SuggestedLevel.ADMIN));
        return msp;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            new ImprovedCatalogTypeArgument(Text.of(this.enchantmentKey), EnchantmentType.class),
            new BoundedIntegerArgument(Text.of(this.levelKey), 0, Short.MAX_VALUE),
            GenericArguments.flags()
                    .valueFlag(new NucleusRequirePermissionArgument(
                            GenericArguments.none(), this.permissions.getPermissionWithSuffix("unsafe")), "u", "-unsafe")
                    .flag("o", "-overwrite")
                    .buildWith(GenericArguments.none())
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) {
        // Check for item in hand
        if (!src.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            sendMessageTo(src, "command.enchant.noitem");
            return CommandResult.empty();
        }

        // Get the arguments
        ItemStack itemInHand = src.getItemInHand(HandTypes.MAIN_HAND).get();
        EnchantmentType enchantment = args.<EnchantmentType>getOne(this.enchantmentKey).get();
        int level = args.<Integer>getOne(this.levelKey).get();
        boolean allowUnsafe = args.hasAny("u");
        boolean allowOverwrite = args.hasAny("o");

        // Can we apply the enchantment?
        if (!allowUnsafe) {
            if (!enchantment.canBeAppliedToStack(itemInHand)) {
                sendMessageTo(src, "command.enchant.nounsafe.enchant", itemInHand);
                return CommandResult.empty();
            }

            if (level > enchantment.getMaximumLevel()) {
                sendMessageTo(src, "command.enchant.nounsafe.level", itemInHand);
                return CommandResult.empty();
            }
        }

        // We know this should exist.
        EnchantmentData ed = itemInHand.getOrCreate(EnchantmentData.class).get();

        // Get all the enchantments.
        List<Enchantment> currentEnchants = ed.getListValue().get();

        if (level == 0) {
            // we want to remove only.
            if (!currentEnchants.removeIf(x -> x.getType().getId().equals(enchantment.getId()))) {
                sendMessageTo(src, "command.enchant.noenchantment", enchantment);
                return CommandResult.empty();
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

                sendMessageTo(src, "command.enchant.overwrite", sb.toString());
                return CommandResult.empty();
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
                sendMessageTo(src, "command.enchant.removesuccess", enchantment);
            } else {
                sendMessageTo(src, "command.enchant.success", enchantment, level);
            }
            return CommandResult.success();
        }

        sendMessageTo(src, "command.enchant.error", enchantment, level);
        return CommandResult.empty();
    }
}
