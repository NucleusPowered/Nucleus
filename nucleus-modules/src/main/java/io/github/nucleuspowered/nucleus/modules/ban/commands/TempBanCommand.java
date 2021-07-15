/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban.commands;

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
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.ban.BanTypes;

import java.time.Duration;
import java.time.Instant;

@Command(
        aliases = "tempban",
        basePermission = BanPermissions.BASE_TEMPBAN,
        commandDescriptionKey = "tempban",
        associatedPermissions = {
                BanPermissions.TEMPBAN_EXEMPT_LENGTH,
                BanPermissions.TEMPBAN_EXEMPT_TARGET,
                BanPermissions.TEMPBAN_OFFLINE,
        },
        associatedPermissionLevelKeys = BanPermissions.BAN_LEVEL_KEY
)
@EssentialsEquivalent("tempban")
public class TempBanCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private BanConfig banConfig = new BanConfig();

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        this.banConfig = serviceCollection.configProvider().getModuleConfig(BanConfig.class);
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.ONE_USER,
                NucleusParameters.DURATION,
                NucleusParameters.OPTIONAL_REASON
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final User u = context.requireOne(NucleusParameters.ONE_USER);
        final Duration time = context.requireOne(NucleusParameters.DURATION);
        final String reason = context.getOne(NucleusParameters.OPTIONAL_REASON)
                .orElseGet(() -> context.getMessageString("ban.defaultreason"));

        if (!context.isConsoleAndBypass() && context.testPermissionFor(u, BanPermissions.TEMPBAN_EXEMPT_TARGET)) {
            return context.errorResult("command.tempban.exempt", u.name());
        }

        if (!u.isOnline() && !context.testPermission(BanPermissions.TEMPBAN_OFFLINE)) {
            return context.errorResult("command.tempban.offline.noperms");
        }

        if (time.getSeconds() > this.banConfig.getMaximumTempBanLength()
                && this.banConfig.getMaximumTempBanLength() != -1 &&
                !context.testPermission(BanPermissions.TEMPBAN_EXEMPT_LENGTH)) {
            return context.errorResult("command.tempban.length.toolong",
                    context.getTimeString(this.banConfig.getMaximumTempBanLength()));
        }

        final BanService service = Sponge.server().serviceProvider().banService();

        // TODO: Fix joins to be async
        if (service.find(u.profile()).join().isPresent()) {
            return context.errorResult("command.ban.alreadyset", u.name());
        }

        if (this.banConfig.getLevelConfig().isUseLevels() &&
                !context.isPermissionLevelOkay(u,
                        BanPermissions.BAN_LEVEL_KEY,
                        BanPermissions.BASE_TEMPBAN,
                        this.banConfig.getLevelConfig().isCanAffectSameLevel())) {
            // Failure.
            return context.errorResult("command.modifiers.level.insufficient", u.name());
        }

        // Expiration date
        final Instant date = Instant.now().plus(time);

        // Create the ban.
        final Component src = context.getDisplayName();
        final Component r = LegacyComponentSerializer.legacyAmpersand().deserialize(reason);
        final Ban bp = Ban.builder().type(BanTypes.PROFILE).profile(u.profile()).source(src).expirationDate(date).reason(r).build();
        service.add(bp);

        final Audience send = Audience.audience(
                context.audience(),
                context.getServiceCollection().permissionService().permissionMessageChannel(BanPermissions.BAN_NOTIFY)
        );
        send.sendMessage(context.getMessage("command.tempban.applied",
                        context.getDisplayName(u.uniqueId()),
                        context.getTimeString(time),
                        src));
        send.sendMessage(context.getMessage("standard.reasoncoloured", reason));

        Sponge.server().player(u.uniqueId()).ifPresent(x -> x.kick(r));
        return context.successResult();
    }
}
