/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.commands.jail;

import com.google.inject.Inject;
import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.api.data.JailData;
import io.github.essencepowered.essence.api.data.WarpLocation;
import io.github.essencepowered.essence.argumentparsers.JailParser;
import io.github.essencepowered.essence.argumentparsers.TimespanParser;
import io.github.essencepowered.essence.argumentparsers.UserParser;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.PermissionRegistry;
import io.github.essencepowered.essence.internal.annotations.*;
import io.github.essencepowered.essence.internal.permissions.PermissionInformation;
import io.github.essencepowered.essence.internal.permissions.SuggestedLevel;
import io.github.essencepowered.essence.internal.services.JailHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MutableMessageChannel;
import org.spongepowered.api.text.format.TextColors;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// quickstart.jail.notify
@Permissions(suggestedLevel = SuggestedLevel.MOD)
@Modules(PluginModule.JAILS)
@NoWarmup
@NoCooldown
@NoCost
@RegisterCommand({"jail", "unjail", "togglejail"})
public class JailCommand extends CommandBase<CommandSource> {
    public static final String notifyPermission = PermissionRegistry.PERMISSIONS_PREFIX + "jail.notify";

    @Inject private JailHandler handler;
    private final String playerKey = "player";
    private final String jailKey = "jail";
    private final String durationKey = "duration";
    private final String reasonKey = "reason";

    @Override
    public Map<String, PermissionInformation> permissionsToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put(notifyPermission, new PermissionInformation(Util.getMessageWithFormat("permission.jail.notify"), SuggestedLevel.MOD));
        return m;
    }

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().executor(this).arguments(
            GenericArguments.onlyOne(new UserParser(Text.of(playerKey))),
            GenericArguments.optional(GenericArguments.onlyOne(new JailParser(Text.of(jailKey), handler))),
            GenericArguments.optionalWeak(GenericArguments.onlyOne(new TimespanParser(Text.of(durationKey)))),
            GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Text.of(reasonKey))))
        ).build();
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        // Get the player.
        User pl = args.<User>getOne(playerKey).get();
        if (handler.isPlayerJailed(pl)) {
            return onUnjail(src, args, pl);
        } else {
            return onJail(src, args, pl);
        }
    }

    private CommandResult onUnjail(CommandSource src, CommandContext args, User user) throws Exception {
        if (handler.unjailPlayer(user)) {
            src.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("command.jail.unjail", user.getName())));
            return CommandResult.success();
        } else {
            src.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("command.jail.unjail.fail", user.getName())));
            return CommandResult.empty();
        }
    }

    private CommandResult onJail(CommandSource src, CommandContext args, User user) throws Exception {
        Optional<WarpLocation> owl = args.<WarpLocation>getOne(jailKey);
        if (!owl.isPresent()) {
            src.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("command.jail.jail.nojail")));
            return CommandResult.empty();
        }

        // This might not be there.
        Optional<Long> duration = args.getOne(durationKey);
        String reason = args.<String>getOne(reasonKey).orElse(Util.getMessageWithFormat("command.jail.reason"));
        JailData jd;
        Text message;
        Text messageTo;
        if (duration.isPresent()) {
            if (user.isOnline()) {
                jd = new JailData(Util.getUUID(src), owl.get().getName(), reason, user.getPlayer().get().getLocation(), Instant.now().plusSeconds(duration.get()));
            } else {
                jd = new JailData(Util.getUUID(src), owl.get().getName(), reason, null, Duration.of(duration.get(), ChronoUnit.SECONDS));
            }

            message = Text.of(TextColors.GREEN,
                    Util.getMessageWithFormat("command.checkjail.jailed",
                            user.getName(), owl.get().getName(), src.getName(), " " + Util.getMessageWithFormat("standard.for"), " " + Util.getTimeStringFromSeconds(duration.get())));
            messageTo = Text.of(TextColors.RED,
                    Util.getMessageWithFormat("command.jail.jailed",
                            owl.get().getName(), src.getName(), " " + Util.getMessageWithFormat("standard.for"), " " + Util.getTimeStringFromSeconds(duration.get())));
        } else {
            jd = new JailData(Util.getUUID(src), owl.get().getName(), reason, user.isOnline() ? user.getPlayer().get().getLocation() : null);
            message = Text.of(TextColors.GREEN,
                    Util.getMessageWithFormat("command.checkjail.jailed",
                            user.getName(), owl.get().getName(), src.getName(), "", ""));
            messageTo = Text.of(TextColors.RED,
                    Util.getMessageWithFormat("command.jail.jailed",
                            owl.get().getName(), src.getName(), "", ""));
        }

        if (handler.jailPlayer(user, jd)) {
            MutableMessageChannel mc = MessageChannel.permission(notifyPermission).asMutable();
            mc.addMember(src);
            mc.send(message);
            mc.send(Text.of(TextColors.GREEN, Util.getMessageWithFormat("standard.reason", reason)));

            if (user.isOnline()) {
                user.getPlayer().get().sendMessage(messageTo);
                user.getPlayer().get().sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("standard.reason", reason)));
            }

            return CommandResult.success();
        }

        src.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("command.jail.error")));
        return CommandResult.empty();
    }
}
