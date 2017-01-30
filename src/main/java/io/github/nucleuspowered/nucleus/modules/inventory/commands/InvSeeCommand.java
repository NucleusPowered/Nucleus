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
import io.github.nucleuspowered.nucleus.modules.inventory.inventory.InvSeeInventoryFactory;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.util.Map;

@NoWarmup
@NoCooldown
@NoCost
@Permissions
@RegisterCommand("invsee")
@Since(minecraftVersion = "1.10.2", spongeApiVersion = "5.0.0", nucleusVersion = "0.13.0")
public class InvSeeCommand extends AbstractCommand<Player> {

    private final String player = "player";
    @Inject
    private UserDataManager udm;

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> mspi = super.permissionSuffixesToRegister();
        mspi.put("exempt.target", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.invsee.exempt.inspect"), SuggestedLevel.ADMIN));
        mspi.put("exempt.interact", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.invsee.exempt.interact"), SuggestedLevel.ADMIN));
        mspi.put("offline", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.invsee.offline"), SuggestedLevel.ADMIN));
        return mspi;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.onlyOne(new NicknameArgument(Text.of(player), udm, NicknameArgument.UnderlyingType.USER))
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        User target = args.<User>getOne(player).get();

        if (!target.isOnline() && !permissions.testSuffix(src, "offline")) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.invsee.nooffline"));
        }

        if (target.getUniqueId().equals(src.getUniqueId())) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.invsee.self"));
        }

        if (permissions.testSuffix(target, "exempt.target")) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.invsee.targetexempt", target.getName()));
        }

        InvSeeInventoryFactory.viewInventory(plugin, src, target, permissions.testSuffix(target, "exempt.interact"));
        return CommandResult.success();
    }
}
