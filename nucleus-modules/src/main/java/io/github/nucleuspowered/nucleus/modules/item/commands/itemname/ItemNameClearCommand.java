/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands.itemname;

import io.github.nucleuspowered.nucleus.modules.item.ItemPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

@NonnullByDefault
@Command(
        aliases = { "clear", "#clearitemname", "#resetitemname" },
        basePermission = ItemPermissions.BASE_ITEMNAME_CLEAR,
        commandDescriptionKey = "itemname.clear",
        parentCommand = ItemNameCommand.class,
        modifiers = {
            @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = ItemPermissions.EXEMPT_COOLDOWN_ITEMNAME_CLEAR),
            @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = ItemPermissions.EXEMPT_WARMUP_ITEMNAME_CLEAR),
            @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = ItemPermissions.EXEMPT_COST_ITEMNAME_CLEAR)
        }
)
public class ItemNameClearCommand implements ICommandExecutor<Player> {

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        Player src = context.getIfPlayer();
        if (!src.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            return context.errorResult("command.itemname.clear.noitem");
        }

        ItemStack stack = src.getItemInHand(HandTypes.MAIN_HAND).get();
        Optional<Text> data = stack.get(Keys.DISPLAY_NAME);

        if (!data.isPresent()) {
            // No display name.
            return context.errorResult("command.lore.clear.none");
        }

        if (stack.remove(Keys.DISPLAY_NAME).isSuccessful()) {
            src.setItemInHand(HandTypes.MAIN_HAND, stack);
            context.sendMessage("command.itemname.clear.success");
            return context.successResult();
        }

        return context.errorResult("command.itemname.clear.fail");
    }
}
