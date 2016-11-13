/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.craftinggui;

import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetype;

public abstract class BasicCraftingCommand extends AbstractCommand<Player> {

    protected abstract InventoryArchetype getArchetype();

    @Override public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        Inventory i = Inventory.builder().of(getArchetype())
            .build(plugin);
        src.openInventory(i, Cause.of(NamedCause.owner(plugin))).orElseThrow(() -> new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.crafting.error")));
        return CommandResult.success();
    }
}
