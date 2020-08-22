/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban.commands;

import io.github.nucleuspowered.nucleus.configurate.config.CommonPermissionLevelConfig;
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
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileManager;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MutableMessageChannel;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;

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
@NonnullByDefault
public class BanCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private CommonPermissionLevelConfig levelConfig = new CommonPermissionLevelConfig();
    private final String name = "name";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.firstParsing(
                        NucleusParameters.ONE_GAME_PROFILE_UUID.get(serviceCollection),
                        NucleusParameters.ONE_GAME_PROFILE.get(serviceCollection),
                        GenericArguments.onlyOne(GenericArguments.string(Text.of(this.name)))
                ),
                GenericArguments.optionalWeak(NucleusParameters.REASON)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        final String r = context.getOne(NucleusParameters.Keys.REASON, String.class).orElseGet(() ->
                context.getMessageString("ban.defaultreason"));

        Optional<GameProfile> ou = context.getOne(NucleusParameters.Keys.USER_UUID, GameProfile.class);
        if (!ou.isPresent()) {
            ou = context.getOne(NucleusParameters.Keys.USER, GameProfile.class);
        }

        if (ou.isPresent()) {
            // power check
            Optional<User> optionalUser = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(ou.get());
            if ((!optionalUser.isPresent() || !optionalUser.get().isOnline()) && !context.testPermission(BanPermissions.BAN_OFFLINE)) {
                return context.errorResult("command.ban.offline.noperms");
            }

            if (optionalUser.isPresent() &&
                    (!context.isConsoleAndBypass() || context.testPermissionFor(optionalUser.get(), BanPermissions.BAN_EXEMPT_TARGET))) {
                return context.errorResult("command.ban.exempt", optionalUser.get().getName());
            }

            return executeBan(context, ou.get(), r);
        }

        if (!context.testPermission(BanPermissions.BAN_OFFLINE)) {
            return context.errorResult("command.ban.offline.noperms");
        }

        final String userToFind = context.requireOne(this.name, String.class);

        // Get the profile async.
        Sponge.getScheduler().createAsyncExecutor(context.getServiceCollection().pluginContainer()).execute(() -> {
            GameProfileManager gpm = Sponge.getServer().getGameProfileManager();
            try {
                GameProfile gp = gpm.get(userToFind).get();

                // Ban the user sync.
                Sponge.getScheduler().createSyncExecutor(context.getServiceCollection().pluginContainer()).execute(() -> {
                    // Create the user.
                    UserStorageService uss = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
                    User user = uss.getOrCreate(gp);
                    context.sendMessage("gameprofile.new", user.getName());

                    try {
                        executeBan(context, gp, r);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                context.sendMessage("command.ban.profileerror", userToFind);
            }
        });

        return context.successResult();
    }

    private ICommandResult executeBan(ICommandContext<? extends CommandSource> context, GameProfile u, String r) {
        BanService service = Sponge.getServiceManager().provideUnchecked(BanService.class);
        CommandSource src = context.getCommandSourceUnchecked();

        UserStorageService uss = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
        User user = uss.get(u).get();
        if (!user.isOnline() && !context.testPermission(BanPermissions.BAN_OFFLINE)) {
            return context.errorResult("command.ban.offline.noperms");
        }

        if (service.isBanned(u)) {
            return context.errorResult("command.ban.alreadyset",
                    u.getName().orElse(context.getServiceCollection().messageProvider()
                            .getMessageString(src,"standard.unknown")));
        }

        if (this.levelConfig.isUseLevels() &&
                !context.isPermissionLevelOkay(user,
                        BanPermissions.BAN_LEVEL_KEY,
                        BanPermissions.BASE_BAN,
                        this.levelConfig.isCanAffectSameLevel())) {
            // Failure.
            return context.errorResult("command.modifiers.level.insufficient",
                    u.getName().orElse(context.getServiceCollection().messageProvider()
                            .getMessageString(src,"standard.unknown")));
        }

        // Create the ban.
        Ban bp = Ban.builder().type(BanTypes.PROFILE).profile(u)
                .source(src)
                .reason(TextSerializers.FORMATTING_CODE.deserialize(r)).build();
        service.addBan(bp);

        // Get the permission, "quickstart.ban.notify"
        MutableMessageChannel send = context.getServiceCollection().permissionService().permissionMessageChannel(BanPermissions.BAN_NOTIFY).asMutable();
        send.addMember(src);
        send.send(context.getMessage("command.ban.applied",
                u.getName().orElse(context.getMessageString("standard.unknown")),
                src.getName()));
        send.send(context.getMessage("standard.reasoncoloured", r));

        if (Sponge.getServer().getPlayer(u.getUniqueId()).isPresent()) {
            Sponge.getServer().getPlayer(u.getUniqueId()).get().kick(TextSerializers.FORMATTING_CODE.deserialize(r));
        }

        return context.successResult();
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.levelConfig = serviceCollection.moduleDataProvider().getModuleConfig(BanConfig.class).getLevelConfig();
    }
}
