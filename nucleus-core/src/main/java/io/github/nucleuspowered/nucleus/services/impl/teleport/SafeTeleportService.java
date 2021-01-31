/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.teleport;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.api.teleport.data.NucleusTeleportHelperFilters;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanner;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.core.config.SafeTeleportConfig;
import io.github.nucleuspowered.nucleus.modules.teleport.events.AboutToTeleportEvent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.INucleusTeleportService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.TeleportHelper;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;
import uk.co.drnaylor.quickstart.config.TypedAbstractConfigAdapter;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SafeTeleportService implements INucleusTeleportService, IReloadableService.Reloadable {

    private static final BorderDisableSession DUMMY = new BorderDisableSession() {};
    private SafeTeleportConfig config = new SafeTeleportConfig();

    @Inject
    public SafeTeleportService(PluginContainer pluginContainer, IReloadableService reloadableService) {
        Sponge.getServiceManager().setProvider(pluginContainer, INucleusTeleportService.class, this);
        reloadableService.registerReloadable(this);
    }

    @Override public boolean setLocation(Player player, Location<World> location) {
        if (player.setLocation(location)) {
            player.setSpectatorTarget(null);
            return true;
        }

        return false;
    }

    @Override public TeleportResult teleportPlayerSmart(Player player,
            Transform<World> transform,
            boolean centreBlock,
            boolean safe,
            TeleportScanner scanner) {
        return teleportPlayerSmart(player, transform.getLocation(), transform.getRotation(), centreBlock, safe, scanner);
    }

    @Override public TeleportResult teleportPlayerSmart(Player player,
            Location<World> location,
            boolean centreBlock,
            boolean safe,
            TeleportScanner scanner) {
        return teleportPlayer(player,
                location,
                player.getRotation(),
                centreBlock,
                scanner,
                getAppropriateFilter(player, safe));
    }

    @Override public TeleportResult teleportPlayerSmart(Player player,
            Location<World> location,
            Vector3d rotation,
            boolean centreBlock,
            boolean safe,
            TeleportScanner scanner) {
        return teleportPlayer(player,
                location,
                rotation,
                centreBlock,
                scanner,
                getAppropriateFilter(player, safe));
    }

    @Override
    public TeleportResult teleportPlayer(Player player,
            Location<World> location,
            boolean centreBlock,
            TeleportScanner scanner,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters) {
        return teleportPlayer(
                player,
                location,
                player.getRotation(),
                centreBlock,
                scanner,
                filter,
                filters
        );
    }

    @Override
    public TeleportResult teleportPlayer(Player player,
            Location<World> location,
            Vector3d rotation,
            boolean centreBlock,
            TeleportScanner scanner,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters) {

        Optional<Transform<World>> optionalWorldTransform = getSafeTransform(
                location,
                rotation,
                scanner,
                filter,
                filters
        );

        Cause cause = Sponge.getCauseStackManager().getCurrentCause();
        if (optionalWorldTransform.isPresent()) {
            Transform<World> targetLocation = optionalWorldTransform.get();
            AboutToTeleportEvent event = new AboutToTeleportEvent(
                    cause,
                    targetLocation,
                    player
            );

            if (Sponge.getEventManager().post(event)) {
                event.getCancelMessage().ifPresent(x -> {
                    Object o = cause.root();
                    if (o instanceof MessageReceiver) {
                        ((MessageReceiver) o).sendMessage(x);
                    }
                });
                return TeleportResult.FAIL_CANCELLED;
            }

            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                frame.addContext(EventContexts.BYPASS_JAILING_RESTRICTION, true);
                Optional<Entity> oe = player.getVehicle();
                if (oe.isPresent()) {
                    player.setVehicle(null);
                }

                // Do it, tell the routine if it worked.
                if (centreBlock) {
                    targetLocation = new Transform<>(
                            targetLocation.getExtent(),
                            targetLocation.getLocation().getBlockPosition().toDouble().add(0.5, 0.5, 0.5),
                            targetLocation.getRotation());
                }

                if (player.setTransform(targetLocation)) {
                    player.setSpectatorTarget(null);
                    return TeleportResult.SUCCESS;
                }

                oe.ifPresent(player::setVehicle);
            }
        }

        return TeleportResult.FAIL_NO_LOCATION;
    }

    @Override
    public Optional<Location<World>> getSafeLocation(
            Location<World> location,
            TeleportScanner scanner,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters) {
        return scanner.scanFrom(
                location.getExtent(),
                location.getBlockPosition(),
                this.config.getHeight(),
                this.config.getWidth(),
                TeleportHelper.DEFAULT_FLOOR_CHECK_DISTANCE,
                filter,
                filters
        );
    }

    @Override
    public Optional<Transform<World>> getSafeTransform(
            Location<World> location,
            Vector3d rotation,
            TeleportScanner scanner,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters) {
        return getSafeLocation(location, scanner, filter, filters)
                .map(x -> new Transform<>(location.getExtent(), location.getPosition(), rotation));
    }

    @Override public TeleportHelperFilter getAppropriateFilter(Player src, boolean safeTeleport) {
        if (safeTeleport && !src.get(Keys.GAME_MODE).filter(x -> x == GameModes.SPECTATOR).isPresent()) {
            if (src.get(Keys.IS_FLYING).orElse(false)) {
                return TeleportHelperFilters.FLYING;
            } else {
                return TeleportHelperFilters.DEFAULT;
            }
        } else {
            return NucleusTeleportHelperFilters.NO_CHECK.get();
        }
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.config = serviceCollection.moduleDataProvider().getModuleConfig(CoreConfig.class).getSafeTeleportConfig();
    }

    @Override public BorderDisableSession temporarilyDisableBorder(boolean reset, World world) {
        if (reset) {
            WorldBorder border = world.getWorldBorder();
            return new WorldBorderReset(border);
        }

        return DUMMY;
    }

    static class WorldBorderReset implements BorderDisableSession {

        private final double x;
        private final double z;
        private final double diameter;
        private final WorldBorder border;

        WorldBorderReset(WorldBorder border) {
            this.border = border;
            this.x = border.getCenter().getX();
            this.z = border.getCenter().getZ();
            this.diameter = border.getDiameter();
        }

        @Override
        public void close() {
            this.border.setCenter(this.x, this.z);
            this.border.setDiameter(this.diameter);
        }
    }
}
