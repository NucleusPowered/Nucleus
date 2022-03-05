/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.commands;

import io.github.nucleuspowered.nucleus.api.module.mute.data.Mute;
import io.github.nucleuspowered.nucleus.core.configurate.config.CommonPermissionLevelConfig;
import io.github.nucleuspowered.nucleus.modules.mute.MutePermissions;
import io.github.nucleuspowered.nucleus.modules.mute.config.MuteConfig;
import io.github.nucleuspowered.nucleus.modules.mute.services.MuteService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.vavr.control.Either;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileCache;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@EssentialsEquivalent(value = {"mute", "silence"}, isExact = false, notes = "Unmuting a player should be done via the /unmute command.")
@Command(
        aliases = { "mute" },
        basePermission = MutePermissions.BASE_MUTE,
        commandDescriptionKey = "mute",
        associatedPermissionLevelKeys = MutePermissions.MUTE_LEVEL_KEY,
        associatedPermissions = {
                MutePermissions.MUTE_EXEMPT_LENGTH,
                MutePermissions.MUTE_EXEMPT_TARGET,
                MutePermissions.MUTE_NOTIFY,
                MutePermissions.MUTE_SEEMUTEDCHAT
        }
)
public class MuteCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private long maxMute = Long.MAX_VALUE;
    private CommonPermissionLevelConfig levelConfig = new CommonPermissionLevelConfig();

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
            NucleusParameters.Composite.USER_OR_GAME_PROFILE,
            NucleusParameters.OPTIONAL_DURATION,
            NucleusParameters.OPTIONAL_REASON
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final MuteService handler = context.getServiceCollection().getServiceUnchecked(MuteService.class);
        // Get the user.
        final UUID uuid = NucleusParameters.Composite.parseUserOrGameProfile(context).fold(Function.identity(), GameProfile::uuid);

        final Optional<Duration> time = context.getOne(NucleusParameters.DURATION);
        final User user = Sponge.server().userManager().loadOrCreate(uuid).join();
        final Optional<String> reas = context.getOne(NucleusParameters.OPTIONAL_REASON);

        if (!context.isConsoleAndBypass() && context.testPermissionFor(user, MutePermissions.MUTE_EXEMPT_TARGET)) {
            return context.errorResult("command.mute.exempt", user.name());
        }

        if (this.levelConfig.isUseLevels() &&
                !context.isPermissionLevelOkay(user,
                        MutePermissions.MUTE_LEVEL_KEY,
                        MutePermissions.BASE_MUTE,
                        this.levelConfig.isCanAffectSameLevel())) {
            // Failure.
            return context.errorResult("command.modifiers.level.insufficient", user.name());
        }

        // Do we have a reason?
        final String rs = reas.orElseGet(() -> context.getMessageString("command.mute.defaultreason"));
        if (this.maxMute > 0 && time.map(Duration::getSeconds).orElse(Long.MAX_VALUE) > this.maxMute &&
                !context.testPermission(MutePermissions.MUTE_EXEMPT_TARGET)) {
            return context.errorResult("command.mute.length.toolong", context.getTimeString(this.maxMute));
        }

        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            context.getAsPlayer().ifPresent(frame::pushCause);
            if (handler.mutePlayer(user.uniqueId(), rs, time.orElse(null))) {
                // Success.
                final Audience mc =
                        context.getServiceCollection().permissionService().permissionMessageChannel(MutePermissions.MUTE_NOTIFY);
                final Audience toSendTo = Audience.audience(mc, context.audience());
                final Mute mute = handler.getPlayerMuteInfo(user.uniqueId()).get(); // we know it exists

                if (time.isPresent()) {
                    this.timedMute(context, user, mute, toSendTo);
                } else {
                    this.permMute(context, user, mute, toSendTo);
                }

                return context.successResult();
            }
        }

        return context.errorResult("command.mute.fail", user.name());
    }

    private void timedMute(
            final ICommandContext context, final User user, final Mute data, final Audience mc) {
        final String ts = context.getTimeString(data.getTimedEntry().get().getRemainingTime());
        mc.sendMessage(context.getMessage("command.mute.success.time", user.name(), context.getName(), ts));
        mc.sendMessage(context.getMessage("standard.reasoncoloured", data.getReason()));

        if (user.isOnline()) {
            context.sendMessageTo(user.player().get(), "mute.playernotify.time", ts);
            context.sendMessageTo(user.player().get(), "command.reason", data.getReason());
        }
    }

    private void permMute(final ICommandContext context, final User user, final Mute data, final Audience mc) {
        mc.sendMessage(context.getMessage("command.mute.success.norm", user.name(), context.getName()));
        mc.sendMessage(context.getMessage("standard.reasoncoloured", data.getReason()));

        if (user.isOnline()) {
            context.sendMessageTo(user.player().get(), "mute.playernotify.standard");
            context.sendMessageTo(user.player().get(), "command.reason", data.getReason());
        }
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        final MuteConfig config = serviceCollection.configProvider().getModuleConfig(MuteConfig.class);
        this.maxMute = config.getMaximumMuteLength();
        this.levelConfig = config.getLevelConfig();
    }
}
