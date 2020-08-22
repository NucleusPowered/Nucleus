/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun.commands;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.modules.fun.FunPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.PositiveDoubleArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.explosion.Explosion;

import java.util.concurrent.TimeUnit;

@NonnullByDefault
@Command(
        aliases = "rocket",
        basePermission = FunPermissions.BASE_ROCKET,
        commandDescriptionKey = "rocket",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = FunPermissions.EXEMPT_COOLDOWN_ROCKET),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = FunPermissions.EXEMPT_WARMUP_ROCKET),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = FunPermissions.EXEMPT_COST_ROCKET)
        },
        associatedPermissions = FunPermissions.OTHERS_ROCKET
)
public class RocketCommand implements ICommandExecutor<CommandSource> {

    private final String velocity = "velocity";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags()
                        .flag("h", "-hard")
                        .flag("g", "-g")
                        .valueFlag(new PositiveDoubleArgument(Text.of(this.velocity), serviceCollection), "v", "-velocity")
                        .flag("s", "-silent")
                        .flag("e", "-explosion")
                        .buildWith(NucleusParameters.ONE_PLAYER.get(serviceCollection))
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player target = context.getPlayerFromArgs();
        boolean isSelf = context.is(target);
        if (!isSelf && !context.testPermission(FunPermissions.OTHERS_ROCKET)) {
            return context.errorResult("command.rocket.noothers");
        }

        double v = 2;
        if (context.hasAny(this.velocity)) {
            v = context.requireOne(this.velocity, double.class);
        } else if (context.hasAny("g")) {
            v = 0.5;
        } else if (context.hasAny("h")) {
            v = 4;
        }

        if (context.hasAny("e")) {
            Explosion ex = Explosion.builder()
                    .canCauseFire(false)
                    .location(target.getLocation())
                    .shouldBreakBlocks(false)
                    .shouldPlaySmoke(true)
                    .shouldDamageEntities(false)
                    .radius((float) v * 2.0f)
                    .build();
            ex.getWorld().triggerExplosion(ex);
            Sponge.getScheduler().createSyncExecutor(context.getServiceCollection().pluginContainer())
                    .schedule(() ->
                                    ex.getWorld().playSound(SoundTypes.ENTITY_FIREWORK_LAUNCH, target.getLocation().getPosition(), 2),
                            500,
                            TimeUnit.MILLISECONDS);
        }

        Vector3d velocity = new Vector3d(0, v, 0);
        target.offer(Keys.VELOCITY, velocity);
        if (!context.hasAny("s")) {
            context.sendMessageTo(target, "command.rocket.self");
        }

        if (!isSelf) {
            context.sendMessage("command.rocket.other", target.getName());
        }

        return context.successResult();
    }
}
