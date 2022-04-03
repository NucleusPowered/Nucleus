/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban.commands;

import io.github.nucleuspowered.nucleus.core.Util;
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
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanService;

import java.util.Optional;

@Command(
        aliases = {"unban", "pardon"},
        basePermission = BanPermissions.BASE_TEMPBAN,
        commandDescriptionKey = "unban",
        associatedPermissionLevelKeys = BanPermissions.BAN_LEVEL_KEY
)
@EssentialsEquivalent({"unban", "pardon"})
public class UnbanCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private CommonPermissionLevelConfig levelConfig = new CommonPermissionLevelConfig();

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.GAME_PROFILE
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final GameProfile profile = context.requireOne(NucleusParameters.GAME_PROFILE);
        final BanService service = Sponge.server().serviceProvider().banService();

        // TODO: Async
        final Optional<Ban.Profile> obp = service.find(profile).join();
        if (!obp.isPresent()) {
            return context.errorResult(
                    "command.checkban.notset", Util.getNameOrUnkown(context, profile));
        }

        if (this.levelConfig.isUseLevels() &&
                !context.isPermissionLevelOkay(profile.uuid(),
                        BanPermissions.BAN_LEVEL_KEY,
                        BanPermissions.BASE_UNBAN,
                        this.levelConfig.isCanAffectSameLevel())) {
            // Failure.
            return context.errorResult("command.modifiers.level.insufficient", profile.name());
        }

        service.remove(obp.get());

        final Audience send = Audience.audience(
                context.audience(),
                context.getServiceCollection().permissionService().permissionMessageChannel(BanPermissions.BAN_NOTIFY)
        );
        send.sendMessage(context.getMessage("command.unban.success", Util.getNameOrUnkown(context, obp.get().profile()), context.getName()));
        return context.successResult();
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        this.levelConfig = serviceCollection.configProvider().getModuleConfig(BanConfig.class).getLevelConfig();
    }
}
