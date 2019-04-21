/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.modules.teleport.events.RequestEvent;
import io.github.nucleuspowered.nucleus.modules.teleport.services.PlayerTeleporterService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.stream.Collectors;

@Permissions(prefix = "teleport")
@NoModifiers
@NonnullByDefault
@RegisterCommand({"tpaall", "tpaskall"})
@EssentialsEquivalent({"tpaall"})
public class TeleportAskAllHereCommand extends AbstractCommand<Player> {

    private final PlayerTeleporterService playerTeleporterService = getServiceUnchecked(PlayerTeleporterService.class);

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.flags().flag("f").buildWith(GenericArguments.none())
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws ReturnMessageException {
        //Cause cause = Cause.of(NamedCause.owner(src));
        List<Player> cancelled = Lists.newArrayList();
        for (Player x : Sponge.getServer().getOnlinePlayers()) {
            if (x.equals(src)) {
                continue;
            }

            // Before we do all this, check the event.
            RequestEvent.PlayerToCause event = new RequestEvent.PlayerToCause(Sponge.getCauseStackManager().getCurrentCause(), x);
            if (Sponge.getEventManager().post(event)) {
                cancelled.add(x);
                continue;
            }

            this.playerTeleporterService.requestTeleport(
                    src,
                    x,
                    0,
                    0,
                    x,
                    src,
                    !args.<Boolean>getOne("f").orElse(false),
                    false,
                    true,
                    p -> {},
                    "command.tpahere.question"
            );
        }

        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.tpaall.success"));
        if (!cancelled.isEmpty()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.tpall.cancelled",
                cancelled.stream().map(User::getName).collect(Collectors.joining(", "))));
        }

        return CommandResult.success();
    }
}
