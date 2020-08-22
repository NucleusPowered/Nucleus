/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ignore.commands;

import io.github.nucleuspowered.nucleus.modules.ignore.IgnorePermissions;
import io.github.nucleuspowered.nucleus.modules.ignore.services.IgnoreService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@EssentialsEquivalent("ignore")
@NonnullByDefault
@Command(
        aliases = { "ignore" },
        basePermission = IgnorePermissions.BASE_IGNORE,
        commandDescriptionKey = "ignore",
        async = true,
        associatedPermissions = IgnorePermissions.IGNORE_CHAT
)
public class IgnoreCommand implements ICommandExecutor<Player> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.ONE_USER.get(serviceCollection),
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        // Get the target
        final User target = context.requireOne(NucleusParameters.Keys.USER, User.class);
        final Player player = context.getIfPlayer();

        if (context.is(target)) {
            return context.errorResult("command.ignore.self");
        }

        final IgnoreService ignoreService = context.getServiceCollection().getServiceUnchecked(IgnoreService.class);
        if (context.testPermissionFor(target, "exempt.chat")) {
            // Make sure they are removed.
            ignoreService.unignore(player.getUniqueId(), target.getUniqueId());
            return context.errorResult("command.ignore.exempt", target.getName());
        }

        // Ok, we can ignore or unignore them.
        boolean ignore = context.getOne(NucleusParameters.Keys.BOOL, Boolean.class)
                .orElseGet(() -> !ignoreService.isIgnored(player.getUniqueId(), target.getUniqueId()));

        if (ignore) {
            ignoreService.ignore(player.getUniqueId(), target.getUniqueId());
            context.sendMessage("command.ignore.added", target.getName());
        } else {
            ignoreService.unignore(player.getUniqueId(), target.getUniqueId());
            context.sendMessage("command.ignore.remove", target.getName());
        }

        return context.successResult();
    }
}
