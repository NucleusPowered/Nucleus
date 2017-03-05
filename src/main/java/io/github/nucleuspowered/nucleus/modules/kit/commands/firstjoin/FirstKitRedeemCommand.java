/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands.firstjoin;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.dataservices.KitService;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfigAdapter;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Tristate;

import javax.inject.Inject;

@SuppressWarnings("ALL")
@Permissions(prefix = "firstjoinkit")
@NoCost
@NoCooldown
@NoWarmup
@RegisterCommand(value = "redeem", subcommandOf = FirstKitCommand.class)
public class FirstKitRedeemCommand extends AbstractCommand.SimpleTargetOtherPlayer {
    @Inject private KitService gds;
    @Inject private KitConfigAdapter kca;

    @Override
    protected CommandResult executeWithPlayer(CommandSource src, Player pl, CommandContext args, boolean isSelf) throws Exception {
        Tristate tristate = Util.addToStandardInventory(pl, gds.getFirstKit(), false, kca.getNodeOrDefault().isProcessTokens());

        if (isSelf) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.firstkit.redeem.success"));
            if (tristate != Tristate.TRUE) {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.firstkit.redeem.reject"));
            }
        } else {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.firstkit.give.spawned", pl.getName()));
            pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.firstkit.give.success", src.getName()));
            if (tristate != Tristate.TRUE) {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.firstkit.give.reject", pl.getName()));
            }
        }

        return CommandResult.success();
    }
}
