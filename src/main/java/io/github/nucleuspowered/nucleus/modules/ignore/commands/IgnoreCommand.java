/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ignore.commands;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.ignore.services.IgnoreService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Map;

@RunAsync
@NoModifiers
@RegisterCommand("ignore")
@Permissions(suggestedLevel = SuggestedLevel.USER)
@EssentialsEquivalent("ignore")
@NonnullByDefault
public class IgnoreCommand extends AbstractCommand<Player> {

    private final IgnoreService ignoreService = getServiceUnchecked(IgnoreService.class);

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = Maps.newHashMap();
        m.put("exempt.chat", PermissionInformation.getWithTranslation("permission.ignore.chat", SuggestedLevel.MOD));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                NucleusParameters.ONE_USER,
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) {
        // Get the target
        User target = args.requireOne(NucleusParameters.Keys.USER);

        if (target.equals(src)) {
            sendMessageTo(src, "command.ignore.self");
            return CommandResult.empty();
        }

        if (this.permissions.testSuffix(target, "exempt.chat")) {
            // Make sure they are removed.
            this.ignoreService.unignore(src.getUniqueId(), target.getUniqueId());
            sendMessageTo(src, "command.ignore.exempt", target.getName());
            return CommandResult.empty();
        }

        // Ok, we can ignore or unignore them.
        boolean ignore = args.<Boolean>getOne(NucleusParameters.Keys.BOOL)
                .orElseGet(() -> !this.ignoreService.isIgnored(src.getUniqueId(), target.getUniqueId()));

        if (ignore) {
            this.ignoreService.ignore(src.getUniqueId(), target.getUniqueId());
            sendMessageTo(src, "command.ignore.added", target.getName());
        } else {
            this.ignoreService.unignore(src.getUniqueId(), target.getUniqueId());
            sendMessageTo(src, "command.ignore.remove", target.getName());
        }

        return CommandResult.success();
    }
}
