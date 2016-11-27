/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.argumentparsers.NicknameArgument;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.Since;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;

import java.util.Map;

@NoWarmup
@NoCooldown
@NoCost
@Permissions
@RegisterCommand({"enderchest", "ec", "echest"})
@Since(minecraftVersion = "1.10.2", spongeApiVersion = "5.0.0", nucleusVersion = "0.13.0")
public class EnderChestCommand extends AbstractCommand<Player> {

    private final String player = "player";
    @Inject
    private UserDataManager udm;

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> mspi = super.permissionSuffixesToRegister();
        mspi.put("others", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.enderchest.others"), SuggestedLevel.ADMIN));
        mspi.put("exempt.target", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.enderchest.exempt.inspect"), SuggestedLevel.ADMIN));
        return mspi;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.optional(
                    GenericArguments.requiringPermission(
                        GenericArguments.onlyOne(new NicknameArgument(Text.of(player), udm, NicknameArgument.UnderlyingType.PLAYER)),
                        permissions.getPermissionWithSuffix("others")
                    ))
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        Player target = args.<Player>getOne(player).orElse(src);

        if (!target.getUniqueId().equals(src.getUniqueId())) {
            if (permissions.testSuffix(target, "exempt.target")) {
                throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.enderchest.targetexempt", target.getName()));
            }
        }

        src.openInventory(target.getEnderChestInventory(), Cause.of(NamedCause.of("plugin", plugin), NamedCause.source(src)));
        return CommandResult.success();
    }
}
