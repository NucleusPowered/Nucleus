/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.commands.nickname;

import com.google.inject.Inject;
import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.argumentparsers.UserParser;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.annotations.Modules;
import io.github.essencepowered.essence.internal.annotations.Permissions;
import io.github.essencepowered.essence.internal.annotations.RegisterCommand;
import io.github.essencepowered.essence.internal.interfaces.InternalEssenceUser;
import io.github.essencepowered.essence.internal.services.datastore.UserConfigLoader;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;

@RegisterCommand({ "delnick", "delnickname", "deletenick" })
@Permissions(alias = "nick")
@Modules(PluginModule.NICKNAME)
public class DelNickCommand extends CommandBase<CommandSource> {
    @Inject
    private UserConfigLoader loader;

    private final String playerKey = "player";

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().arguments(
                GenericArguments.optional(GenericArguments.requiringPermission(GenericArguments.onlyOne(new UserParser(Text.of(playerKey))), permissions.getPermissionWithSuffix("others")))
        ).executor(this).build();
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Optional<User> opl = this.getUser(User.class, src, playerKey, args);
        if (opl.isPresent()) {
            return CommandResult.empty();
        }

        User pl = opl.get();
        InternalEssenceUser internalQuickStartUser = loader.getUser(pl);
        internalQuickStartUser.removeNickname();

        if (!src.equals(pl)) {
            src.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("command.delnick.success.other", pl.getName())));
        }

        if (pl.isOnline()) {
            pl.getPlayer().get().sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("command.delnick.success")));
        }

        return CommandResult.success();
    }
}
