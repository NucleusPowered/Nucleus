/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool.commands;

import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.modules.powertool.services.PowertoolService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

@Permissions(mainOverride = "powertool")
@RunAsync
@NoModifiers
@NonnullByDefault
@RegisterCommand(value = {"delete", "del", "rm", "remove"}, subcommandOf = PowertoolCommand.class)
public class DeletePowertoolCommand extends AbstractCommand<Player> {

    PowertoolService service = getServiceUnchecked(PowertoolService.class);

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws Exception {
        Optional<ItemStack> itemStack = src.getItemInHand(HandTypes.MAIN_HAND);
        if (!itemStack.isPresent()) {
            throw ReturnMessageException.fromKey("command.powertool.noitem");
        }

        ItemStack inHand = itemStack.get();
        ItemType type = inHand.getType();
        this.service.getPowertoolForItem(src.getUniqueId(), type)
                .orElseThrow(() -> ReturnMessageException.fromKey(src, "command.powertool.nocmds", Text.of(inHand)));
        this.service.clearPowertool(src.getUniqueId(), type);
        sendMessageTo(src, "command.powertool.removed", Text.of(inHand));
        return CommandResult.success();
    }
}
