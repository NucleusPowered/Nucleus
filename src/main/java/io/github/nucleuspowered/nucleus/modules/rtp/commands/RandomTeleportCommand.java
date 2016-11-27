/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.commands;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.CostCancellableTask;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import io.github.nucleuspowered.nucleus.modules.rtp.config.RTPConfig;
import io.github.nucleuspowered.nucleus.modules.rtp.config.RTPConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.data.property.block.PassableProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.TeleportHelper;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;

import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

@Permissions
@RegisterCommand({"rtp", "randomteleport", "rteleport"})
public class RandomTeleportCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    private Set<BlockType> prohibitedTypes = null;

    private final Random random = new Random();
    @Inject private RTPConfigAdapter rca;

    @Override
    public CommandResult executeCommand(final Player src, CommandContext args) throws Exception {
        // Get the current world.
        World currentWorld = src.getWorld();

        // World border
        WorldBorder wb = currentWorld.getWorldBorder();

        RTPConfig rc = rca.getNodeOrDefault();
        int diameter = Math.min(Math.abs(rc.getRadius() * 2), (int)wb.getDiameter());
        Vector3d centre = wb.getCenter();

        int count = Math.max(rc.getNoOfAttempts(), 1);
        src.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.rtp.searching"));

        Sponge.getScheduler().createTaskBuilder().execute(new RTPTask(plugin, count, diameter, centre, getCost(src, args), src, currentWorld,
                rc.isMustSeeSky(), rc.getMinY(), rc.getMaxY())).submit(plugin);
        return CommandResult.success();
    }

    /*
     * (non-Javadoc)
     *
     * The RTPTask class encapsulates the logic for the /rtp. Because TeleportHelper#getSafeLocation(Location) can be slow, particularly if there is a
     * large area to check, we opt for smaller areas, but to try multiple times. We separate each check by a couple of ticks so that the server
     * still gets to keep ticking, avoiding timeouts and too much lag.
     */
    private class RTPTask extends CostCancellableTask {

        private final int minY;
        private final int maxY;
        private int count;
        private final int maxCount;
        private final int diameter;
        private final Vector3d centre;
        private final TeleportHelper th = Sponge.getGame().getTeleportHelper();
        private final World currentWorld;
        private final boolean onSurface;

        private RTPTask(NucleusPlugin plugin, int count, int diameter, Vector3d centre, double cost, Player src, World currentWorld, boolean onSurface,
                        int minY, int maxY) {
            super(plugin, src, cost);
            this.count = count;
            this.maxCount = count;
            this.diameter = diameter;
            this.centre = centre;
            this.currentWorld = currentWorld;
            this.onSurface = onSurface;
            this.minY = minY;
            this.maxY = maxY;
        }

        @Override
        public void accept(Task task) {
            count--;
            if (!player.isOnline()) {
                onCancel();
                return;
            }

            plugin.getLogger().debug(String.format("RTP of %s, attempt %s of %s", player.getName(), maxCount - count, maxCount));

            // Generate random co-ords.
            int x = RandomTeleportCommand.this.random.nextInt(diameter) - diameter/2;
            int z = RandomTeleportCommand.this.random.nextInt(diameter) - diameter/2;

            // Load the chunk before continuing with /rtp. Sponge issue means we have to load the chunk first.
            currentWorld.loadChunk(new Vector3i(x, 0, z), true);

            int y;
            if (rca.getNodeOrDefault().isMustSeeSky()) {
                // From the x and z co-ordinates, scan down from the top to get the next block.
                Optional<BlockRayHit<World>> blockRayHitOptional = BlockRay
                        .from(new Location<>(currentWorld, new Vector3d(x, Math.min(player.getLocation().getExtent().getBlockMax().getY() - 11, maxY), z)))
                        .to(new Vector3d(x, Math.min(player.getLocation().getExtent().getBlockMax().getY() - 11, minY), z))
                        .stopFilter(BlockRay.onlyAirFilter()).end();
                if (blockRayHitOptional.isPresent()) {
                    y = blockRayHitOptional.get().getBlockY();
                } else {
                    onUnsuccesfulAttempt();
                    return;
                }
            } else {
                // We remove 11 to avoid getting a location too high up for the safe location teleporter to handle.
                y = Math.min(player.getLocation().getExtent().getBlockMax().getY() - 11, random.nextInt(maxY - minY + 1) + minY);
            }

            // To get within the world border, add the centre on.
            final Location<World> test = new Location<>(currentWorld, new Vector3d(x + centre.getX(), y, z + centre.getZ()));
            Optional<Location<World>> oSafeLocation = NucleusTeleportHandler.TELEPORT_HELPER.getSafeLocation(test, 10, 5);

            // getSafeLocation might have put us out of the world border. Best to check.
            // We also check to see that it's not in water or lava, and if enabled, we see if the player would end up on the surface.
            try {
                if (oSafeLocation.isPresent() && isSafe(oSafeLocation.get()) && Util.isLocationInWorldBorder(oSafeLocation.get())) {
                    Location<World> tpTarget = oSafeLocation.get();

                    plugin.getLogger().debug(String.format("RTP of %s, found location %s, %s, %s", player.getName(),
                            String.valueOf(tpTarget.getBlockX()),
                            String.valueOf(tpTarget.getBlockY()),
                            String.valueOf(tpTarget.getBlockZ())));
                    if (player.setLocation(tpTarget)) {
                        player.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.rtp.success",
                                String.valueOf(tpTarget.getBlockX()),
                                String.valueOf(tpTarget.getBlockY()),
                                String.valueOf(tpTarget.getBlockZ())));
                        return;
                    } else {
                        player.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.rtp.cancelled"));
                        onCancel();
                        return;
                    }
                }
            } catch (PositionOutOfBoundsException e) {
                // Swallow - we treat it as a fail.
            }

            onUnsuccesfulAttempt();
        }

        private void onUnsuccesfulAttempt() {
            if (count <= 0) {
                plugin.getLogger().debug(String.format("RTP of %s was unsuccessful", player.getName()));
                player.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.rtp.error"));
                onCancel();
            } else {
                // We're using a scheduler to allow some ticks to go by between attempts to find a
                // safe place.
                Sponge.getScheduler().createTaskBuilder().delayTicks(2).execute(this).submit(plugin);
            }
        }

        private boolean isSafe(Location<World> location) {
            return !location.hasBlock() || !isSolid(location) || !getProhibitedTypes().contains(location.getBlockType());
        }

        private Set<BlockType> getProhibitedTypes() {
            if (prohibitedTypes == null) {
                prohibitedTypes = Sets.newHashSet(
                    BlockTypes.WATER,
                    BlockTypes.LAVA,
                    BlockTypes.FLOWING_WATER,
                    BlockTypes.FLOWING_LAVA
                );
            }

            return prohibitedTypes;
        }

        private boolean isSolid(Location<World> location) {
            Optional<MatterProperty> pp = location.getBlockType().getProperty(MatterProperty.class);
            return pp.isPresent() && pp.get().getValue() != MatterProperty.Matter.SOLID;
        }

        @Override
        public void onCancel() {
            super.onCancel();
            RandomTeleportCommand.this.removeCooldown(player.getUniqueId());
        }

        private class SurfaceCheckPredicate implements Predicate<BlockRayHit<World>> {

            private int count = 0;

            @Override
            public boolean test(BlockRayHit<World> l) {
                BlockType type = l.getLocation().getBlockType();

                // If passable - treat it as seeing the sky.
                if (Boolean.TRUE.equals(type.getProperty(PassableProperty.class).orElse(new PassableProperty(false)).getValue())) {
                    return true;
                }

                // If leaves, treat it as on the surface.
                if (count++ > 1 && type.equals(BlockTypes.LEAVES) || type.equals(BlockTypes.LEAVES2)) {
                    return true;
                }

                // Nope, we're not on the surface
                return false;
            }
        }
    }
}
