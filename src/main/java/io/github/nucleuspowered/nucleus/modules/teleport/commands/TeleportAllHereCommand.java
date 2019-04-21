/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import io.github.nucleuspowered.nucleus.modules.teleport.services.PlayerTeleporterService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@Permissions(prefix = "teleport")
@NoModifiers
@NonnullByDefault
@RegisterCommand({"tpall", "tpallhere"})
@EssentialsEquivalent("tpall")
public class TeleportAllHereCommand extends AbstractCommand<Player> {

    private final PlayerTeleporterService handler = getServiceUnchecked(PlayerTeleporterService.class);
    private final SafeTeleportService service = getServiceUnchecked(SafeTeleportService.class);

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.flags().flag("f").buildWith(GenericArguments.none())
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) {
        MessageChannel.TO_ALL.send(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.tpall.broadcast", src.getName()));
        Sponge.getServer().getOnlinePlayers().forEach(x -> {
            if (!x.equals(src)) {
                this.service.teleportPlayerSmart(
                        x,
                        src.getTransform(),
                        false,
                        !args.<Boolean>getOne("f").orElse(false),
                        TeleportScanners.NO_SCAN
                );
            }
        });

        return CommandResult.success();
    }
}
