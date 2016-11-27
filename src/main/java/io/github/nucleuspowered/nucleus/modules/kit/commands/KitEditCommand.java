/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.KitArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.Since;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.kit.handlers.KitHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tuple;

import java.util.Optional;

@Permissions(prefix = "kit", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"edit", "ed"}, subcommandOf = KitCommand.class)
@NoWarmup
@NoCooldown
@NoCost
@Since(spongeApiVersion = "5.0", minecraftVersion = "1.10.2", nucleusVersion = "0.13")
public class KitEditCommand extends AbstractCommand<Player> {

    @Inject private KitConfigAdapter kca;
    @Inject private CoreConfigAdapter cca;
    @Inject private KitHandler kitHandler;
    private final String kitKey = "kit";

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.onlyOne(new KitArgument(Text.of(kitKey), kca, kitHandler, false))
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        final KitArgument.KitInfo kitInfo = args.<KitArgument.KitInfo>getOne(kitKey).get();

        if (kitHandler.isOpen(kitInfo.name)) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.edit.current", kitInfo.name));
        }

        Inventory inventory = Util.getKitInventoryBuilder()
            .property(InventoryTitle.PROPERTY_NAME, InventoryTitle.of(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.edit.title", kitInfo.name)))
            .build(plugin);

        kitInfo.kit.getStacks().stream().filter(x -> !x.getType().equals(ItemTypes.NONE)).forEach(x -> inventory.offer(x.createStack()));
        Optional<Container> openedInventory = src.openInventory(inventory, Cause.of(NamedCause.owner(plugin), NamedCause.source(src)));

        if (openedInventory.isPresent()) {
            kitHandler.addKitInventoryToListener(Tuple.of(kitInfo, inventory), openedInventory.get());
            return CommandResult.success();
        }

        throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.edit.cantopen", kitInfo.name));
    }
}
