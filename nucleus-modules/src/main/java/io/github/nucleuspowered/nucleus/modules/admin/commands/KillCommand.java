/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands;

import io.github.nucleuspowered.nucleus.modules.admin.AdminPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.util.TypeTokens;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collection;

@Command(aliases = "kill", basePermission = AdminPermissions.BASE_KILL, commandDescriptionKey = "kill",
    modifiers = {
            @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = AdminPermissions.EXEMPT_WARMUP_KILL),
            @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = AdminPermissions.EXEMPT_COOLDOWN_KILL),
            @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = AdminPermissions.EXEMPT_COST_KILL)
    })
@EssentialsEquivalent(value = { "kill", "remove", "butcher", "killall", "mobkill" },
        isExact = false, notes = "Nucleus supports killing entities using the Minecraft selectors.")
@NonnullByDefault
public class KillCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.MANY_ENTITY.get(serviceCollection)
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Collection<Entity> entities = context.getAll(NucleusParameters.Keys.SUBJECT, TypeTokens.ENTITY);

        int entityKillCount = 0;
        int playerKillCount = 0;
        for (Entity x : entities) {
            DataTransactionResult dtr = x.offer(Keys.HEALTH, 0d);
            if (!dtr.isSuccessful() && !(x instanceof Living)) {
                x.remove();
            }
            entityKillCount++;

            if (x instanceof Player) {
                Player pl = (Player) x;
                playerKillCount++;
                context.sendMessage("command.kill.killed", pl.getName());
                context.sendMessageTo(pl, "command.kill.killedby", context.getCommandSource().getName());
            }
        }

        if (entityKillCount > playerKillCount) {
            context.sendMessage("command.kill.overall", entityKillCount, playerKillCount);
        }

        return context.successResult();
    }
}
