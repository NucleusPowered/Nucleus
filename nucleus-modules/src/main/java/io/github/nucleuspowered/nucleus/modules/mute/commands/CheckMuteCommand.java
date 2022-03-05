/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.commands;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.module.mute.data.Mute;
import io.github.nucleuspowered.nucleus.modules.mute.MutePermissions;
import io.github.nucleuspowered.nucleus.modules.mute.services.MuteService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.vavr.control.Either;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.util.Identifiable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Command( aliases = "checkmute", basePermission = MutePermissions.BASE_CHECKMUTE, commandDescriptionKey = "checkmute")
public class CheckMuteCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.Composite.USER_OR_GAME_PROFILE
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        // Get the user.
        final Either<UUID, GameProfile> either = NucleusParameters.Composite.parseUserOrGameProfile(context);
        final UUID uuid = either.fold(Function.identity(), GameProfile::uuid);
        final Component name = context.getServiceCollection().playerDisplayNameService().getName(uuid);
        final MuteService muteHandler = context.getServiceCollection().getServiceUnchecked(MuteService.class);

        final Optional<Mute> omd = muteHandler.getPlayerMuteInfo(uuid);
        if (!omd.isPresent()) {
            context.sendMessage("command.checkmute.none", name);
            return context.successResult();
        }

        // Muted, get information.
        final Mute md = omd.get();
        final Component muterName = context.getServiceCollection().playerDisplayNameService().getName(md.getMuter().orElse(Util.CONSOLE_FAKE_UUID));
        if (md.getTimedEntry().isPresent()) {
            context.sendMessage("command.checkmute.mutedfor", name,
                    muterName,
                    context.getTimeString(md.getTimedEntry().get().getRemainingTime().getSeconds()));
        } else {
            context.sendMessage("command.checkmute.mutedperm", name, muterName);
        }

        if (md.getCreationInstant().isPresent()) {
            context.sendMessage("command.checkmute.created",
                    Util.FULL_TIME_FORMATTER.withLocale(context.getLocale())
                            .format(md.getCreationInstant().get()));
        } else {
            context.sendMessage("command.checkmute.created", context.getMessage("standard.unknown"));
        }

        context.sendMessage("standard.reasoncoloured", md.getReason());
        return context.successResult();
    }
}
