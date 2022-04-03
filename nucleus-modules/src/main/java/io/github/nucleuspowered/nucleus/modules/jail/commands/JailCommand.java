/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jail;
import io.github.nucleuspowered.nucleus.core.configurate.config.CommonPermissionLevelConfig;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfig;
import io.github.nucleuspowered.nucleus.modules.jail.parameter.JailParameter;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.util.PermissionMessageChannel;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.profile.GameProfile;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

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
public class JailCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private final Parameter.Value<Jail> parameter;
    private CommonPermissionLevelConfig levelConfig = new CommonPermissionLevelConfig();
    private final JailService handler;

    @Inject
    public JailCommand(final INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(JailService.class);
        this.parameter = Parameter.builder(Jail.class)
                .key("jail")
                .addParser(new JailParameter(this.handler, serviceCollection.messageProvider()))
                .build();
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.Composite.USER_OR_GAME_PROFILE,
                this.parameter,
                NucleusParameters.OPTIONAL_DURATION,
                NucleusParameters.OPTIONAL_REASON
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        // Get the subject.
        final User pl = Sponge.server().userManager().loadOrCreate(
                NucleusParameters.Composite.parseUserOrGameProfile(context).fold(Function.identity(), GameProfile::uuid)).join();
        if (!pl.isOnline() && !context.testPermission(JailPermissions.JAIL_OFFLINE)) {
            return context.errorResult("command.jail.offline.noperms");
        }

        if (this.levelConfig.isUseLevels() &&
                !context.isPermissionLevelOkay(pl,
                        JailPermissions.JAIL_LEVEL_KEY,
                        JailPermissions.BASE_JAIL,
                        this.levelConfig.isCanAffectSameLevel())) {
            // Failure.
            return context.errorResult("command.modifiers.level.insufficient", pl.name());
        }

        if (this.handler.isPlayerJailed(pl.uniqueId())) {
            return context.errorResult("command.jail.alreadyjailed", pl.name());
        }

        if (!context.isConsoleAndBypass() && context.testPermissionFor(pl, JailPermissions.JAIL_EXEMPT_TARGET)) {
            return context.errorResult("command.jail.exempt", pl.name());
        }

        return this.onJail(context, pl);
    }

    private ICommandResult onJail(final ICommandContext context, final User user) {
        final Jail jail = context.requireOne(this.parameter);

        // This might not be there.
        final Optional<Duration> duration = context.getOne(NucleusParameters.DURATION);
        final String reason = context.getOne(NucleusParameters.REASON)
                .orElseGet(() -> context.getMessageString("command.jail.reason"));
        final Component message;
        final Component messageTo;

        final boolean success = this.handler.jailPlayer(
                user.uniqueId(),
                jail,
                reason,
                duration.orElse(null)
        );

        if (success) {
            if (duration.isPresent()) {
                final IMessageProviderService messageProviderService = context.getServiceCollection().messageProvider();
                message = context.getMessage("command.checkjail.jailedfor", user.name(), jail.getName(),
                        context.getName(), messageProviderService.getTimeString(context.audience(), duration.get()));
                messageTo = context.getMessage("command.jail.jailedfor", jail.getName(), context.getName(),
                        messageProviderService.getTimeString(context.getLocale(), duration.get()));
            } else {
                message = context.getMessage("command.checkjail.jailedperm", user.name(), jail.getName(), context.getName());
                messageTo = context.getMessage("command.jail.jailedperm", jail.getName(), context.getName());
            }

            final Audience audience = Audience.audience(
                    new PermissionMessageChannel(context.getServiceCollection().permissionService(), JailPermissions.JAIL_NOTIFY),
                    context.audience());

            audience.sendMessage(message);
            audience.sendMessage(context.getMessage("standard.reasoncoloured", reason));

            user.player().ifPresent(x -> {
                x.sendMessage(messageTo);
                x.sendMessage(context.getMessage("standard.reasoncoloured", reason));
            });

            return context.successResult();
        }

        return context.errorResult("command.jail.error");
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.levelConfig = serviceCollection.configProvider().getModuleConfig(JailConfig.class).getCommonPermissionLevelConfig();
    }
}
