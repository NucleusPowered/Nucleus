/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.craftinggui.commands;

import io.github.nucleuspowered.nucleus.modules.craftinggui.BasicCraftingCommand;
import io.github.nucleuspowered.nucleus.modules.craftinggui.CraftingGuiPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@Command(
        aliases = "anvil",
        basePermission = CraftingGuiPermissions.BASE_ANVIL,
        commandDescriptionKey = "anvil",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = CraftingGuiPermissions.EXEMPT_COOLDOWN_ANVIL),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = CraftingGuiPermissions.EXEMPT_WARMUP_ANVIL),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = CraftingGuiPermissions.EXEMPT_COST_ANVIL)
        }
)
@NonnullByDefault
public class AnvilCommand extends BasicCraftingCommand {

    @Override protected InventoryArchetype getArchetype() {
        return InventoryArchetypes.ANVIL;
    }
}
