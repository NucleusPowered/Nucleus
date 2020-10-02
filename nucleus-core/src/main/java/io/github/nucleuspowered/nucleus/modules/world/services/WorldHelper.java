/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.services;

import com.flowpowered.math.GenericMath;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.modules.world.WorldKeys;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.modules.world.config.WorldConfig;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IWorldDataObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.spongepowered.api.event.world.ChunkPreGenerationEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.ChunkPreGenerate;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class WorldHelper implements IReloadableService.Reloadable, ServiceBase {

    private final INucleusServiceCollection serviceCollection;

    private boolean notify = false;
    private boolean display = true;
    private long timeToNotify = 20 * 1000L;

    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");
    private static final String TIME_FORMAT = "s's 'S'ms'";

    private final Map<UUID, ChunkPreGenerate> pregen = Maps.newHashMap();

    @Inject
    public WorldHelper(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    public boolean isPregenRunningForWorld(UUID uuid) {
        cleanup();
        return this.pregen.containsKey(uuid);
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        WorldConfig config = serviceCollection.moduleDataProvider().getModuleConfig(WorldConfig.class);
        this.notify = config.isDisplayAfterEachGen();
        this.display = config.isDisplayWarningGeneration();
        this.timeToNotify = config.getNotificationInterval() * 1000L;
    }

    public boolean startPregenningForWorld(World world, int aggressiveLevel, long saveTime, @Nullable Integer tickPercent,
            @Nullable Integer tickFrequency, boolean onRestart) {
        cleanup();
        if (!isPregenRunningForWorld(world.getUniqueId())) {
            WorldProperties wp = world.getProperties();
            ChunkPreGenerate.Builder wbcp = world.newChunkPreGenerate(wp.getWorldBorderCenter(), wp.getWorldBorderDiameter())
                .owner(this.serviceCollection.pluginContainer()).addListener(new Listener(this.serviceCollection,
                            aggressiveLevel,
                            saveTime,
                            this.notify,
                            this.display,
                            this.timeToNotify));


            if (tickPercent != null) {
                wbcp.tickPercentLimit(Math.max(0f, Math.min(tickPercent / 100.0f, 1f)));
            } else if (aggressiveLevel == 2) {
                wbcp.tickPercentLimit(0.95f);
            } else if (aggressiveLevel == 1) {
                wbcp.tickPercentLimit(0.9f);
            } else {
                wbcp.tickPercentLimit(0.8f);
            }

            if (tickFrequency != null) {
                tickFrequency = Math.max(1, tickFrequency);
            } else if (aggressiveLevel == 2) {
                tickFrequency = 1;
            } else if (aggressiveLevel == 1) {
                tickFrequency = 3;
            } else {
                tickFrequency = 4;
            }
            wbcp.tickInterval(tickFrequency);

            if (onRestart) {
                IWorldDataObject service = this.serviceCollection
                        .storageManager()
                        .getWorldService()
                        .getOrNewOnThread(world.getUniqueId());

                service.set(WorldKeys.WORLD_PREGEN_START, true);
                service.set(WorldKeys.WORLD_PREGEN_AGGRESSIVE_LEVEL, aggressiveLevel);
                service.set(WorldKeys.WORLD_PREGEN_SAVE_FREQUENCY, saveTime);
                service.set(WorldKeys.WORLD_PREGEN_TICK_FREQUENCY, tickFrequency);
                service.set(WorldKeys.WORLD_PREGEN_TICK_PERCENT, tickPercent);
                this.serviceCollection.storageManager()
                        .getWorldService().save(world.getUniqueId(), service);
            }

            this.pregen.put(world.getUniqueId(), wbcp.start());
            return true;
        }

        return false;
    }

    public boolean cancelPregenRunningForWorld(UUID uuid) {
        cleanup();
        if (this.pregen.containsKey(uuid)) {
            ChunkPreGenerate cpg = this.pregen.remove(uuid);
            getChannel().send(
                this.serviceCollection.messageProvider().getMessage("command.pregen.gen.cancelled2",
                    String.valueOf(cpg.getTotalGeneratedChunks()),
                    String.valueOf(cpg.getTotalSkippedChunks()),
                    DurationFormatUtils.formatDuration(cpg.getTotalTime().toMillis(), TIME_FORMAT, false)
                ));

            cpg.cancel();
            return true;
        }

        return false;
    }

    private synchronized void cleanup() {
        this.pregen.entrySet().removeIf(x -> x.getValue().isCancelled());
    }

    private MessageChannel getChannel() {
        return MessageChannel.combined(MessageChannel.TO_CONSOLE,
                this.serviceCollection.permissionService().permissionMessageChannel(WorldPermissions.WORLD_BORDER_GEN_NOTIFY));
    }

    private static class Listener implements Consumer<ChunkPreGenerationEvent> {

        private final int aggressiveLevel;
        private final long timeToSave;
        private final INucleusServiceCollection serviceCollection;
        private final boolean notify;
        private final boolean display;
        private final long timeToNotify;
        private boolean highMemTriggered = false;
        private long time = 0;
        private long lastSaveTime;
        private long lastNotifyTime;

        Listener(INucleusServiceCollection serviceCollection,
                int aggressiveLevel,
                long timeToSave,
                boolean notify,
                boolean display,
                long timeToNotify) {
            this.serviceCollection = serviceCollection;
            this.aggressiveLevel = aggressiveLevel;
            this.lastSaveTime = System.currentTimeMillis();
            this.timeToSave = timeToSave;
            this.notify = notify;
            this.display = display;
            this.timeToNotify = timeToNotify;
        }

        @Override public void accept(ChunkPreGenerationEvent event) {
            ChunkPreGenerate cpg = event.getChunkPreGenerate();
            if (event instanceof ChunkPreGenerationEvent.Pre) {
                if (this.aggressiveLevel == 0) {
                    long percent = getMemPercent();
                    if (percent >= 90) {
                        if (!this.highMemTriggered) {
                            event.getTargetWorld().getLoadedChunks().forEach(Chunk::unloadChunk);
                            save(event.getTargetWorld());
                            this.serviceCollection.messageProvider().getMessage("command.pregen.gen.memory.high", String.valueOf(percent));
                            this.highMemTriggered = true;
                            save(event.getTargetWorld());
                        }

                        // Try again next tick.
                        ((ChunkPreGenerationEvent.Pre) event).setSkipStep(true);
                    } else if (this.highMemTriggered && percent <= 80) {
                        // Get the memory usage down to 80% to prevent too much ping pong.
                        this.highMemTriggered = false;
                        this.serviceCollection.messageProvider().getMessage("command.pregen.gen.memory.low");
                    }
                }
            } else if (event instanceof ChunkPreGenerationEvent.Post) {

                ChunkPreGenerationEvent.Post cpp = ((ChunkPreGenerationEvent.Post) event);
                Text message;
                this.time += cpp.getTimeTakenForStep().toMillis();
                if (cpp.getChunksSkippedThisStep() > 0) {
                    message = this.serviceCollection.messageProvider().getMessage("command.pregen.gen.notifyskipped",
                        String.valueOf(cpp.getChunksGeneratedThisStep()),
                        String.valueOf(cpp.getChunksSkippedThisStep()),
                        DurationFormatUtils.formatDuration(cpp.getTimeTakenForStep().toMillis(), TIME_FORMAT, false),
                        DurationFormatUtils.formatDuration(cpp.getChunkPreGenerate().getTotalTime().toMillis(), TIME_FORMAT, false),
                        String.valueOf(GenericMath.floor((cpg.getTotalGeneratedChunks() * 100) / cpg.getTargetTotalChunks())));
                } else {
                    message = this.serviceCollection.messageProvider().getMessage("command.pregen.gen.notify",
                        String.valueOf(cpp.getChunksGeneratedThisStep()),
                        DurationFormatUtils.formatDuration(cpp.getTimeTakenForStep().toMillis(), TIME_FORMAT, false),
                        DurationFormatUtils.formatDuration(cpp.getChunkPreGenerate().getTotalTime().toMillis(), TIME_FORMAT, false),
                        String.valueOf(GenericMath.floor((cpg.getTotalGeneratedChunks() * 100) / cpg.getTargetTotalChunks())));
                }

                if (this.notify) {
                    getChannel().send(message);
                }

                long time = System.currentTimeMillis();
                if (this.lastSaveTime + this.timeToSave < time) {
                    save(event.getTargetWorld());
                }

                if (this.display && this.lastNotifyTime + this.timeToNotify < time) {
                    MessageChannel.TO_ALL.send(this.serviceCollection.messageProvider().getMessage("command.pregen.gen.all"));

                    double total = 100*(cpg.getTotalGeneratedChunks() + cpg.getTotalSkippedChunks())/(double) cpg.getTargetTotalChunks();

                    getChannel().send(this.serviceCollection.messageProvider().getMessage("command.pregen.gen.quickstatus",
                            WorldHelper.PERCENT_FORMAT.format(total),
                            String.valueOf(cpg.getTotalGeneratedChunks() + cpg.getTotalSkippedChunks()),
                            String.valueOf(cpg.getTargetTotalChunks())
                    ));
                    this.lastNotifyTime = time;
                }
            } else if (event instanceof ChunkPreGenerationEvent.Complete) {
                getChannel().send(
                        this.serviceCollection.messageProvider().getMessage("command.pregen.gen.completed",
                        String.valueOf(cpg.getTotalGeneratedChunks()),
                        String.valueOf(cpg.getTotalSkippedChunks()),
                        DurationFormatUtils.formatDuration(this.time, TIME_FORMAT, false),
                        DurationFormatUtils.formatDuration(cpg.getTotalTime().toMillis(), TIME_FORMAT, false)
                    ));
                this.serviceCollection.storageManager()
                        .getWorldService()
                        .getOrNew(event.getTargetWorld().getUniqueId())
                        .thenAccept(x -> x.set(WorldKeys.WORLD_PREGEN_START, false));
            }
        }

        private long getMemPercent() {
            // Check system memory
            long max = Runtime.getRuntime().maxMemory() / 1024 / 1024;
            long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
            long free = Runtime.getRuntime().freeMemory() / 1024 / 1024;

            return ((max - total + free ) * 100)/ max;
        }

        private void save(World world) {
            getChannel().send(
                    this.serviceCollection.messageProvider().getMessage("command.pregen.gen.saving"));
            try {
                world.save();
                getChannel().send(
                        this.serviceCollection.messageProvider().getMessage("command.pregen.gen.saved"));
            } catch (Throwable e) {
                getChannel().send(
                        this.serviceCollection.messageProvider().getMessage("command.pregen.gen.savefailed"));
                e.printStackTrace();
            } finally {
                this.lastSaveTime = System.currentTimeMillis();
            }
        }

        private MessageChannel getChannel() {
            return MessageChannel.combined(MessageChannel.TO_CONSOLE,
                    this.serviceCollection.permissionService().permissionMessageChannel(WorldPermissions.WORLD_BORDER_GEN_NOTIFY));
        }
    }
}
