/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.misc.MiscPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.util.DataScanner;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@NonnullByDefault
@Command(
        aliases = "entityinfo",
        basePermission = MiscPermissions.BASE_ENTITYINFO,
        commandDescriptionKey = "entityinfo",
        associatedPermissions = MiscPermissions.ENTITYINFO_EXTENDED
)
public class EntityInfoCommand implements ICommandExecutor<Player> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags()
                        .permissionFlag(MiscPermissions.ENTITYINFO_EXTENDED, "e", "-extended")
                        .buildWith(GenericArguments.none())
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        // Get all the entities in the world.
        Player player = context.getIfPlayer();
        Vector3i playerPos = player.getLocation().getBlockPosition();
        Collection<Entity> entities = player.getWorld().getEntities().stream()
            .filter(x -> x.getLocation().getBlockPosition().distanceSquared(playerPos) < 121) // 11 blocks.
            .collect(Collectors.toList());

        BlockRay<World> bl = BlockRay.from(player).distanceLimit(10).stopFilter(BlockRay.continueAfterFilter(x -> {
            Vector3i pt1 = x.getLocation().getBlockPosition();
            Vector3i pt2 = pt1.add(0, 1, 0);
            return entities.stream()
                .allMatch(e -> {
                    Vector3i current = e.getLocation().getBlockPosition();

                    // We don't want it to stop until one of these are hit.
                    return !(current.equals(pt1) || current.equals(pt2));
                });
        }, 1)).build();
        Optional<BlockRayHit<World>> ob = bl.end();

        if (ob.isPresent()) {
            BlockRayHit<World> brh = ob.get();
            Vector3d location = brh.getLocation().getPosition();
            Vector3d locationOneUp = location.add(0, 1, 0);

            Optional<Entity> entityOptional = entities.stream().filter(e -> {
                Vector3i current = e.getLocation().getBlockPosition();
                return current.equals(location.toInt()) || current.equals(locationOneUp.toInt());
            }).min(Comparator.comparingDouble(x -> x.getLocation().getPosition().distanceSquared(location)));

            if (entityOptional.isPresent()) {
                // Display info about the entity
                Entity entity = entityOptional.get();
                EntityType type = entity.getType();

                List<Text> lt = new ArrayList<>();
                lt.add(context.getMessage("command.entityinfo.id", type.getId(), Util.getTranslatableIfPresent(type)));
                lt.add(context.getMessage("command.entityinfo.uuid", entity.getUniqueId().toString()));

                if (context.hasAny("e") || context.hasAny("extended")) {
                    // For each key, see if the entity supports it. If so, get and print the value.
                    DataScanner.getInstance(context.getServiceCollection().messageProvider())
                            .getKeysForHolder(entity).entrySet().stream().filter(x -> x.getValue() != null).filter(x -> {
                        // Work around a Sponge bug.
                        try {
                            return entity.supports(x.getValue());
                        } catch (Exception e) {
                            return false;
                        }
                    }).forEach(x -> {
                        Key<? extends BaseValue<Object>> k = (Key<? extends BaseValue<Object>>) x.getValue();
                        if (entity.get(k).isPresent()) {
                            DataScanner.getInstance(context.getServiceCollection().messageProvider())
                                    .getText(player, "command.entityinfo.key", x.getKey(), entity.get(k).get()).ifPresent(lt::add);
                        }
                    });
                }

                Sponge.getServiceManager().provideUnchecked(PaginationService.class).builder().contents(lt).padding(Text.of(TextColors.GREEN, "-"))
                    .title(context.getMessage("command.entityinfo.list.header", String.valueOf(brh.getBlockX()),
                        String.valueOf(brh.getBlockY()), String.valueOf(brh.getBlockZ())))
                    .sendTo(player);

                return context.successResult();
            }
        }

        return context.errorResult("command.entityinfo.none");
    }
}
