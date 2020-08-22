/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands.gamemode;

import io.github.nucleuspowered.nucleus.modules.admin.AdminPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.ImprovedGameModeArgument;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.NoneThrowOnCompleteArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

@Command(aliases = {"gamemode", "gm"},
        basePermission = AdminPermissions.BASE_GAMEMODE,
        commandDescriptionKey = "gamemode",
        modifiers =
                {
                        @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = AdminPermissions.EXEMPT_WARMUP_GAMEMODE),
                        @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = AdminPermissions.EXEMPT_COOLDOWN_GAMEMODE),
                        @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = AdminPermissions.EXEMPT_COST_GAMEMODE)
                },
        associatedPermissions = AdminPermissions.GAMEMODE_OTHER
)
@NonnullByDefault
@EssentialsEquivalent(value = {"gamemode", "gm"}, isExact = false, notes = "/gm does not toggle between survival and creative, use /gmt for that")
public class GamemodeCommand extends GamemodeBase<CommandSource> {

    private final String gamemodeKey = "gamemode";
    private final String gamemodeself = "gamemode_self";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.firstParsing(
                        GenericArguments.requiringPermission(
                                GenericArguments.seq(
                                    NucleusParameters.ONE_PLAYER.get(serviceCollection),
                                    GenericArguments.onlyOne(new ImprovedGameModeArgument(Text.of(this.gamemodeKey), serviceCollection))
                        ), AdminPermissions.GAMEMODE_OTHER),
                        GenericArguments.onlyOne(new ImprovedGameModeArgument(Text.of(this.gamemodeself), serviceCollection)),
                        NoneThrowOnCompleteArgument.INSTANCE
                )
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player user;
        Optional<GameMode> ogm;
        if (context.hasAny(this.gamemodeself)) {
            user = context.getIfPlayer();
            ogm = context.getOne(this.gamemodeself, GameMode.class);
        } else {
            user = context.getPlayerFromArgs();
            ogm = context.getOne(this.gamemodeKey, GameMode.class);
        }

        if (!ogm.isPresent()) {
            String mode = user.get(Keys.GAME_MODE).orElse(GameModes.SURVIVAL).getName();
            if (context.is(user)) {
                context.sendMessage("command.gamemode.get.base", mode);
            } else {
                context.sendMessage("command.gamemode.get.other", user.getName(), mode);
            }

            return context.successResult();
        }

        GameMode gm = ogm.get();
        return baseCommand(context, user, gm);
    }
}
