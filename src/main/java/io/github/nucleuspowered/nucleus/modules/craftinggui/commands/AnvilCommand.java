/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.craftinggui.commands;

import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.modules.craftinggui.BasicCraftingCommand;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.item.inventory.InventoryArchetypes;

@Permissions
@RegisterCommand("anvil")
public class AnvilCommand extends BasicCraftingCommand {

    @Override protected InventoryArchetype getArchetype() {
        return InventoryArchetypes.ANVIL;
    }
}
