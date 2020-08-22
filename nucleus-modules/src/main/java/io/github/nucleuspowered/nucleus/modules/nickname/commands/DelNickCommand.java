/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname.commands;

import io.github.nucleuspowered.nucleus.api.module.nickname.exception.NicknameException;
import io.github.nucleuspowered.nucleus.modules.nickname.NicknamePermissions;
import io.github.nucleuspowered.nucleus.modules.nickname.services.NicknameService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = {"delnick", "delnickname", "deletenick"},
        basePermission = NicknamePermissions.BASE_NICK,
        commandDescriptionKey = "delnick"
)
public class DelNickCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[]{
                serviceCollection.commandElementSupplier()
                        .createOnlyOtherUserPermissionElement(false, NicknamePermissions.OTHERS_NICK)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User pl = context.getUserFromArgs();
        try {
            context.getServiceCollection().getServiceUnchecked(NicknameService.class).removeNick(pl, context.getCommandSource());
        } catch (NicknameException e) {
            e.printStackTrace();
            return context.errorResultLiteral(e.getTextMessage());
        }

        if (!context.is(pl)) {
            context.sendMessage("command.delnick.success.other", pl.getName());
        }

        return context.successResult();
    }
}
