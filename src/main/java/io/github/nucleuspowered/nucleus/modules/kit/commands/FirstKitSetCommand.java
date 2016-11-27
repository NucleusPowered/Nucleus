/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.dataservices.KitService;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

@Permissions(prefix = "firstjoinkit")
@NoCost
@NoCooldown
@NoWarmup
@RegisterCommand(value = {"set", "updateFromInventory"}, subcommandOf = FirstKitCommand.class)
public class FirstKitSetCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject
    private KitService gds;

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        List<Inventory> slots = Lists.newArrayList(Util.getStandardInventory(src).slots());
        final List<ItemStackSnapshot> stacks = slots.stream().filter(x -> x.peek().isPresent()).map(x -> x.peek().get().createSnapshot()).collect(Collectors.toList());
        gds.setFirstKit(stacks);
        gds.save();

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.firstkit.set.success"));
        return CommandResult.success();
    }
}
