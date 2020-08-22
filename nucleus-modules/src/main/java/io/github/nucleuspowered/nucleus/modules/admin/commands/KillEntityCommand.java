/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Sets;
import io.github.nucleuspowered.nucleus.modules.admin.AdminPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.util.TypeTokens;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.monster.Monster;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Command(aliases = "kill", basePermission = AdminPermissions.BASE_KILL, commandDescriptionKey = "kill",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = AdminPermissions.EXEMPT_WARMUP_KILLENTITY),
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = AdminPermissions.EXEMPT_COOLDOWN_KILLENTITY),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = AdminPermissions.EXEMPT_COST_KILLENTITY)
        })
@NonnullByDefault
public class KillEntityCommand implements ICommandExecutor<CommandSource> {

    private static final String radius = "radius";
    private static final String world = "world";
    private static final String type = "type";

    private static final Predicate<Entity> armourStand = e -> e.getType().equals(EntityTypes.ARMOR_STAND);
    private static final Predicate<Entity> hostile = e -> e instanceof Monster;
    private static final Predicate<Entity> passive = e -> e instanceof Living && !(e instanceof Player || e instanceof Monster);

    private final Map<String, ?> map = new HashMap<String, Predicate<Entity>>() {{
        put("armorstand", armourStand);
        put("armourstand", armourStand);
        put("monsters", hostile);
        put("hostile", hostile);
        put("passive", passive);
        put("animal", passive);
        put("item", e -> e instanceof Item);
        put("player", e -> e instanceof Player);
    }};

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags()
                        .setAnchorFlags(true)
                        .valueFlag(GenericArguments.integer(Text.of(radius)), "r")
                        .valueFlag(NucleusParameters.WORLD_PROPERTIES_LOADED_ONLY.get(serviceCollection), "w")
                        .buildWith(GenericArguments.allOf(GenericArguments.choices(Text.of(type), this.map)))
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        CommandSource src = context.getCommandSource();
        if (!(src instanceof Locatable) && context.hasAny(radius)) {
            // We can't do that.
            return context.errorResult("command.killentity.commandsourceradius");
        }

        if (context.hasAny(radius) && context.hasAny(world)) {
            // Can't do that, either.
            return context.errorResult("command.killentity.radiusworld");
        }

        Set<Entity> currentEntities;
        if (context.hasAny(radius)) {
            Locatable l = ((Locatable) src);
            Vector3d locationTest = l.getLocation().getPosition();
            int r = context.requireOne(radius, int.class);
            currentEntities = Sets.newHashSet(l.getWorld().getEntities(entity -> entity.getTransform().getPosition().distance(locationTest) <= r));
        } else {
            WorldProperties worldProperties;
            if (context.hasAny(world)) {
                worldProperties = context.requireOne(world, WorldProperties.class);
            } else {
                worldProperties = ((Locatable) src).getWorld().getProperties();
            }
            currentEntities = Sets.newHashSet(Sponge.getServer().getWorld(worldProperties.getUniqueId()).get().getEntities());
        }

        Predicate<Entity> entityPredicate = context.getAll(type, TypeTokens.PREDICATE_ENTITY).stream().reduce(Predicate::or)
                .orElseThrow(() -> context.createException("command.killentity.noselection"));
        Set<Entity> toKill = currentEntities.stream().filter(entityPredicate).collect(Collectors.toSet());
        if (toKill.isEmpty()) {
            return context.errorResult("command.killentity.nothing");
        }

        int killCount = toKill.size();
        toKill.forEach(x -> {
            x.offer(Keys.HEALTH, 0d);
            x.remove();
        });

        context.sendMessage("command.killentity.success", String.valueOf(killCount));
        return context.successResult();
    }
}
