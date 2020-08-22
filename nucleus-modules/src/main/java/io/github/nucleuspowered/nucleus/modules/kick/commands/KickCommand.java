/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kick.commands;

import io.github.nucleuspowered.nucleus.configurate.config.CommonPermissionLevelConfig;
import io.github.nucleuspowered.nucleus.modules.kick.KickPermissions;
import io.github.nucleuspowered.nucleus.modules.kick.config.KickConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@EssentialsEquivalent("kick")
@NonnullByDefault
@Command(
        aliases = "kick",
        basePermission = KickPermissions.BASE_KICK,
        commandDescriptionKey = "kick",
        associatedPermissionLevelKeys = KickPermissions.LEVEL_KEY,
        associatedPermissions = {
                KickPermissions.KICK_EXEMPT_TARGET,
                KickPermissions.KICK_NOTIFY
        }
)
public class KickCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private CommonPermissionLevelConfig levelConfig = new CommonPermissionLevelConfig();

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.ONE_PLAYER.get(serviceCollection),
                NucleusParameters.OPTIONAL_REASON
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player pl = context.requireOne(NucleusParameters.Keys.PLAYER, Player.class);
        String r = context.getOne(NucleusParameters.Keys.REASON, String.class)
                .orElseGet(() -> context.getMessageString("command.kick.defaultreason"));

        if (context.isConsoleAndBypass() || context.testPermissionFor(pl, KickPermissions.KICK_EXEMPT_TARGET)) {
            return context.errorResult("command.kick.exempt", pl.getName());
        }

        User user = context.requireOne(NucleusParameters.Keys.USER, User.class);
        if (this.levelConfig.isUseLevels() &&
                !context.isPermissionLevelOkay(user,
                        KickPermissions.LEVEL_KEY,
                        KickPermissions.BASE_KICK,
                        this.levelConfig.isCanAffectSameLevel())) {
            // Failure.
            return context.errorResult("command.modifiers.level.insufficient", user.getName());
        }

        pl.kick(TextSerializers.FORMATTING_CODE.deserialize(r));

        MessageChannel messageChannel = context.getServiceCollection().permissionService().permissionMessageChannel(KickPermissions.KICK_NOTIFY);
        messageChannel.send(context.getCommandSource(), context.getMessage("command.kick.message", pl.getName(), context.getName()));
        messageChannel.send(context.getCommandSource(), context.getMessage("command.reason", r));
        return context.successResult();
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.levelConfig = serviceCollection.moduleDataProvider().getModuleConfig(KickConfig.class).getLevelConfig();
    }
}
