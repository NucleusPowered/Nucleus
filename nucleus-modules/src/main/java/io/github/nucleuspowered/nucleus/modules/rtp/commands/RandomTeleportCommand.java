/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.commands;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import io.github.nucleuspowered.nucleus.api.module.rtp.NucleusRTPService;
import io.github.nucleuspowered.nucleus.api.module.rtp.kernel.RTPKernel;
import io.github.nucleuspowered.nucleus.modules.rtp.RTPPermissions;
import io.github.nucleuspowered.nucleus.modules.rtp.config.RTPConfig;
import io.github.nucleuspowered.nucleus.modules.rtp.events.RTPSelectedLocationEvent;
import io.github.nucleuspowered.nucleus.modules.rtp.options.RTPOptions;
import io.github.nucleuspowered.nucleus.modules.rtp.services.RTPService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.scaffold.task.CostCancellableTask;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.inject.Inject;

@NonnullByDefault
@Command(
        aliases = {"rtp", "randomteleport", "rteleport"},
        basePermission = RTPPermissions.BASE_RTP,
        commandDescriptionKey = "rtp",
        modifiers = {
                @CommandModifier(
                        value = CommandModifiers.HAS_COOLDOWN,
                        exemptPermission = RTPPermissions.EXEMPT_COOLDOWN_RTP,
                        onCompletion = false
                ),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = RTPPermissions.EXEMPT_WARMUP_RTP),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = RTPPermissions.EXEMPT_COST_RTP)
        },
        associatedPermissions = {
                RTPPermissions.OTHERS_RTP,
                RTPPermissions.RTP_WORLDS
        }
)
public class RandomTeleportCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private RTPConfig rc = new RTPConfig();
    private final Map<Task, UUID> cachedTasks = new WeakHashMap<>();

    private final Timing TIMINGS;

    @Inject
    public RandomTeleportCommand(INucleusServiceCollection serviceCollection) {
        TIMINGS = Timings.of(serviceCollection.pluginContainer(), "RTP task");;
    }

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier().createOtherUserPermissionElement(true, RTPPermissions.OTHERS_RTP),
                GenericArguments.optionalWeak(NucleusParameters.WORLD_PROPERTIES_ENABLED_ONLY.get(serviceCollection))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player player = context.getPlayerFromArgs();
        synchronized (this.cachedTasks) {
            this.cachedTasks.keySet().removeIf(task -> !Sponge.getScheduler().getTaskById(task.getUniqueId()).isPresent());
            if (this.cachedTasks.containsValue(player.getUniqueId())) {
                return context.errorResult("command.rtp.inprogress", player.getName());
            }
        }

        // Get the current world.
        final WorldProperties wp;
        if (this.rc.getDefaultWorld().isPresent()) {
            wp = context.getOne(NucleusParameters.Keys.WORLD, WorldProperties.class).orElseGet(() -> this.rc.getDefaultWorld().get());
        } else {
            wp = context.getWorldPropertiesOrFromSelf(NucleusParameters.Keys.WORLD).get();
        }

        if (this.rc.isPerWorldPermissions()) {
            String name = wp.getWorldName();
            if (!context.testPermission(RTPPermissions.RTP_WORLDS + "." + name.toLowerCase())) {
                return context.errorResult("command.rtp.worldnoperm", name);
            }
        }

        World currentWorld = Sponge.getServer().loadWorld(wp.getUniqueId()).orElse(null);
        if (currentWorld == null) {
            currentWorld = Sponge.getServer().loadWorld(wp).orElseThrow(() -> context.createException("command.rtp.worldnoload", wp.getWorldName()));
        }

        context.sendMessage("command.rtp.searching");

        RTPOptions options = new RTPOptions(this.rc, currentWorld.getName());
        RTPTask rtask = new RTPTask(
                context.getServiceCollection().pluginContainer(),
                currentWorld,
                context,
                player,
                this.rc.getNoOfAttempts(),
                options,
                context.getServiceCollection().getServiceUnchecked(RTPService.class).getKernel(wp),
                context.is(player) ? context.getCost() : 0);
        Task task = Sponge.getScheduler().createTaskBuilder().execute(rtask).submit(context.getServiceCollection().pluginContainer());
        this.cachedTasks.put(task, player.getUniqueId());

        return context.successResult();
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.rc = serviceCollection.moduleDataProvider().getModuleConfig(RTPConfig.class);
    }

    /*
     * (non-Javadoc)
     *
     * The RTPTask class encapsulates the logic for the /rtp. Because TeleportHelper#getSafeLocation(Location) can be slow, particularly if there is a
     * large area to check, we opt for smaller areas, but to try multiple times. We separate each check by a couple of ticks so that the server
     * still gets to keep ticking, avoiding timeouts and too much lag.
     */
    private class RTPTask extends CostCancellableTask {

        private final PluginContainer pluginContainer;
        private final Cause cause;
        private final World targetWorld;
        private final ICommandContext<? extends CommandSource> source;
        private final Player target;
        private final boolean isSelf;
        private final Logger logger;
        private int count;
        private final int maxCount;
        private final NucleusRTPService.RTPOptions options;
        private final RTPKernel kernel;

        private RTPTask(
                PluginContainer pluginContainer,
                World target,
                ICommandContext<? extends CommandSource> source,
                Player target1,
                int maxCount,
                NucleusRTPService.RTPOptions options,
                RTPKernel kernel,
                double cost) {
            super(source.getServiceCollection(), target1, cost);
            this.logger = source.getServiceCollection().logger();
            this.pluginContainer = pluginContainer;
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
                this.logger.debug(String.format("RTP of %s, attempt %s of %s", this.target.getName(), this.maxCount - this.count, this.maxCount));

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

                            this.source.getServiceCollection().logger().debug(String.format("RTP of %s, found location %s, %s, %s",
                                    this.target.getName(),
                                    String.valueOf(targetLocation.getBlockX()),
                                    String.valueOf(targetLocation.getBlockY()),
                                    String.valueOf(targetLocation.getBlockZ())));
                            if (this.source.getServiceCollection().teleportService().setLocation(this.target, targetLocation)) {
                                if (!this.isSelf) {
                                    this.source.sendMessageTo(this.target, "command.rtp.other");
                                    this.source.sendMessage("command.rtp.successother",
                                            this.target.getName(),
                                            targetLocation.getBlockX(),
                                            targetLocation.getBlockY(),
                                            targetLocation.getBlockZ());
                                }

                                this.source.sendMessageTo(this.target, "command.rtp.success",
                                        targetLocation.getBlockX(),
                                        targetLocation.getBlockY(),
                                        targetLocation.getBlockZ());
                                if (this.isSelf) {
                                    this.source.getServiceCollection()
                                            .cooldownService()
                                            .setCooldown(
                                                    this.source.getCommandKey(),
                                                    this.target,
                                                    Duration.ofSeconds(this.source.getServiceCollection()
                                                            .commandMetadataService()
                                                            .getControl(RandomTeleportCommand.class)
                                                            .orElseThrow(IllegalStateException::new)
                                                            .getCooldown(this.target))
                                            );
                                    synchronized (RandomTeleportCommand.this.cachedTasks) {
                                        RandomTeleportCommand.this.cachedTasks.remove(task);
                                    }
                                }
                                return;
                            } else {
                                this.source.sendMessage("command.rtp.cancelled");
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
                    this.source.getServiceCollection().logger()
                            .debug(String.format("RTP of %s was unsuccessful", this.target.getName()));
                    this.source.sendMessage("command.rtp.error");
                    onCancel();
                } else {
                    // We're using a scheduler to allow some ticks to go by between attempts to find a
                    // safe place.
                    RandomTeleportCommand.this.cachedTasks.put(
                            Sponge.getScheduler().createTaskBuilder().delayTicks(2).execute(this).submit(this.pluginContainer),
                            target.getUniqueId()
                    );
                }

                RandomTeleportCommand.this.cachedTasks.remove(task);
            }
        }
    }

}
