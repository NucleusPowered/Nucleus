/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.commands;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.NameUtil;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Map;
import java.util.Optional;

@Permissions
@NoCooldown
@NoCost
@NoWarmup
@RegisterCommand({"vanish", "v"})
public class VanishCommand extends CommandBase<CommandSource> {

    private final String b = "toggle";
    private final String playerKey = "player";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.optionalWeak(GenericArguments.requiringPermission(
                    GenericArguments.onlyOne(GenericArguments.player(Text.of(playerKey))),
                    permissions.getPermissionWithSuffix("other"))),
            GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.bool(Text.of(b))))
        };
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> mspi = Maps.newHashMap();
        mspi.put("other", new PermissionInformation(Util.getMessageWithFormat("permission.vanish.other"), SuggestedLevel.ADMIN));
        return mspi;
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Optional<Player> opl = this.getUser(Player.class, src, playerKey, args);
        if (!opl.isPresent()) {
            return CommandResult.empty();
        }

        Player playerToVanish = opl.get();

        // If we don't specify whether to vanish, toggle
        boolean toVanish = args.<Boolean>getOne(b).orElse(!playerToVanish.get(Keys.INVISIBLE).orElse(false));

        DataTransactionResult dtr = playerToVanish.offer(Keys.VANISH, toVanish);
        playerToVanish.offer(Keys.VANISH_PREVENTS_TARGETING, toVanish);
        playerToVanish.offer(Keys.VANISH_IGNORES_COLLISION, toVanish);
        playerToVanish.offer(Keys.IS_SILENT, toVanish);

        if (dtr.isSuccessful()) {
            src.sendMessage(Util.getTextMessageWithFormat("command.vanish.success",
                    toVanish ? Util.getMessageWithFormat("command.vanish.vanished") : Util.getMessageWithFormat("command.vanish.visible")));

            if (!(src instanceof Player) || !(((Player)src).getUniqueId().equals(playerToVanish.getUniqueId()))) {
                src.sendMessage(Util.getTextMessageWithFormat("command.vanish.successplayer",
                        TextSerializers.FORMATTING_CODE.serialize(NameUtil.getName(playerToVanish)),
                        toVanish ? Util.getMessageWithFormat("command.vanish.vanished") : Util.getMessageWithFormat("command.vanish.visible")));
            }

            return CommandResult.success();
        }

        src.sendMessage(Util.getTextMessageWithFormat("command.vanish.fail"));
        return CommandResult.empty();
    }
}
