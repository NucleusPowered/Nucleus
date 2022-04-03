/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.commands;

import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnPermissions;
import io.github.nucleuspowered.nucleus.modules.spawn.config.GlobalSpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.helpers.SpawnHelper;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.math.vector.Vector3d;

@Command(
        aliases = "other",
        basePermission = SpawnPermissions.BASE_SPAWN_OTHER,
        commandDescriptionKey = "spawn.other",
        parentCommand = SpawnCommand.class,
        associatedPermissions = {
                SpawnPermissions.SPAWNOTHER_OFFLINE
        }
)
public class SpawnOtherCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private GlobalSpawnConfig gsc = new GlobalSpawnConfig();
    private boolean safeTeleport = true;

    @Override public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.ONE_USER,
                NucleusParameters.ONLINE_WORLD_OPTIONAL
        };
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        final SpawnConfig sc = serviceCollection.configProvider().getModuleConfig(SpawnConfig.class);
        this.gsc = sc.getGlobalSpawn();
        this.safeTeleport = sc.isSafeTeleport();
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final User target = Sponge.server().userManager().loadOrCreate(context.requireOne(NucleusParameters.ONE_USER)).join();
        final ServerWorld world = context.getWorldPropertiesOrFromSelfOptional(NucleusParameters.ONLINE_WORLD_OPTIONAL.key())
            .orElseGet(() -> this.gsc.isOnSpawnCommand() ? this.gsc.getWorld().get() :
                    Sponge.server().worldManager().defaultWorld());

        final Tuple<ServerLocation, Vector3d> worldTransform = SpawnHelper.getSpawn(world, target.player().orElse(null), context);

        if (!target.isOnline()) {
            return this.isOffline(context, target, worldTransform.first(), worldTransform.second());
        }

        // If we don't have a rotation, then use the current rotation
        final Player player = target.player().get();
        final TeleportResult result = context.getServiceCollection()
                .teleportService()
                .teleportPlayerSmart(
                        target.player().get(),
                        worldTransform.first(),
                        worldTransform.second(), true,
                        this.safeTeleport,
                        TeleportScanners.NO_SCAN.get()
                );
        if (result.isSuccessful()) {
            context.sendMessage("command.spawnother.success.source", target.name(), world.key().asString());
            context.sendMessageTo(player, "command.spawnother.success.target", world.key().asString());
            return context.successResult();
        }

        return context.errorResult("command.spawnother.fail", target.name(), world.key().asString());
    }

    private ICommandResult isOffline(final ICommandContext context, final User user, final ServerLocation worldTransform, final Vector3d rotation) throws CommandException {
        if (!context.testPermission(SpawnPermissions.SPAWNOTHER_OFFLINE)) {
            return context.errorResult("command.spawnother.offline.permission");
        }

        user.setLocation(worldTransform.worldKey(), worldTransform.position());
        user.setRotation(rotation);
        context.sendMessage("command.spawnother.offline.sendonlogin", user.name(), worldTransform.worldKey().asString());
        return context.successResult();
    }
}
