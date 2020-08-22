/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool.commands;

import io.github.nucleuspowered.nucleus.modules.powertool.PowertoolPermissions;
import io.github.nucleuspowered.nucleus.modules.powertool.services.PowertoolService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;
import java.util.UUID;

@NonnullByDefault
@Command(
        aliases = {"delete", "del", "rm", "remove"},
        basePermission = PowertoolPermissions.BASE_POWERTOOL,
        commandDescriptionKey = "powertool.delete",
        parentCommand = PowertoolCommand.class
)
public class DeletePowertoolCommand implements ICommandExecutor<Player> {

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        Optional<ItemStack> itemStack = context.getCommandSource().getItemInHand(HandTypes.MAIN_HAND);
        if (!itemStack.isPresent()) {
            return context.errorResult("command.powertool.noitem");
        }

        ItemStack inHand = itemStack.get();
        ItemType type = inHand.getType();
        UUID uuid = context.getUniqueId().get();
        PowertoolService service = context.getServiceCollection().getServiceUnchecked(PowertoolService.class);
        service.getPowertoolForItem(uuid, type)
                .orElseThrow(() -> context.createException("command.powertool.nocmds", Text.of(inHand)));
        service.clearPowertool(uuid, type);
        context.sendMessage("command.powertool.removed", Text.of(inHand));
        return context.successResult();
    }
}
