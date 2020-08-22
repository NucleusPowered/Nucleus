/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.commands;

import io.github.nucleuspowered.nucleus.modules.mute.MutePermissions;
import io.github.nucleuspowered.nucleus.modules.mute.services.MuteHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(aliases = "globalmute", basePermission = MutePermissions.BASE_GLOBALMUTE, commandDescriptionKey = "globalmute", async = true)
public class GlobalMuteCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        MuteHandler muteHandler = context.getServiceCollection().getServiceUnchecked(MuteHandler.class);
        boolean turnOn = context.getOne(NucleusParameters.Keys.BOOL, Boolean.class).orElse(!muteHandler.isGlobalMuteEnabled());

        muteHandler.setGlobalMuteEnabled(turnOn);
        String onOff = context.getMessageString(turnOn ? "standard.enabled" : "standard.disabled");
        context.sendMessage("command.globalmute.status", onOff);
        String key = "command.globalmute.broadcast." + (turnOn ? "enabled" : "disabled");
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            context.sendMessageTo(player, key);
        }
        context.sendMessageTo(Sponge.getServer().getConsole(), key);
        return context.successResult();
    }
}
