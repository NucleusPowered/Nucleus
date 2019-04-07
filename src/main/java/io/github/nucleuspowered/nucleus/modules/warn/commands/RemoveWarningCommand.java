/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.argumentparsers.WarningArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.warn.data.WarnData;
import io.github.nucleuspowered.nucleus.modules.warn.services.WarnHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;

@Permissions(suggestedLevel = SuggestedLevel.ADMIN)
@RunAsync
@NoModifiers
@RegisterCommand({"removewarning", "deletewarning", "delwarn"})
@NonnullByDefault
public class RemoveWarningCommand extends AbstractCommand<CommandSource> {

    private final WarnHandler handler = getServiceUnchecked(WarnHandler.class);
    private final String warningKey = "warning";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.flags().flag("-remove", "r").buildWith(
                GenericArguments.onlyOne(new WarningArgument(Text.of(this.warningKey), this.handler)))};
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
        WarningArgument.Result result = args.<WarningArgument.Result>getOne(this.warningKey).get();
        User user = result.user;
        boolean removePermanently = false;
        if (args.hasAny("remove")) {
            removePermanently = true;
        }

        List<WarnData> warnings = this.handler.getWarningsInternal(user);
        if (warnings.isEmpty()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.checkwarnings.none", user.getName()));
            return CommandResult.success();
        }

        if (this.handler.removeWarning(user, result.warnData, removePermanently, cause)) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.removewarning.success", user.getName()));
            return CommandResult.success();
        }

        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.removewarning.failure", user.getName()));
        return CommandResult.empty();
    }
}
