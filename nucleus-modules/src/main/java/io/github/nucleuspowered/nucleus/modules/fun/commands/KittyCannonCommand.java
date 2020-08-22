/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun.commands;

import com.flowpowered.math.imaginary.Quaterniond;
import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.modules.fun.FunPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.OcelotType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

@NonnullByDefault
@Command(
        aliases = {"kittycannon", "kc"},
        basePermission = FunPermissions.BASE_KITTYCANNON,
        commandDescriptionKey = "kittycannon",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = FunPermissions.EXEMPT_COOLDOWN_KITTYCANNON),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = FunPermissions.EXEMPT_WARMUP_KITTYCANNON),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = FunPermissions.EXEMPT_COST_KITTYCANNON)
        },
        associatedPermissions = FunPermissions.OTHERS_KITTYCANNON
)
public class KittyCannonCommand implements ICommandExecutor<CommandSource> {

    private final Random random = new Random();
    private final List<OcelotType> ocelotTypes = Lists.newArrayList(Sponge.getRegistry().getAllOf(OcelotType.class));

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.flags()
                .permissionFlag(FunPermissions.KITTYCANNON_DAMAGE, "d", "-damageentities")
                .permissionFlag(FunPermissions.KITTYCANNON_BREAK, "b", "-breakblocks")
                .permissionFlag(FunPermissions.KITTYCANNON_FIRE, "f", "-fire")
                .buildWith(
                    GenericArguments.optional(
                        serviceCollection.commandElementSupplier().createPermissionParameter(
                                NucleusParameters.MANY_PLAYER.get(serviceCollection), FunPermissions.OTHERS_KITTYCANNON, false
                        )))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Collection<Player> playerList = context.getAll(NucleusParameters.Keys.PLAYER, Player.class);
        if (playerList.isEmpty()) {
            playerList = ImmutableList.of(context.getIfPlayer());
        }

        // For each subject, create a kitten, throw it out in the direction of the subject, and make it explode after between 2 and 5 seconds
        for (Player x : playerList) {
            getACat(context, x, context.hasAny("d"), context.hasAny("b"), context.hasAny("f"));
        }
        return context.successResult();
    }

    private void getACat(ICommandContext<? extends CommandSource> context, Player spawnAt, boolean damageEntities, boolean breakBlocks,
            boolean causeFire) throws CommandException {
        // Fire it in the direction that the subject is facing with a speed of 0.5 to 3.5, plus the subject's current velocity.
        Vector3d headRotation = spawnAt.getHeadRotation();
        Quaterniond rot = Quaterniond.fromAxesAnglesDeg(headRotation.getX(), -headRotation.getY(), headRotation.getZ());
        Vector3d velocity = spawnAt.getVelocity().add(rot.rotate(Vector3d.UNIT_Z).mul(5 * this.random.nextDouble() + 1));
        World world = spawnAt.getWorld();
        Entity cat = world.createEntity(EntityTypes.OCELOT, spawnAt.getLocation()
            .getPosition().add(0, 1, 0).add(spawnAt.getTransform().getRotationAsQuaternion().getDirection()));
        cat.offer(Keys.OCELOT_TYPE, this.ocelotTypes.get(this.random.nextInt(this.ocelotTypes.size())));

        Sponge.getScheduler().createTaskBuilder().intervalTicks(5).delayTicks(5)
            .execute(new CatTimer(world.getUniqueId(), cat.getUniqueId(), spawnAt, this.random.nextInt(60) + 20, damageEntities, breakBlocks,
                    causeFire))
            .submit(context.getServiceCollection().pluginContainer());

        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(context.getCommandSource());
            world.spawnEntity(cat);
        }

        cat.offer(Keys.VELOCITY, velocity);
    }

    private static class CatTimer implements Consumer<Task> {

        private final UUID entity;
        private final UUID world;
        private final Player player;
        private final boolean damageEntities;
        private final boolean causeFire;
        private final boolean breakBlocks;
        private int ticksToDestruction;

        private CatTimer(UUID world, UUID entity, Player player, int ticksToDestruction, boolean damageEntities, boolean breakBlocks, boolean causeFire) {
            this.entity = entity;
            this.ticksToDestruction = ticksToDestruction;
            this.world = world;
            this.player = player;
            this.damageEntities = damageEntities;
            this.breakBlocks = breakBlocks;
            this.causeFire = causeFire;
        }

        @Override public void accept(Task task) {
            Optional<World> oWorld = Sponge.getServer().getWorld(this.world);
            if (!oWorld.isPresent()) {
                task.cancel();
                return;
            }

            Optional<Entity> oe = oWorld.get().getEntity(this.entity);
            if (!oe.isPresent()) {
                task.cancel();
                return;
            }

            Entity e = oe.get();
            if (e.isRemoved()) {
                task.cancel();
                return;
            }

            this.ticksToDestruction -= 5;
            if (this.ticksToDestruction <= 0 || e.isOnGround()) {
                // Cat explodes.
                Explosion explosion = Explosion.builder().location(e.getLocation()).canCauseFire(this.causeFire)
                    .shouldDamageEntities(this.damageEntities).shouldPlaySmoke(true).shouldBreakBlocks(this.breakBlocks)
                    .radius(2).build();
                e.remove();
                try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                    frame.pushCause(this.player);
                    oWorld.get().triggerExplosion(explosion);
                }

                task.cancel();
            }
        }
    }
}
