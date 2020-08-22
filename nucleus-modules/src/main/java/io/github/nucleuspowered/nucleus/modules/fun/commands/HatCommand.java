/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.fun.FunPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

@EssentialsEquivalent({"hat", "head"})
@NonnullByDefault
@Command(
        aliases = {"hat", "head"},
        basePermission = FunPermissions.BASE_HAT,
        commandDescriptionKey = "hat",
        associatedPermissions = FunPermissions.OTHERS_HAT
)
public class HatCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier().createOnlyOtherUserPermissionElement(true, FunPermissions.OTHERS_HAT)
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player pl = context.getPlayerFromArgs();
        boolean isSelf = context.is(pl);
        Optional<ItemStack> helmetOptional = pl.getHelmet();

        ItemStack stack = pl.getItemInHand(HandTypes.MAIN_HAND)
                .orElseThrow(() -> context.createException("command.generalerror.handempty"));
        ItemStack hand = stack.copy();
        hand.setQuantity(1);
        pl.setHelmet(hand);
        Text itemName = hand.get(Keys.DISPLAY_NAME).orElseGet(() -> Text.of(stack));

        GameMode gameMode = pl.get(Keys.GAME_MODE).orElse(GameModes.NOT_SET);
        if (gameMode != GameModes.CREATIVE) {
            if (stack.getQuantity() > 1) {
                stack.setQuantity(stack.getQuantity() - 1);
                pl.setItemInHand(HandTypes.MAIN_HAND, stack);
            } else {
                pl.setItemInHand(HandTypes.MAIN_HAND, null);
            }
        }

        // If the old item can't be placed back in the subject inventory, drop the item.
        helmetOptional.ifPresent(itemStack -> Util.getStandardInventory(pl).offer(itemStack.copy())
                .getRejectedItems().forEach(x -> Util.dropItemOnFloorAtLocation(x, pl.getWorld(), pl.getLocation().getPosition())));

        if (!isSelf) {
            context.sendMessage(
                    "command.hat.success",
                    context.getServiceCollection().playerDisplayNameService().getDisplayName(pl.getUniqueId()),
                    itemName);
        }

        context.sendMessageTo(pl, "command.hat.successself", itemName);
        return context.successResult();
    }
}