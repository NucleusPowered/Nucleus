/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fly.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.modules.fly.FlyKeys;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@Permissions(supportsSelectors = true, supportsOthers = true)
@RegisterCommand("fly")
@EssentialsEquivalent("fly")
@NonnullByDefault
public class FlyCommand extends AbstractCommand.SimpleTargetOtherPlayer {

    @Override public CommandElement[] additionalArguments() {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override protected CommandResult executeWithPlayer(CommandSource src, Player pl, CommandContext args, boolean isSelf) {
        boolean fly = args.<Boolean>getOne(NucleusParameters.Keys.BOOL).orElse(!pl.get(Keys.CAN_FLY).orElse(false));

        if (!setFlying(pl, fly)) {
            src.sendMessages(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.fly.error"));
            return CommandResult.empty();
        }

        getOrCreateUser(pl.getUniqueId()).thenAccept(x -> x.set(FlyKeys.FLY_TOGGLE, fly));
        if (pl != src) {
            src.sendMessages(
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat(fly ? "command.fly.player.on" : "command.fly.player.off", pl.getName()));
        }

        pl.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat(fly ? "command.fly.on" : "command.fly.off"));
        return CommandResult.success();
    }

    private boolean setFlying(Player pl, boolean fly) {
        // Only if we don't want to fly, offer IS_FLYING as false.
        return !(!fly && !pl.offer(Keys.IS_FLYING, false).isSuccessful()) && pl.offer(Keys.CAN_FLY, fly).isSuccessful();
    }
}
