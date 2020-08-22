/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands.gamemode;

import io.github.nucleuspowered.nucleus.modules.admin.AdminPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;

import java.util.HashMap;
import java.util.Map;

abstract class GamemodeBase<T extends CommandSource> implements ICommandExecutor<T> {

    private static final Map<String, String> MODE_MAP = new HashMap<String, String>() {{
        put(GameModes.SURVIVAL.getId(), AdminPermissions.GAMEMODE_MODES_SURVIVAL);
        put(GameModes.CREATIVE.getId(), AdminPermissions.GAMEMODE_MODES_CREATIVE);
        put(GameModes.ADVENTURE.getId(), AdminPermissions.GAMEMODE_MODES_ADVENTURE);
        put(GameModes.SPECTATOR.getId(), AdminPermissions.GAMEMODE_MODES_SPECTATOR);
    }};

    ICommandResult baseCommand(ICommandContext<? extends CommandSource> context, Player user, GameMode gm) throws CommandException {

        if (!context.testPermission(MODE_MAP.computeIfAbsent(
                gm.getId(), key -> {
                    String[] keySplit = key.split(":", 2);
                    String r = keySplit[keySplit.length - 1].toLowerCase();
                    String perm = AdminPermissions.GAMEMODE_MODES_ROOT + "." + r;
                    MODE_MAP.put(key, perm);
                    return perm;
                }
        ))) {
            return context.errorResult("command.gamemode.permission", gm.getTranslation().get());
        }

        DataTransactionResult dtr = user.offer(Keys.GAME_MODE, gm);
        if (dtr.isSuccessful()) {
            if (!context.is(user)) {
                context.sendMessage("command.gamemode.set.other", user.getName(), gm.getName());
            }

            context.sendMessageTo(user, "command.gamemode.set.base", gm.getName());
            return context.successResult();
        }

        return context.errorResult("command.gamemode.error", user.getName());
    }
}
