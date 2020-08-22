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
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = { "set", "#setitemname", "#renameitem" },
        basePermission = ItemPermissions.BASE_ITEMNAME_SET,
        commandDescriptionKey = "itemname.set",
        parentCommand = ItemNameCommand.class,
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = ItemPermissions.EXEMPT_COOLDOWN_ITEMNAME_SET),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = ItemPermissions.EXEMPT_WARMUP_ITEMNAME_SET),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = ItemPermissions.EXEMPT_COST_ITEMNAME_SET)
        }
)
public class ItemNameSetCommand implements ICommandExecutor<Player> {

    private final String nameKey = "name";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.remainingJoinedStrings(Text.of(this.nameKey))
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        Player src = context.getIfPlayer();
        if (!src.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            return context.errorResult("command.itemname.set.noitem");
        }

        ItemStack stack = src.getItemInHand(HandTypes.MAIN_HAND).get();
        Text name = TextSerializers.FORMATTING_CODE.deserialize(context.requireOne(this.nameKey, String.class));

        if (stack.offer(Keys.DISPLAY_NAME, name).isSuccessful()) {
            src.setItemInHand(HandTypes.MAIN_HAND, stack);

            context.sendMessage("command.itemname.set.success");
            return context.successResult();
        }

        return context.errorResult("command.itemname.set.fail");
    }

}
