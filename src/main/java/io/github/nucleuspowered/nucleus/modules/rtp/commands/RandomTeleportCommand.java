/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.commands;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.rtp.RTPKernel;
import io.github.nucleuspowered.nucleus.api.service.NucleusRTPService;
import io.github.nucleuspowered.nucleus.internal.CostCancellableTask;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.command.SetCooldownManually;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import io.github.nucleuspowered.nucleus.modules.rtp.RTPModule;
import io.github.nucleuspowered.nucleus.modules.rtp.config.RTPConfig;
import io.github.nucleuspowered.nucleus.modules.rtp.config.RTPConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.rtp.events.RTPSelectedLocationEvent;
import io.github.nucleuspowered.nucleus.modules.rtp.options.RTPOptions;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import uk.co.drnaylor.quickstart.config.TypedAbstractConfigAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

@SetCooldownManually
@NonnullByDefault
@Permissions(supportsOthers = true)
@RegisterCommand({"rtp", "randomteleport", "rteleport"})
public class RandomTeleportCommand extends AbstractCommand.SimpleTargetOtherPlayer implements Reloadable {

    private RTPConfig rc = new RTPConfig();
    private final Map<Task, UUID> cachedTasks = new WeakHashMap<>();

    private final Timing TIMINGS = Timings.of(Nucleus.getNucleus(), "RTP task");

    @Override protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        return new HashMap<String, PermissionInformation>() {{
            put("worlds", PermissionInformation.getWithTranslation("permission.rtp.worlds", SuggestedLevel.ADMIN));
        }};
    }

    @Override public CommandElement[] additionalArguments() {
        return new CommandElement[] {
            GenericArguments.optionalWeak(
                requirePermissionArg(
                        NucleusParameters.WORLD_PROPERTIES_ENABLED_ONLY, this.permissions.getPermissionWithSuffix("world")
                ))
        };
    }

    @Override
    protected CommandResult executeWithPlayer(CommandSource src, Player player, CommandContext args, boolean self)
            throws Exception {
        synchronized (this.cachedTasks) {
            this.cachedTasks.keySet().removeIf(task -> !Sponge.getScheduler().getTaskById(task.getUniqueId()).isPresent());
            if (this.cachedTasks.containsValue(player.getUniqueId())) {
                throw ReturnMessageException.fromKey("command.rtp.inprogress", player.getName());
            }
        }

        // Get the current world.
        final WorldProperties wp;
        if (this.rc.getDefaultWorld().isPresent()) {
            wp = args.<WorldProperties>getOne(NucleusParameters.Keys.WORLD).orElseGet(() -> this.rc.getDefaultWorld().get());
        } else {
            wp = this.getWorldFromUserOrArgs(src, NucleusParameters.Keys.WORLD, args);
        }

        if (this.rc.isPerWorldPermissions()) {
            String name = wp.getWorldName();
            this.permissions.checkSuffix(src, "worlds." + name.toLowerCase(), () -> ReturnMessageException.fromKey("command.rtp.worldnoperm", name));
        }

        World currentWorld = Sponge.getServer().loadWorld(wp.getUniqueId()).orElse(null);
        if (currentWorld == null) {
            currentWorld = Sponge.getServer().loadWorld(wp).orElseThrow(() -> ReturnMessageException.fromKey("command.rtp.worldnoload", wp.getWorldName()));
        }

        sendMessageTo(src, "command.rtp.searching");

        RTPOptions options = new RTPOptions(this.rc, currentWorld.getName());
        RTPTask rtask = new RTPTask(currentWorld, src, player, this.rc.getNoOfAttempts(),
                options,
                this.rc.getKernel(wp.getWorldName()),
                getCost(src, args));
        Task task = Sponge.getScheduler().createTaskBuilder().execute(rtask).submit(Nucleus.getNucleus());
        this.cachedTasks.put(task, player.getUniqueId());

        return CommandResult.success();
    }

    @Override public void onReload() {
        this.rc = Nucleus.getNucleus().getConfigAdapter(RTPModule.ID, RTPConfigAdapter.class)
                .map(TypedAbstractConfigAdapter::getNodeOrDefault).orElseGet(RTPConfig::new);
    }

    /*
     * (non-Javadoc)
     *
     * The RTPTask class encapsulates the logic for the /rtp. Because TeleportHelper#getSafeLocation(Location) can be slow, particularly if there is a
     * large area to check, we opt for smaller areas, but to try multiple times. We separate each check by a couple of ticks so that the server
     * still gets to keep ticking, avoiding timeouts and too much lag.
     */
    private class RTPTask extends CostCancellableTask {

        private final Cause cause;
        private final World targetWorld;
        private final CommandSource source;
        private final Player target;
        private final boolean isSelf;
        private int count;
        private final int maxCount;
        private final NucleusRTPService.RTPOptions options;
        private final RTPKernel kernel;

        private RTPTask(World target, CommandSource source, Player target1, int maxCount, NucleusRTPService.RTPOptions options,
                RTPKernel kernel, double cost) {
            super(Nucleus.getNucleus(), source, cost);
            this.cause = Sponge.getCauseStackManager().getCurrentCause();
            this.targetWorld = target;
            this.source = source;
            this.target = target1;
            this.isSelf = source instanceof Player && ((Player) source).getUniqueId().equals(target1.getUniqueId());
            this.maxCount = maxCount;
            this.count = maxCount;
            this.options = options;
            this.kernel = kernel;
        }

        @Override public void accept(Task task) {
            this.count--;
            if (!this.target.isOnline()) {
                onCancel();
                return;
            }

            try (Timing dummy = TIMINGS.startTiming()) {
                Nucleus.getNucleus()
                        .getLogger()
                        .debug(String.format("RTP of %s, attempt %s of %s", this.target.getName(), this.maxCount - this.count, this.maxCount));

                int counter = 0;
                while (++counter <= 10) {
                    try {
                        Optional<Location<World>> optionalLocation =
                                this.kernel.getLocation(this.target.getLocation(), this.targetWorld, this.options);
                        if (optionalLocation.isPresent()) {
                            Location<World> targetLocation = optionalLocation.get();
                            if (Sponge.getEventManager().post(new RTPSelectedLocationEvent(
                                    targetLocation,
                                    this.target,
                                    this.cause
                            ))) {
                                continue;
                            }

                            Nucleus.getNucleus().getLogger().debug(String.format("RTP of %s, found location %s, %s, %s", this.target.getName(),
                                    String.valueOf(targetLocation.getBlockX()),
                                    String.valueOf(targetLocation.getBlockY()),
                                    String.valueOf(targetLocation.getBlockZ())));
                            if (SafeTeleportService.setLocation(this.target, targetLocation)) {
                                if (!this.isSelf) {
                                    sendMessageTo(this.target, "command.rtp.other");
                                    sendMessageTo(this.source, "command.rtp.successother",
                                            this.target.getName(),
                                            targetLocation.getBlockX(),
                                            targetLocation.getBlockY(),
                                            targetLocation.getBlockZ());
                                }

                                sendMessageTo(this.target, "command.rtp.success",
                                        targetLocation.getBlockX(),
                                        targetLocation.getBlockY(),
                                        targetLocation.getBlockZ());
                                if (this.isSelf) {
                                    RandomTeleportCommand.this.setCooldown(this.target);
                                    synchronized (RandomTeleportCommand.this.cachedTasks) {
                                        RandomTeleportCommand.this.cachedTasks.remove(task);
                                    }
                                }
                                return;
                            } else {
                                sendMessageTo(this.source, "command.rtp.cancelled");
                                onCancel();
                                return;
                            }
                        }
                    } catch (PositionOutOfBoundsException ignore) {
                        // treat as fail.
                    }
                }

                onUnsuccesfulAttempt(task);
            }
        }

        private void onUnsuccesfulAttempt(Task task) {
            synchronized (RandomTeleportCommand.this.cachedTasks) {
                if (this.count <= 0) {
                    Nucleus.getNucleus().getLogger().debug(String.format("RTP of %s was unsuccessful", this.subject.getName()));
                    sendMessageTo(this.subject, "command.rtp.error");
                    onCancel();
                } else {
                    // We're using a scheduler to allow some ticks to go by between attempts to find a
                    // safe place.
                    RandomTeleportCommand.this.cachedTasks.put(
                            Sponge.getScheduler().createTaskBuilder().delayTicks(2).execute(this).submit(Nucleus.getNucleus()),
                            target.getUniqueId()
                    );
                }

                RandomTeleportCommand.this.cachedTasks.remove(task);
            }
        }
    }

}
