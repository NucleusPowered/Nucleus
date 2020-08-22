/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban.commands;

import io.github.nucleuspowered.nucleus.modules.ban.BanPermissions;
import io.github.nucleuspowered.nucleus.modules.ban.config.BanConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.channel.MutableMessageChannel;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
@NonnullByDefault
public class TempBanCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private BanConfig banConfig = new BanConfig();

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.banConfig = serviceCollection.moduleDataProvider().getModuleConfig(BanConfig.class);
    }

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.ONE_USER.get(serviceCollection),
                NucleusParameters.DURATION.get(serviceCollection),
                NucleusParameters.OPTIONAL_REASON
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User u = context.requireOne(NucleusParameters.Keys.USER, User.class);
        long time = context.requireOne(NucleusParameters.Keys.DURATION, long.class);
        String reason = context.getOne(NucleusParameters.Keys.REASON, String.class)
                .orElseGet(() -> context.getServiceCollection().messageProvider().getMessageString(context.getCommandSourceUnchecked(), "ban.defaultreason"));

        if (!(context.isConsoleAndBypass() || context.testPermissionFor(u, BanPermissions.TEMPBAN_EXEMPT_TARGET))) {
            return context.errorResult("command.tempban.exempt", u.getName());
        }

        if (!u.isOnline() && !context.testPermission(BanPermissions.TEMPBAN_OFFLINE)) {
            return context.errorResult("command.tempban.offline.noperms");
        }

        if (time > this.banConfig.getMaximumTempBanLength()
                && this.banConfig.getMaximumTempBanLength() != -1 &&
                !context.testPermission(BanPermissions.TEMPBAN_EXEMPT_LENGTH)) {
            return context.errorResult("command.tempban.length.toolong",
                    context.getTimeString(this.banConfig.getMaximumTempBanLength()));
        }

        BanService service = Sponge.getServiceManager().provideUnchecked(BanService.class);

        if (service.isBanned(u.getProfile())) {
            return context.errorResult("command.ban.alreadyset", u.getName());
        }

        if (this.banConfig.getLevelConfig().isUseLevels() &&
                !context.isPermissionLevelOkay(u,
                        BanPermissions.BAN_LEVEL_KEY,
                        BanPermissions.BASE_TEMPBAN,
                        this.banConfig.getLevelConfig().isCanAffectSameLevel())) {
            // Failure.
            return context.errorResult("command.modifiers.level.insufficient", u.getName());
        }

        // Expiration date
        Instant date = Instant.now().plus(time, ChronoUnit.SECONDS);

        // Create the ban.
        CommandSource src = context.getCommandSource();
        Ban bp = Ban.builder().type(BanTypes.PROFILE).profile(u.getProfile()).source(src).expirationDate(date).reason(TextSerializers.FORMATTING_CODE.deserialize(reason)).build();
        service.addBan(bp);

        MutableMessageChannel send =
                context.getServiceCollection().permissionService().permissionMessageChannel(BanPermissions.BAN_NOTIFY).asMutable();
        send.addMember(src);
        for (MessageReceiver messageReceiver : send.getMembers()) {
            if (messageReceiver instanceof CommandSource) {
                context.sendMessageTo(messageReceiver,
                        "command.tempban.applied",
                        u.getName(),
                        context.getTimeString(time),
                        src.getName());
                context.sendMessageTo(messageReceiver,
                        "standard.reasoncoloured",
                        reason);
            }
        }

        if (Sponge.getServer().getPlayer(u.getUniqueId()).isPresent()) {
            Sponge.getServer().getPlayer(u.getUniqueId()).get().kick(TextSerializers.FORMATTING_CODE.deserialize(reason));
        }

        return context.successResult();
    }
}
