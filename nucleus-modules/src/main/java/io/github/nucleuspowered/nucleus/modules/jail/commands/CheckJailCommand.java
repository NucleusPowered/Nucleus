/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jailing;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.vavr.control.Either;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.util.Identifiable;

import java.util.Optional;
import java.util.UUID;

@Command(
        aliases = "checkjail",
        basePermission = JailPermissions.BASE_CHECKJAIL,
        commandDescriptionKey = "checkjail")
public class CheckJailCommand implements ICommandExecutor {

    private final JailService handler;

    @Inject
    public CheckJailCommand(final INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(JailService.class);
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.Composite.USER_OR_GAME_PROFILE
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final Either<User, GameProfile> either = NucleusParameters.Composite.parseUserOrGameProfile(context);
        final UUID uuid = either.fold(Identifiable::getUniqueId, Identifiable::getUniqueId);
        final Optional<Jailing> jailing = this.handler.getPlayerJailData(uuid);
        final Component name = context.getDisplayName(uuid);

        if (!jailing.isPresent()) {
            return context.errorResult("command.checkjail.nojail", name);
        }

        final IMessageProviderService messageProviderService = context.getServiceCollection().messageProvider();
        final Jailing md = jailing.get();
        final Component jailer = context.getServiceCollection().playerDisplayNameService().getName(md.getJailer().orElse(Util.CONSOLE_FAKE_UUID));
        if (md.getRemainingTime().isPresent()) {
            context.sendMessage("command.checkjail.jailedfor", name, md.getJailName(),
                    jailer, messageProviderService.getTimeString(
                            context.getAudience(),
                            md.getRemainingTime().get().getSeconds()));
        } else {
            context.sendMessage("command.checkjail.jailedperm", name, md.getJailName(), name);
        }

        if (md.getCreationInstant().isPresent()) {
            context.sendMessage("command.checkjail.created",
                    Util.FULL_TIME_FORMATTER.withLocale(context.getLocale()).format(md.getCreationInstant().get()));
        } else {
            context.sendMessage("command.checkjail.created", "loc:standard.unknown");
        }

        context.sendMessage("standard.reasoncoloured", md.getReason());
        return context.successResult();
    }
}
