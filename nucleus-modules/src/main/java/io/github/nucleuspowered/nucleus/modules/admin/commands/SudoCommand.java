/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands;

import io.github.nucleuspowered.nucleus.configurate.config.CommonPermissionLevelConfig;
import io.github.nucleuspowered.nucleus.modules.admin.AdminModule;
import io.github.nucleuspowered.nucleus.modules.admin.AdminPermissions;
import io.github.nucleuspowered.nucleus.modules.admin.config.AdminConfig;
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
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@Command(aliases = {"sudo"},
        basePermission = AdminPermissions.BASE_SUDO,
        commandDescriptionKey = "sudo",
        associatedPermissionLevelKeys = AdminModule.SUDO_LEVEL_KEY)
@EssentialsEquivalent("sudo")
@NonnullByDefault
public class SudoCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private CommonPermissionLevelConfig levelConfig = new CommonPermissionLevelConfig();

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[]{
                NucleusParameters.ONE_PLAYER.get(serviceCollection),
                NucleusParameters.COMMAND
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player pl = context.requireOne(NucleusParameters.Keys.PLAYER, Player.class);
        String cmd = context.requireOne(NucleusParameters.Keys.COMMAND, String.class);
        if (context.is(pl) || (!context.isConsoleAndBypass() && context.testPermissionFor(pl, AdminPermissions.SUDO_EXEMPT))) {
            return context.errorResult("command.sudo.noperms");
        }

        if (this.levelConfig.isUseLevels() &&
                !context.isPermissionLevelOkay(pl,
                        AdminModule.SUDO_LEVEL_KEY,
                        AdminPermissions.BASE_SUDO,
                        this.levelConfig.isCanAffectSameLevel())) {
            // Failure.
            return context.errorResult("command.modifiers.level.insufficient", pl.getName());
        }

        if (cmd.startsWith("c:")) {
            if (cmd.equals("c:")) {
                return context.errorResult("command.sudo.chatfail");
            }

            Text rawMessage = Text.of(cmd.split(":", 2)[1]);
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                frame.pushCause(context.getCommandSource());
                frame.pushCause(pl); // on top
                frame.addContext(EventContextKeys.PLAYER_SIMULATED, pl.getProfile());

                if (pl.simulateChat(rawMessage, frame.getCurrentCause()).isCancelled()) {
                    return context.errorResult("command.sudo.chatcancelled");
                }
            }

            return context.successResult();
        }

        context.sendMessage("command.sudo.force", pl.getName(), cmd);
        Sponge.getCommandManager().process(pl, cmd);
        return context.successResult();
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.levelConfig = serviceCollection.moduleDataProvider().getModuleConfig(AdminConfig.class).getLevelConfig();
    }
}
