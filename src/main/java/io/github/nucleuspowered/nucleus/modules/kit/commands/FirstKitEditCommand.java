/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.dataservices.GeneralService;
import io.github.nucleuspowered.nucleus.dataservices.KitService;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.Since;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.kit.handlers.KitHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.util.Tuple;

import java.util.List;
import java.util.Optional;

@Permissions(prefix = "firstjoinkit", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"edit", "ed"}, subcommandOf = FirstKitCommand.class)
@NoWarmup
@NoCooldown
@NoCost
@Since(spongeApiVersion = "5.0", minecraftVersion = "1.10.2", nucleusVersion = "0.13")
public class FirstKitEditCommand extends AbstractCommand<Player> {

    @Inject private KitService generalService;
    @Inject private KitHandler kitHandler;

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        List<ItemStackSnapshot> firstKit = generalService.getFirstKit();

        if (kitHandler.getFirstJoinKitInventory().isPresent()) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.firstkit.edit.current"));
        }

        Inventory inventory = Util.getKitInventoryBuilder()
            .property(InventoryTitle.PROPERTY_NAME, InventoryTitle.of(plugin.getMessageProvider().getTextMessageWithFormat("command.firstkit.edit.title")))
            .build(plugin);

        firstKit.stream().filter(x -> !x.getType().equals(ItemTypes.NONE)).forEach(x -> inventory.offer(x.createStack()));
        Optional<Container> openedInventory = src.openInventory(inventory, Cause.of(NamedCause.owner(plugin), NamedCause.source(src)));

        if (openedInventory.isPresent()) {
            kitHandler.setFirstJoinKitInventory(Tuple.of(openedInventory.get(), inventory));
            return CommandResult.success();
        }

        throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.firstkit.edit.cantopen"));
    }
}
