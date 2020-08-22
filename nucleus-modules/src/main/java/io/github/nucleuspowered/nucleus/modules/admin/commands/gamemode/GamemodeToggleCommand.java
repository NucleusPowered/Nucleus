/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands.gamemode;

import io.github.nucleuspowered.nucleus.modules.admin.AdminPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = {"gmt"},
        parentCommand = GamemodeCommand.class,
        basePermission = {AdminPermissions.BASE_GAMEMODE,
                AdminPermissions.GAMEMODE_MODES_SPECTATOR,
                AdminPermissions.GAMEMODE_MODES_CREATIVE},
        commandDescriptionKey = "gmt",
        modifiers =
                {
                        @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = AdminPermissions.EXEMPT_WARMUP_GAMEMODE,
                                useFrom = GamemodeCommand.class),
                        @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = AdminPermissions.EXEMPT_COOLDOWN_GAMEMODE,
                                useFrom = GamemodeCommand.class),
                        @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = AdminPermissions.EXEMPT_COST_GAMEMODE,
                                useFrom = GamemodeCommand.class)
                }
)
@EssentialsEquivalent("gmt")
public class GamemodeToggleCommand extends GamemodeBase<Player> {

    @Override
    public ICommandResult execute(ICommandContext<? extends Player> src) throws CommandException {
        GameMode mode = src.getIfPlayer().get(Keys.GAME_MODE).orElse(GameModes.SURVIVAL);
        if (mode.equals(GameModes.SURVIVAL) || mode.equals(GameModes.NOT_SET)) {
            mode = GameModes.CREATIVE;
        } else {
            mode = GameModes.SURVIVAL;
        }

        return baseCommand(src, src.getIfPlayer(), mode);
    }
}
