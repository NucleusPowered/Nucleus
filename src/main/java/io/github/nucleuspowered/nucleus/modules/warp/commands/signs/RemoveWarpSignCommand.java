/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands.signs;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.WarpSignData;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.Optional;

@Permissions(root = "warpsign")
@RegisterCommand(value = {"del", "remove", "rm"}, subcommandOf = WarpSignCommand.class)
public class RemoveWarpSignCommand extends CommandBase<Player> {

    @Inject private WarpConfigAdapter wca;

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        if (!wca.getNodeOrDefault().areWarpSignsEnabled()) {
            src.sendMessage(Util.getTextMessageWithFormat("command.warpsign.notenabled"));
            return CommandResult.empty();
        }

        Optional<Sign> optionalSign = WarpSignCommand.getSignFromBlockRay(src);
        if (!optionalSign.isPresent()) {
            src.sendMessage(Util.getTextMessageWithFormat("command.warpsign.nosign"));
            return CommandResult.empty();
        }

        Sign sign = optionalSign.get();
        Optional<WarpSignData> wsd = sign.get(WarpSignData.class);
        if (wsd.isPresent()) {
            // If the sign data was removed, then we can also remove the warp text from the sign.
            if (sign.remove(WarpSignData.class).isSuccessful()) {
                ListValue<Text> signLines = sign.getSignData().lines();
                if (signLines.get(0).toPlain().equalsIgnoreCase("[Warp]")) {
                    signLines.set(0, Text.EMPTY);
                    SignData sd = sign.getSignData().set(signLines);
                    sign.offer(sd);
                }

                src.sendMessage(Util.getTextMessageWithFormat("command.warpsign.del.success"));
                return CommandResult.success();
            }

            src.sendMessage(Util.getTextMessageWithFormat("command.warpsign.del.error"));
        } else {
            src.sendMessage(Util.getTextMessageWithFormat("command.warpsign.nodata"));
        }

        return CommandResult.empty();
    }
}
