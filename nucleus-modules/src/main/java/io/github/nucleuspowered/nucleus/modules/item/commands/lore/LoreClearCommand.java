/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands.lore;

import io.github.nucleuspowered.nucleus.modules.item.ItemPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.data.manipulator.mutable.item.LoreData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = { "clear" },
        basePermission = ItemPermissions.BASE_LORE_SET,
        commandDescriptionKey = "lore.clear",
        parentCommand = LoreCommand.class,
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = ItemPermissions.EXEMPT_COOLDOWN_LORE_SET),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = ItemPermissions.EXEMPT_WARMUP_LORE_SET),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = ItemPermissions.EXEMPT_COST_LORE_SET)
        }
)
public class LoreClearCommand implements ICommandExecutor<Player> {

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        Player src = context.getIfPlayer();
        if (!src.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            return context.errorResult("command.lore.clear.noitem");
        }

        ItemStack stack = src.getItemInHand(HandTypes.MAIN_HAND).get();
        LoreData loreData = stack.getOrCreate(LoreData.class).get();
        if (loreData.lore().isEmpty()) {
            return context.errorResult("command.lore.clear.none");
        }

        if (stack.remove(LoreData.class).isSuccessful()) {
            src.setItemInHand(HandTypes.MAIN_HAND, stack);
            context.errorResult("command.lore.clear.success");
            return context.successResult();
        }

        return context.errorResult("command.lore.clear.fail");
    }
}
