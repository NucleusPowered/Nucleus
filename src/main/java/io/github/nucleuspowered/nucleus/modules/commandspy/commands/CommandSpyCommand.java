/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandspy.commands;

import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.commandspy.CommandSpyUserPrefKeys;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Map;

@Permissions
@RegisterCommand("commandspy")
@NonnullByDefault
public class CommandSpyCommand extends AbstractCommand<Player> {

    @Override protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> mspi = super.permissionSuffixesToRegister();
        mspi.put("exempt.target", PermissionInformation.getWithTranslation("permission.commandspy.exempt.target", SuggestedLevel.ADMIN));
        return mspi;
    }

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override public CommandResult executeCommand(Player src, CommandContext args, Cause cause) {
        UserPreferenceService ups = getServiceUnchecked(UserPreferenceService.class);
        boolean to = args.<Boolean>getOne(NucleusParameters.Keys.BOOL)
                .orElseGet(() -> !ups.getUnwrapped(src.getUniqueId(), CommandSpyUserPrefKeys.COMMAND_SPY));
        ups.set(src.getUniqueId(), CommandSpyUserPrefKeys.COMMAND_SPY, to);
        sendMessageTo(src, "command.commandspy.success", getMessageFor(src, to ? "standard.enabled" : "standard.disabled"));

        return CommandResult.success();
    }
}
