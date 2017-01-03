/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.afk.handlers.AFKHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;

import java.util.HashMap;
import java.util.Map;

@RegisterCommand("afk")
@Permissions(suggestedLevel = SuggestedLevel.USER)
@NoCooldown
@NoWarmup
@NoCost
@RunAsync
public class AFKCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject private AFKHandler afkHandler;

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("exempt.toggle", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.afk.exempt.toggle"), SuggestedLevel.NONE));
        m.put("exempt.kick", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.afk.exempt.kick"), SuggestedLevel.ADMIN));
        m.put("notify", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.afk.notify"), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        if (permissions.testSuffix(src, "exempt.toggle")) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.afk.exempt"));
            return CommandResult.empty();
        }

        boolean isAFK = afkHandler.isAfk(src);

        if (isAFK) {
            afkHandler.stageUserActivityUpdate(src);
        } else if (!afkHandler.setAfk(src)) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.afk.notset"));
        }

        return CommandResult.success();
    }
}
