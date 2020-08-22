/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.craftinggui;

import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
public abstract class BasicCraftingCommand implements ICommandExecutor<Player> {

    protected abstract InventoryArchetype getArchetype();

    @Override
    public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        Inventory i = Inventory.builder().of(getArchetype()).build(context.getServiceCollection().pluginContainer());
        context.getCommandSource().openInventory(i).orElseThrow(() -> context.createException("command.crafting.error"));
        return context.successResult();
    }
}
