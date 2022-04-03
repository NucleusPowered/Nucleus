/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.commands;

import io.github.nucleuspowered.nucleus.core.configurate.config.CommonPermissionLevelConfig;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfig;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.profile.GameProfile;

import java.util.UUID;
import java.util.function.Function;

@Command(
        aliases = {"unjail"},
        basePermission = JailPermissions.JAIL_UNJAIL,
        commandDescriptionKey = "unjail",
        associatedPermissionLevelKeys = JailPermissions.JAIL_LEVEL_KEY
)
@EssentialsEquivalent(value = "unjail", isExact = false, notes = "Not a toggle.")
public class UnjailCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private CommonPermissionLevelConfig levelConfig = new CommonPermissionLevelConfig();

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.Composite.USER_OR_GAME_PROFILE
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final User user =
                Sponge.server().userManager().loadOrCreate(
                        NucleusParameters.Composite.parseUserOrGameProfile(context)
                                .fold(Function.identity(), GameProfile::uuid)).join();
        if (this.levelConfig.isUseLevels() &&
                !context.isPermissionLevelOkay(user,
                        JailPermissions.JAIL_LEVEL_KEY,
                        JailPermissions.BASE_JAIL,
                        this.levelConfig.isCanAffectSameLevel())) {
            // Failure.
            return context.errorResult("command.modifiers.level.insufficient", user.name());
        }

        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            if (context.getServiceCollection().getServiceUnchecked(JailService.class).unjailPlayer(user.uniqueId())) {
                context.sendMessage("command.jail.unjail.success", user.name());
                return context.successResult();
            } else {
                return context.errorResult("command.jail.unjail.fail", user.name());
            }
        }
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.levelConfig = serviceCollection.configProvider().getModuleConfig(JailConfig.class).getCommonPermissionLevelConfig();
    }
}
