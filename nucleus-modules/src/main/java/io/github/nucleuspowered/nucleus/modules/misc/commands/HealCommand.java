/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import io.github.nucleuspowered.nucleus.modules.misc.MiscPermissions;
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
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@EssentialsEquivalent("heal")
@NonnullByDefault
@Command(
        aliases = {"heal"},
        basePermission = MiscPermissions.BASE_HEAL,
        commandDescriptionKey = "heal",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = MiscPermissions.EXEMPT_COOLDOWN_HEAL),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = MiscPermissions.EXEMPT_WARMUP_HEAL),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = MiscPermissions.EXEMPT_COST_HEAL)
        },
        associatedPermissions = MiscPermissions.OTHERS_HEAL
)
public class HealCommand implements ICommandExecutor<CommandSource> { // extends AbstractCommand.SimpleTargetOtherPlayer {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier()
                        .createOnlyOtherUserPermissionElement(true, MiscPermissions.OTHERS_HEAL)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player pl = context.getPlayerFromArgs();
        if (pl.offer(Keys.HEALTH, pl.get(Keys.MAX_HEALTH).get()).isSuccessful()) {
            pl.offer(Keys.FIRE_TICKS, 0);
            context.sendMessageTo(pl, "command.heal.success.self");
            if (!context.is(pl)) {
                context.sendMessage("command.heal.success.other", pl.getName());
            }

            return context.successResult();
        } else {
            return context.errorResult("command.heal.error");
        }
    }
}
