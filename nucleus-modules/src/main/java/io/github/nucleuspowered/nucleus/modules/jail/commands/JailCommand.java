/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.configurate.config.CommonPermissionLevelConfig;
import io.github.nucleuspowered.nucleus.datatypes.LocationData;
import io.github.nucleuspowered.nucleus.modules.jail.JailParameters;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfig;
import io.github.nucleuspowered.nucleus.modules.jail.data.JailData;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.util.PermissionMessageChannel;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MutableMessageChannel;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Locatable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.inject.Inject;

@NonnullByDefault
@Command(
        aliases = {"jail"},
        basePermission = JailPermissions.BASE_JAIL,
        commandDescriptionKey = "jail",
        associatedPermissions = {
                JailPermissions.JAIL_NOTIFY,
                JailPermissions.JAIL_OFFLINE,
                JailPermissions.JAIL_EXEMPT_TARGET,
                JailPermissions.JAIL_TELEPORTJAILED,
                JailPermissions.JAIL_TELEPORTTOJAILED
        },
        associatedPermissionLevelKeys = JailPermissions.JAIL_LEVEL_KEY
)
@EssentialsEquivalent(value = {"togglejail", "tjail", "jail"}, isExact = false, notes = "This command is not a toggle.")
public class JailCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private CommonPermissionLevelConfig levelConfig = new CommonPermissionLevelConfig();
    private final JailHandler handler;

    @Inject
    public JailCommand(INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(JailHandler.class);
    }

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.ONE_USER.get(serviceCollection),
                JailParameters.OPTIONAL_JAIL.get(serviceCollection),
                NucleusParameters.OPTIONAL_WEAK_DURATION.get(serviceCollection),
                NucleusParameters.OPTIONAL_REASON
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        // Get the subject.
        User pl = context.requireOne(NucleusParameters.Keys.USER, User.class);
        if (!pl.isOnline() && !context.testPermission(JailPermissions.JAIL_OFFLINE)) {
            return context.errorResult("command.jail.offline.noperms");
        }

        if (this.levelConfig.isUseLevels() &&
                !context.isPermissionLevelOkay(pl,
                        JailPermissions.JAIL_LEVEL_KEY,
                        JailPermissions.BASE_JAIL,
                        this.levelConfig.isCanAffectSameLevel())) {
            // Failure.
            return context.errorResult("command.modifiers.level.insufficient", pl.getName());
        }

        if (this.handler.isPlayerJailed(pl)) {
            return context.errorResult("command.jail.alreadyjailed", pl.getName());
        }

        if (!context.isConsoleAndBypass() && context.testPermission(JailPermissions.JAIL_EXEMPT_TARGET)) {
            return context.errorResult("command.jail.exempt", pl.getName());
        }

        return onJail(context, pl);
    }

    private ICommandResult onJail(ICommandContext<? extends CommandSource> context, User user) throws CommandException {
        Optional<LocationData> owl = context.getOne(JailParameters.JAIL_KEY, LocationData.class);
        if (!owl.isPresent()) {
            return context.errorResult("command.jail.jail.nojail");
        }

        // This might not be there.
        Optional<Long> duration = context.getOne(NucleusParameters.Keys.DURATION, Long.class);
        String reason = context.getOne(NucleusParameters.Keys.REASON, String.class)
                .orElseGet(() -> context.getMessageString("command.jail.reason"));
        JailData jd;
        Text message;
        Text messageTo;

        CommandSource src = context.getCommandSource();
        if (duration.isPresent()) {
            if (user.isOnline()) {
                jd = new JailData(Util.getUUID(src), owl.get().getName(), reason, user.getPlayer().get().getLocation(),
                        Instant.now().plusSeconds(duration.get()));
            } else {
                jd = new JailData(Util.getUUID(src), owl.get().getName(), reason, null, Duration.of(duration.get(), ChronoUnit.SECONDS));
            }

            IMessageProviderService messageProviderService = context.getServiceCollection().messageProvider();
            message = context.getMessage("command.checkjail.jailedfor", user.getName(), jd.getJailName(),
                    src.getName(), messageProviderService.getTimeString(src.getLocale(), duration.get()));
            messageTo = context.getMessage("command.jail.jailedfor", owl.get().getName(), src.getName(),
                    messageProviderService.getTimeString(src.getLocale(), duration.get()));
        } else {
            jd = new JailData(Util.getUUID(src), owl.get().getName(), reason, user.getPlayer().map(Locatable::getLocation).orElse(null));
            message = context.getMessage("command.checkjail.jailedperm", user.getName(), owl.get().getName(), src.getName());
            messageTo = context.getMessage("command.jail.jailedperm", owl.get().getName(), src.getName());
        }

        if (this.handler.jailPlayer(user, jd)) {
            MutableMessageChannel mc = new PermissionMessageChannel(context.getServiceCollection().permissionService(),
                    JailPermissions.JAIL_NOTIFY).asMutable();
            mc.addMember(src);
            mc.send(message);
            mc.send(context.getMessage("standard.reasoncoloured", reason));

            user.getPlayer().ifPresent(x -> {
                x.sendMessage(messageTo);
                x.sendMessage(context.getMessage("standard.reasoncoloured", reason));
            });

            return context.successResult();
        }

        return context.errorResult("command.jail.error");
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        boolean requireUnjailPermission = serviceCollection.moduleDataProvider().getModuleConfig(JailConfig.class).isRequireUnjailPermission();
        this.levelConfig = serviceCollection.moduleDataProvider().getModuleConfig(JailConfig.class).getCommonPermissionLevelConfig();
    }
}
