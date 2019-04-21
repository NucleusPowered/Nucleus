/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.home.services.HomeService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@RunAsync
@NoModifiers
@NonnullByDefault
@Permissions(prefix = "home", supportsOthers = true, suggestedLevel = SuggestedLevel.USER)
@RegisterCommand(value = "limit", subcommandOf = HomeCommand.class)
public class HomeLimitCommand extends AbstractCommand<CommandSource> {

    private final String player = "player";
    private final HomeService handler = getServiceUnchecked(HomeService.class);

    @Override
    protected CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.optional(
                        requirePermissionArg(
                                GenericArguments.user(Text.of(this.player)), this.permissions.getOthers()))
        };
    }

    @Override
    protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        User user = this.getUserFromArgs(User.class, src, this.player, args);
        int current = this.handler.getHomeCount(user);
        int max = this.handler.getMaximumHomes(user);
        if (user.getPlayer().map(src::equals).orElse(false)) {
            if (max == Integer.MAX_VALUE) {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider()
                        .getTextMessageWithFormat("command.home.limit.selfu", String.valueOf(current)));
            } else {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider()
                        .getTextMessageWithFormat("command.home.limit.self", String.valueOf(current), String.valueOf(max)));
            }
        } else {
            if (max == Integer.MAX_VALUE) {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider()
                        .getTextMessageWithFormat("command.home.limit.otheru", user.getName(), String.valueOf(current)));
            } else {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider()
                        .getTextMessageWithFormat("command.home.limit.other", user.getName(), String.valueOf(current), String.valueOf(max)));
            }
        }

        return CommandResult.success();
    }
}
