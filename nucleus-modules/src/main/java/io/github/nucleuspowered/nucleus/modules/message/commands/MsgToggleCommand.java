/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.commands;

import io.github.nucleuspowered.nucleus.modules.message.MessagePermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.UserPreferenceService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.UUID;

@NonnullByDefault
@Command(
        aliases = {"msgtoggle", "messagetoggle", "mtoggle"},
        basePermission = MessagePermissions.BASE_MSGTOGGLE,
        commandDescriptionKey = "msgtoggle",
        associatedPermissions = {
                MessagePermissions.MSGTOGGLE_BYPASS
        }
)
public class MsgToggleCommand implements ICommandExecutor<Player> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        UserPreferenceService userPreferenceService = context.getServiceCollection().getServiceUnchecked(UserPreferenceService.class);
        final UUID player = context.getIfPlayer().getUniqueId();
        boolean flip = context.getOne(NucleusParameters.Keys.BOOL, Boolean.class)
                .orElseGet(() -> userPreferenceService.getUnwrapped(player, NucleusKeysProvider.RECEIVING_MESSAGES));

        userPreferenceService.set(player, NucleusKeysProvider.RECEIVING_MESSAGES, flip);
        context.sendMessage("command.msgtoggle.success." + flip);

        return context.successResult();
    }
}
