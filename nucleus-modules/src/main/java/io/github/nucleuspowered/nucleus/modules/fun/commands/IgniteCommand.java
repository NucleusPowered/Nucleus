/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun.commands;

import io.github.nucleuspowered.nucleus.modules.fun.FunPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
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

@EssentialsEquivalent("burn")
@NonnullByDefault
@Command(
        aliases = {"ignite", "burn"},
        basePermission = FunPermissions.BASE_IGNITE,
        commandDescriptionKey = "ignite",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = FunPermissions.EXEMPT_COOLDOWN_IGNITE),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = FunPermissions.EXEMPT_WARMUP_IGNITE),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = FunPermissions.EXEMPT_COST_IGNITE)
        },
        associatedPermissions = FunPermissions.OTHERS_IGNITE
)
public class IgniteCommand implements ICommandExecutor<CommandSource> {

    private final String ticks = "ticks";

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier().createOtherUserPermissionElement(true, FunPermissions.OTHERS_IGNITE),
                GenericArguments.onlyOne(GenericArguments.integer(Text.of(this.ticks)))
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player target = context.getPlayerFromArgs();
        int ticksInput = context.requireOne(this.ticks, Integer.class);
        GameMode gm = target.get(Keys.GAME_MODE).orElse(GameModes.SURVIVAL);
        if (gm == GameModes.CREATIVE || gm == GameModes.SPECTATOR) {
            return context.errorResult("command.ignite.gamemode", target.getName());
        }

        if (target.offer(Keys.FIRE_TICKS, ticksInput).isSuccessful()) {
            context.sendMessage("command.ignite.success", target.getName(), String.valueOf(ticksInput));
            return context.successResult();
        } else {
            return context.errorResult("command.ignite.error", target.getName());
        }
    }
}
