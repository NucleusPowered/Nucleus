/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.commands;

import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnPermissions;
import io.github.nucleuspowered.nucleus.modules.spawn.config.GlobalSpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.events.SendToSpawnEvent;
import io.github.nucleuspowered.nucleus.modules.spawn.helpers.SpawnHelper;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

@NonnullByDefault
@Command(
        aliases = "other",
        basePermission = SpawnPermissions.BASE_SPAWN_OTHER,
        commandDescriptionKey = "spawn.other",
        parentCommand = SpawnCommand.class,
        associatedPermissions = {
                SpawnPermissions.SPAWNOTHER_OFFLINE
        }
)
public class SpawnOtherCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private GlobalSpawnConfig gsc = new GlobalSpawnConfig();
    private boolean safeTeleport = true;

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.ONE_USER.get(serviceCollection),
                NucleusParameters.OPTIONAL_WORLD_PROPERTIES_ENABLED_ONLY.get(serviceCollection)
        };
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        SpawnConfig sc = serviceCollection.moduleDataProvider().getModuleConfig(SpawnConfig.class);
        this.gsc = sc.getGlobalSpawn();
        this.safeTeleport = sc.isSafeTeleport();
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User target = context.requireOne(NucleusParameters.Keys.USER, User.class);
        WorldProperties world = context.getWorldPropertiesOrFromSelf(NucleusParameters.Keys.WORLD)
            .orElseGet(() -> gsc.isOnSpawnCommand() ? gsc.getWorld().get() : Sponge.getServer().getDefaultWorld().get());

        Transform<World> worldTransform = SpawnHelper.getSpawn(world, target.getPlayer().orElse(null), context);

        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContexts.SPAWN_EVENT_TYPE, SendToSpawnEvent.Type.COMMAND);
            frame.pushCause(context.getCommandSource());
            SendToSpawnEvent event = new SendToSpawnEvent(worldTransform, target, frame.getCurrentCause());
            if (Sponge.getEventManager().post(event)) {
                if (event.getCancelReason().isPresent()) {
                    return context.errorResult("command.spawnother.other.failed.reason", target.getName(), event.getCancelReason().get());
                } else {
                    return context.errorResult("command.spawnother.other.failed.noreason", target.getName());
                }
            }

            if (!target.isOnline()) {
                return isOffline(context, target, event.getTransformTo());
            }

            // If we don't have a rotation, then use the current rotation
            Player player = target.getPlayer().get();
            TeleportResult result = context.getServiceCollection()
                    .teleportService()
                    .teleportPlayerSmart(
                            player,
                            event.getTransformTo(),
                            true,
                            this.safeTeleport,
                            TeleportScanners.NO_SCAN.get()
                    );
            if (result.isSuccessful()) {
                context.sendMessage("command.spawnother.success.source", target.getName(), world.getWorldName());
                context.sendMessageTo(player, "command.spawnother.success.target", world.getWorldName());
                return context.successResult();
            }

            return context.errorResult("command.spawnother.fail", target.getName(), world.getWorldName());
        }
    }

    private ICommandResult isOffline(ICommandContext<? extends CommandSource> context, User user, Transform<World> worldTransform) throws CommandException {
        if (!context.testPermission(SpawnPermissions.SPAWNOTHER_OFFLINE)) {
            return context.errorResult("command.spawnother.offline.permission");
        }

        user.setLocation(worldTransform.getPosition(), worldTransform.getExtent().getUniqueId());
        context.sendMessage("command.spawnother.offline.sendonlogin", user.getName(), worldTransform.getExtent().getName());
        return context.successResult();
    }
}
