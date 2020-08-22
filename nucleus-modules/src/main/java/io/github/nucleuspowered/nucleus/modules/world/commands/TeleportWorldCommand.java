/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnModule;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

@EssentialsEquivalent(value = "world", notes = "The world command in Essentials was just a warp command.")
@Command(
        aliases = {"teleport", "tp"},
        basePermission = WorldPermissions.BASE_WORLD_TELEPORT,
        commandDescriptionKey = "world.teleport",
        parentCommand = WorldCommand.class,
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = WorldPermissions.EXEMPT_COOLDOWN_WORLD_TELEPORT),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = WorldPermissions.EXEMPT_WARMUP_WORLD_TELEPORT),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = WorldPermissions.EXEMPT_COST_WORLD_TELEPORT)
        },
        associatedPermissions = {
                WorldPermissions.WORLD_TELEPORT_OTHER,
                WorldPermissions.WORLDS_ACCESS_PERMISSION_PREFIX
        }
)
public class TeleportWorldCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            NucleusParameters.WORLD_PROPERTIES_ENABLED_ONLY.get(serviceCollection),
            serviceCollection.commandElementSupplier()
                .createOnlyOtherUserPermissionElement(true, WorldPermissions.WORLD_TELEPORT_OTHER)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player player = context.getPlayerFromArgs(NucleusParameters.Keys.PLAYER, "command.world.player");
        WorldProperties worldProperties = context.requireOne(NucleusParameters.Keys.WORLD, WorldProperties.class);
        if (!worldProperties.isEnabled()) {
            return context.errorResult("command.world.teleport.notenabled", worldProperties.getWorldName());
        }

        World world = Sponge.getServer().loadWorld(worldProperties.getUniqueId())
            .orElseThrow(() -> context.createException("command.world.teleport.failed", worldProperties.getWorldName()));

        Vector3d pos = worldProperties.getSpawnPosition().toDouble();
        if (!player.transferToWorld(world, pos)) {
            return context.errorResult("command.world.teleport.failed", worldProperties.getWorldName());
        }

        // Rotate.
        if (context.getServiceCollection().moduleDataProvider().isLoaded(SpawnModule.ID)) {
            context.getServiceCollection()
                    .storageManager()
                    .getWorldService()
                    .getOnThread(worldProperties.getUniqueId())
                    .flatMap(x -> x.get(SpawnKeys.WORLD_SPAWN_ROTATION))
                    .ifPresent(y -> new Transform<>(world, pos, y));
        }
        if (context.is(player)) {
            context.sendMessage("command.world.teleport.success", worldProperties.getWorldName());
        } else {
            context.sendMessage("command.world.teleport.successplayer",
                    context.getServiceCollection().playerDisplayNameService().getDisplayName(player),
                    worldProperties.getWorldName());
            context.sendMessageTo(player, "command.world.teleport.success", worldProperties.getWorldName());
        }

        return context.successResult();
    }
}
