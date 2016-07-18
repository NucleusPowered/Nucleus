/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.WarnData;
import io.github.nucleuspowered.nucleus.argumentparsers.TimespanArgument;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.warn.handlers.WarnHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MutableMessageChannel;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Permissions(suggestedLevel = SuggestedLevel.MOD)
@NoWarmup
@NoCooldown
@NoCost
@RegisterCommand({"warn", "warning", "warnings"})
public class WarnCommand extends CommandBase<CommandSource> {

    public static final String notifyPermission = PermissionRegistry.PERMISSIONS_PREFIX + "warn.notify";

    private final String playerKey = "player";
    private final String durationKey = "duration";
    private final String reasonKey = "reason";

    @Inject private WarnHandler warnHandler;

    @Override
    public Map<String, PermissionInformation> permissionsToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put(notifyPermission, new PermissionInformation(Util.getMessageWithFormat("permission.warn.notify"), SuggestedLevel.MOD));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.onlyOne(GenericArguments.user(Text.of(playerKey))),
                GenericArguments.onlyOne(GenericArguments.optionalWeak(new TimespanArgument(Text.of(durationKey)))),
                GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Text.of(reasonKey))))};
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        User user = args.<User>getOne(playerKey).get();
        Optional<Long> optDuration = args.getOne(durationKey);
        Optional<String> optReason = args.getOne(reasonKey);

        String reason = optReason.orElse(Util.getMessageWithFormat("command.warn.defaultreason"));
        UUID warner = Util.consoleFakeUUID;
        if (src instanceof Player) {
            warner = ((Player) src).getUniqueId();
        }

        WarnData warnData;
        if (optDuration.isPresent()) {
            warnData = new WarnData(warner, reason, Duration.ofSeconds(optDuration.get()));
        } else {
            warnData = new WarnData(warner, reason);
        }

        if (warnHandler.addWarning(user, warnData)) {
            MutableMessageChannel messageChannel = MessageChannel.permission(notifyPermission).asMutable();
            messageChannel.addMember(src);

            if (optDuration.isPresent()) {
                String time= Util.getTimeStringFromSeconds(optDuration.get());
                messageChannel.send(Util.getTextMessageWithFormat("command.warn.success.time", user.getName(), src.getName(), time));
                messageChannel.send(Util.getTextMessageWithFormat("standard.reason", warnData.getReason()));

                if (user.isOnline()) {
                    user.getPlayer().get().sendMessage(Util.getTextMessageWithFormat("warn.playernotify.time", warnData.getReason(), time));
                }
            } else {
                String time= Util.getTimeStringFromSeconds(optDuration.get());
                messageChannel.send(Util.getTextMessageWithFormat("command.warn.success.time", user.getName(), src.getName(), time));
                messageChannel.send(Util.getTextMessageWithFormat("standard.reason", warnData.getReason()));

                if (user.isOnline()) {
                    user.getPlayer().get().sendMessage(Util.getTextMessageWithFormat("mute.playernotify.standard", warnData.getReason()));
                }
            }
            return CommandResult.success();
        }

        src.sendMessage(Util.getTextMessageWithFormat("command.warn.fail", user.getName()));
        return CommandResult.empty();
    }
}
