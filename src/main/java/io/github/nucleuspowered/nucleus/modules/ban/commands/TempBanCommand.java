/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.TimespanArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.ban.config.BanConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MutableMessageChannel;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@RegisterCommand("tempban")
@Permissions(suggestedLevel = SuggestedLevel.MOD)
@NoWarmup
@NoCooldown
@NoCost
public class TempBanCommand extends CommandBase<CommandSource> {

    @Inject private BanConfigAdapter bca;
    private final String user = "user";
    private final String reason = "reason";
    private final String duration = "duration";

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("offline", new PermissionInformation(Util.getMessageWithFormat("permission.tempban.offline"), SuggestedLevel.MOD));
        m.put("exempt.target", new PermissionInformation(Util.getMessageWithFormat("permission.tempban.exempt.target"), SuggestedLevel.MOD));
        m.put("exempt.length", new PermissionInformation(Util.getMessageWithFormat("permission.tempban.exempt.length"), SuggestedLevel.MOD));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.onlyOne(GenericArguments.user(Text.of(user))), GenericArguments.onlyOne(new TimespanArgument(Text.of(duration))),
                GenericArguments.optionalWeak(GenericArguments.remainingJoinedStrings(Text.of(reason)))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        User u = args.<User>getOne(user).get();
        Long time = args.<Long>getOne(duration).get();
        String r = args.<String>getOne(reason).orElse(Util.getMessageWithFormat("ban.defaultreason"));

        if (permissions.testSuffix(u, "exempt.target")) {
            src.sendMessage(Util.getTextMessageWithFormat("command.tempban.exempt", u.getName()));
            return CommandResult.success();
        }

        if (!u.isOnline() && !permissions.testSuffix(src, "offline")) {
            src.sendMessage(Util.getTextMessageWithFormat("command.tempban.offline.noperms"));
            return CommandResult.success();
        }

        if (time > bca.getNodeOrDefault().getMaximumTempBanLength() &&  bca.getNodeOrDefault().getMaximumTempBanLength() != -1 && !permissions.testSuffix(src, "exempt.length")) {
            src.sendMessage(Util.getTextMessageWithFormat("command.tempban.length.toolong", Util.getTimeStringFromSeconds(bca.getNodeOrDefault().getMaximumTempBanLength())));
            return CommandResult.success();
        }

        BanService service = Sponge.getServiceManager().provideUnchecked(BanService.class);

        if (service.isBanned(u.getProfile())) {
            src.sendMessage(Util.getTextMessageWithFormat("command.ban.alreadyset", u.getName()));
            return CommandResult.empty();
        }

        // Expiration date
        Instant date = Instant.now().plus(time, ChronoUnit.SECONDS);

        // Create the ban.
        Ban bp = Ban.builder().type(BanTypes.PROFILE).profile(u.getProfile()).source(src).expirationDate(date).reason(TextSerializers.FORMATTING_CODE.deserialize(r)).build();
        service.addBan(bp);

        MutableMessageChannel send = MessageChannel.permission(BanCommand.notifyPermission).asMutable();
        send.addMember(src);
        send.send(Util.getTextMessageWithFormat("command.tempban.applied", u.getName(), Util.getTimeStringFromSeconds(time), src.getName()));
        send.send(Util.getTextMessageWithFormat("standard.reason", reason));

        return CommandResult.success();
    }
}
