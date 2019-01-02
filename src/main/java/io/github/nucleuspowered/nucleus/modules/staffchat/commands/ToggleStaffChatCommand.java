/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat.commands;

import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.staffchat.StaffChatUserPrefKeys;
import io.github.nucleuspowered.nucleus.modules.staffchat.services.StaffChatService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@Permissions(suggestedLevel = SuggestedLevel.MOD, mainOverride = "staffchat")
@NoModifiers
@RegisterCommand({"toggleviewstaffchat", "vsc", "togglevsc"})
@NonnullByDefault
public class ToggleStaffChatCommand extends AbstractCommand<Player> {

    private final StaffChatService staffChatService = getServiceUnchecked(StaffChatService.class);

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws Exception {
        UserPreferenceService ups = getServiceUnchecked(UserPreferenceService.class);
        boolean result =
                args.<Boolean>getOne(NucleusParameters.Keys.BOOL).orElseGet(() ->
                    ups.getPreferenceFor(src, StaffChatUserPrefKeys.VIEW_STAFF_CHAT).orElse(true));
        ups.setPreferenceFor(src, StaffChatUserPrefKeys.VIEW_STAFF_CHAT, !result);

        if (!result && this.staffChatService.isToggledChat(src)) {
            this.staffChatService.toggle(src, false);
        }

        sendMessageTo(src, "command.staffchat.view." + (result ? "on" : "off"));
        return CommandResult.success();
    }

}
