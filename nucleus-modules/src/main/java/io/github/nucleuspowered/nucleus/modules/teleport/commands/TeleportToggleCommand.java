/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import io.github.nucleuspowered.nucleus.modules.teleport.TeleportPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserPreferenceService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@EssentialsEquivalent("tptoggle")
@Command(
        aliases = "tptoggle",
        basePermission = TeleportPermissions.BASE_TPTOGGLE,
        commandDescriptionKey = "tptoggle",
        async = true,
        associatedPermissions = TeleportPermissions.TPTOGGLE_EXEMPT
)
public class TeleportToggleCommand implements ICommandExecutor<Player> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection service) {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        IUserPreferenceService ups = context.getServiceCollection().userPreferenceService();
        Player pl = context.getIfPlayer();
        boolean toggle = ups.get(pl.getUniqueId(), NucleusKeysProvider.TELEPORT_TARGETABLE).get(); // we know it's always there
        boolean flip = context.getOne(NucleusParameters.Keys.BOOL, Boolean.class).orElseGet(() -> !toggle);
        ups.set(pl.getUniqueId(), NucleusKeysProvider.TELEPORT_TARGETABLE, flip);
        context.sendMessage(
                "command.tptoggle.success", flip ? "loc:standard.enabled" : "loc:standard.disabled"
        );
        return context.successResult();
    }
}
