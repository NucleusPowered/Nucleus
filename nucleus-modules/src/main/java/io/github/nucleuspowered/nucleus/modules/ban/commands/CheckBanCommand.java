/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.ban.BanPermissions;
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
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.ban.Ban;

import java.util.Optional;

@Command(aliases = "checkban", basePermission = BanPermissions.BASE_CHECKBAN, commandDescriptionKey = "checkban")
@NonnullByDefault
public class CheckBanCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.firstParsing(
                        NucleusParameters.ONE_GAME_PROFILE_UUID.get(serviceCollection),
                        NucleusParameters.ONE_GAME_PROFILE.get(serviceCollection)
                )
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        GameProfile gp;
        if (context.hasAny(NucleusParameters.Keys.USER_UUID)) {
            gp = context.requireOne(NucleusParameters.Keys.USER_UUID, GameProfile.class);
        } else {
            gp = context.requireOne(NucleusParameters.Keys.USER, GameProfile.class);
        }

        BanService service = Sponge.getServiceManager().provideUnchecked(BanService.class);

        Optional<Ban.Profile> obp = service.getBanFor(gp);
        if (!obp.isPresent()) {
            return context.errorResult("command.checkban.notset", Util.getNameOrUnkown(context, gp));
        }

        Ban.Profile bp = obp.get();

        String name;
        if (bp.getBanSource().isPresent()) {
            name = bp.getBanSource().get().toPlain();
        } else {
            name = context.getServiceCollection().messageProvider().getMessageString(context.getCommandSource(), "standard.unknown");
        }

        if (bp.getExpirationDate().isPresent()) {
            context.sendMessage("command.checkban.bannedfor", Util.getNameOrUnkown(context, gp), name,
                    context.getTimeToNowString(bp.getExpirationDate().get()));
        } else {
            context.sendMessage("command.checkban.bannedperm", Util.getNameOrUnkown(context, gp), name);
        }

        context.sendMessage("command.checkban.created", Util.FULL_TIME_FORMATTER.withLocale(context.getCommandSource().getLocale())
                .format(bp.getCreationDate()
        ));
        context.sendMessage("standard.reasoncoloured", TextSerializers.FORMATTING_CODE.serialize(bp.getReason()
                        .orElse(
                        context.getServiceCollection().messageProvider().getMessageFor(context.getCommandSource().getLocale(), "ban.defaultreason"))));
        return context.successResult();
    }

}
