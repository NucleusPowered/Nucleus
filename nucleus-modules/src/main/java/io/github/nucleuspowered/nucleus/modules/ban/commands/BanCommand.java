/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban.commands;

import io.github.nucleuspowered.nucleus.core.configurate.config.CommonPermissionLevelConfig;
import io.github.nucleuspowered.nucleus.modules.ban.BanPermissions;
import io.github.nucleuspowered.nucleus.modules.ban.config.BanConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileManager;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.ban.BanTypes;
import org.spongepowered.api.user.UserManager;

import java.util.Optional;

@Command(
        aliases = "ban",
        basePermission = BanPermissions.BASE_BAN,
        commandDescriptionKey = "ban",
        associatedPermissions = {
                BanPermissions.BAN_OFFLINE,
                BanPermissions.BAN_EXEMPT_TARGET,
                BanPermissions.BAN_NOTIFY
        },
        associatedPermissionLevelKeys = {
                BanPermissions.BAN_LEVEL_KEY
        }
)
@EssentialsEquivalent("ban")
public class BanCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private CommonPermissionLevelConfig levelConfig = new CommonPermissionLevelConfig();

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                Parameter.firstOf(
                        NucleusParameters.ONE_USER,
                        NucleusParameters.STRING_NAME
                ),
                NucleusParameters.OPTIONAL_REASON
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final String r = context.getOne(NucleusParameters.REASON)
                .orElseGet(() -> context.getMessageString("ban.defaultreason"));

        final Optional<? extends User> optionalUser = context.getOne(NucleusParameters.ONE_USER);
        if (optionalUser.isPresent()) {
            // power check
            if (!optionalUser.get().isOnline() && !context.testPermission(BanPermissions.BAN_OFFLINE)) {
                return context.errorResult("command.ban.offline.noperms");
            }

            if (!context.isConsoleAndBypass() && context.testPermissionFor(optionalUser.get(), BanPermissions.BAN_EXEMPT_TARGET)) {
                return context.errorResult("command.ban.exempt", optionalUser.get().name());
            }

            return this.executeBan(context, optionalUser.get(), r);
        }

        if (!context.testPermission(BanPermissions.BAN_OFFLINE)) {
            return context.errorResult("command.ban.offline.noperms");
        }

        final String userToFind = context.requireOne(NucleusParameters.STRING_NAME);

        // Get the profile async.
        Sponge.asyncScheduler().createExecutor(context.getServiceCollection().pluginContainer()).execute(() -> {
            final GameProfileManager gpm = Sponge.server().gameProfileManager();
            try {
                final GameProfile gp = gpm.profile(userToFind).get();

                // Ban the user sync.
                Sponge.server().scheduler().createExecutor(context.getServiceCollection().pluginContainer()).execute(() -> {
                    // Create the user.
                    final UserManager uss = Sponge.server().userManager();
                    final User user = uss.findOrCreate(gp);
                    context.sendMessage("gameprofile.new", user.name());

                    try {
                        this.executeBan(context, user, r);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (final Exception e) {
                e.printStackTrace();
                context.sendMessage("command.ban.profileerror", userToFind);
            }
        });

        return context.successResult();
    }

    private ICommandResult executeBan(final ICommandContext context, final User user, final String r) {
        final BanService service = Sponge.server().serviceProvider().banService();

        if (!user.isOnline() && !context.testPermission(BanPermissions.BAN_OFFLINE)) {
            return context.errorResult("command.ban.offline.noperms");
        }

        // TODO: Fix joins to be async
        if (service.find(user.profile()).join().isPresent()) {
            return context.errorResult("command.ban.alreadyset",
                    context.getServiceCollection().playerDisplayNameService().getName(user));
        }

        if (this.levelConfig.isUseLevels() &&
                !context.isPermissionLevelOkay(user,
                        BanPermissions.BAN_LEVEL_KEY,
                        BanPermissions.BASE_BAN,
                        this.levelConfig.isCanAffectSameLevel())) {
            // Failure.
            return context.errorResult("command.modifiers.level.insufficient",
                    context.getServiceCollection().playerDisplayNameService().getName(user));
        }

        // Create the ban.
        final Ban bp = Ban.builder().type(BanTypes.PROFILE).profile(user.profile())
                .source(context.getDisplayName())
                .reason(LegacyComponentSerializer.legacyAmpersand().deserialize(r)).build();
        service.add(bp);

        // Get the permission, "quickstart.ban.notify"
        final Audience audience = Audience.audience(
                context.audience(),
                context.getServiceCollection().permissionService().permissionMessageChannel(BanPermissions.BAN_NOTIFY));
        audience.sendMessage(context.getMessage("command.ban.applied",
                context.getServiceCollection().playerDisplayNameService().getName(user),
                context.getDisplayName()));
        final Component reason = LegacyComponentSerializer.legacyAmpersand().deserialize(r);
        audience.sendMessage(context.getMessage("standard.reasoncoloured", reason));

        Sponge.server().player(user.uniqueId()).ifPresent(pl -> pl.kick(reason));
        return context.successResult();
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.levelConfig = serviceCollection.configProvider().getModuleConfig(BanConfig.class).getLevelConfig();
    }
}
