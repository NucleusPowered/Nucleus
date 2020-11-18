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
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.channel.MutableMessageChannel;
import java.util.UUID;

@Command(aliases = { "voice" }, basePermission = MutePermissions.BASE_VOICE, commandDescriptionKey = "voice")
public class VoiceCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.ONE_PLAYER.get(serviceCollection),
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final MuteHandler muteHandler = context.getServiceCollection().getServiceUnchecked(MuteHandler.class);
        if (!muteHandler.isGlobalMuteEnabled()) {
            return context.errorResult("command.voice.globaloff");
        }

        final Player pl = context.requireOne(NucleusParameters.Keys.PLAYER, Player.class);
        if (context.testPermissionFor(pl, MutePermissions.VOICE_AUTO)) {
            return context.errorResult("command.voice.autovoice", pl.getName());
        }

        final boolean turnOn = context.getOne(NucleusParameters.Keys.BOOL, Boolean.class).orElseGet(() -> !muteHandler.isVoiced(pl.getUniqueId()));

        final UUID voice = pl.getUniqueId();
        if (turnOn == muteHandler.isVoiced(voice)) {
            if (turnOn) {
                context.sendMessage("command.voice.alreadyvoiced", pl.getName());
            } else {
                context.sendMessage("command.voice.alreadynotvoiced", pl.getName());
            }

            return context.failResult();
        }

        final MutableMessageChannel mmc =
                context.getServiceCollection().permissionService().permissionMessageChannel(MutePermissions.VOICE_NOTIFY).asMutable();
        mmc.addMember(context.getCommandSourceRoot());
        if (turnOn) {
            muteHandler.addVoice(pl.getUniqueId());
            mmc.send(context.getMessage("command.voice.voiced.source", pl.getName()));
            context.sendMessageTo(pl, "command.voice.voiced.target");
        } else {
            muteHandler.removeVoice(pl.getUniqueId());
            mmc.send(context.getMessage("command.voice.voiced.source", pl.getName()));
            context.sendMessageTo(pl, "command.voice.voiced.target");
        }

        return context.successResult();
    }
}
