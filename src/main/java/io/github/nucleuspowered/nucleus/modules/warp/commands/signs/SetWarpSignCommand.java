/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands.signs;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.WarpSignData;
import io.github.nucleuspowered.nucleus.argumentparsers.PositiveIntegerArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.WarpArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import io.github.nucleuspowered.nucleus.spongedata.warp.NucleusWarpSignData;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;
import java.util.regex.Pattern;

@Permissions(root = "warpsign")
@RegisterCommand(value = "set", subcommandOf = WarpSignCommand.class)
public class SetWarpSignCommand extends AbstractCommand<Player> {

    private final String warpKey = "warp";
    private final String warmupKey = "warmup";
    private final String permissionKey = "permission";
    private final String costKey = "cost";

    private final Pattern permissionPattern = Pattern.compile("^[A-Za-z0-9\\.\\-]+$");

    @Inject private WarpConfigAdapter wca;

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.flags()
                    .valueFlag(GenericArguments.onlyOne(GenericArguments.string(Text.of(permissionKey))), "p")
                    .valueFlag(GenericArguments.onlyOne(new PositiveIntegerArgument(Text.of(costKey))), "c")
                    .valueFlag(GenericArguments.onlyOne(new PositiveIntegerArgument(Text.of(warmupKey))), "w")
                    .buildWith(GenericArguments.onlyOne(new WarpArgument(Text.of(warpKey), wca, false)))
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        if (!wca.getNodeOrDefault().areWarpSignsEnabled()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warpsign.notenabled"));
            return CommandResult.empty();
        }

        Optional<Sign> optionalSign = WarpSignCommand.getSignFromBlockRay(src);
        if (!optionalSign.isPresent()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warpsign.nosign"));
            return CommandResult.empty();
        }

        Optional<String> permission = args.getOne(permissionKey);

        // Verify the permission if there is one.
        if (permission.isPresent() && !permissionPattern.matcher(permission.get()).matches()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warpsign.set.permissioninvalid"));
            return CommandResult.empty();
        }

        // Verify the warp name
        String warpName = args.<WarpArgument.Result>getOne(warpKey).get().warp;

        WarpSignData data = new NucleusWarpSignData(warpName, permission.orElse(null), args.<Integer>getOne(warmupKey).orElse(0), args.<Integer>getOne(costKey).orElse(0));
        Sign s = optionalSign.get();
        if (s.offer(data).isSuccessful()) {
            SignData sd = s.getSignData();
            if (sd.lines().get(0).toPlain().isEmpty()) {
                sd.setElement(0, Text.of(TextColors.DARK_BLUE, "[Warp]"));
                s.offer(sd);
            }

            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warpsign.set.success", warpName));
            return CommandResult.success();
        }

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warpsign.set.error", warpName));
        return CommandResult.empty();
    }
}
