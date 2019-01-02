/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.services;

import com.flowpowered.math.GenericMath;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.modules.world.WorldKeys;
import io.github.nucleuspowered.nucleus.modules.world.WorldModule;
import io.github.nucleuspowered.nucleus.modules.world.commands.border.GenerateChunksCommand;
import io.github.nucleuspowered.nucleus.modules.world.config.WorldConfig;
import io.github.nucleuspowered.nucleus.modules.world.config.WorldConfigAdapter;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IWorldDataObject;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.spongepowered.api.event.world.ChunkPreGenerationEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.ChunkPreGenerate;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class WorldHelper implements Reloadable, ServiceBase {

    private boolean notify = false;
    private boolean display = true;
    private long timeToNotify = 20 * 1000L;

    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");
    private static final String TIME_FORMAT = "s's 'S'ms'";

    private static final String notifyPermission = Nucleus.getNucleus().getPermissionRegistry()
        .getPermissionsForNucleusCommand(GenerateChunksCommand.class).getPermissionWithSuffix("notify");

    private final Map<UUID, ChunkPreGenerate> pregen = Maps.newHashMap();

    public boolean isPregenRunningForWorld(UUID uuid) {
        cleanup();
        return this.pregen.containsKey(uuid);
    }

    @Override
    public void onReload() {
        WorldConfig config = Nucleus.getNucleus().getConfigValue(WorldModule.ID, WorldConfigAdapter.class, c -> c).orElseGet(WorldConfig::new);
        this.notify = config.isDisplayAfterEachGen();
        this.display = config.isDisplayWarningGeneration();
        this.timeToNotify = config.getNotificationInterval() * 1000L;
    }

    public boolean startPregenningForWorld(World world, boolean aggressive, long saveTime, @Nullable Integer tickPercent,
            @Nullable Integer tickFrequency, boolean onRestart) {
        cleanup();
        if (!isPregenRunningForWorld(world.getUniqueId())) {
            WorldProperties wp = world.getProperties();
            ChunkPreGenerate.Builder wbcp = world.newChunkPreGenerate(wp.getWorldBorderCenter(), wp.getWorldBorderDiameter())
                .owner(Nucleus.getNucleus()).addListener(new Listener(aggressive, saveTime));
            if (aggressive) {
                wbcp.tickPercentLimit(0.9f).tickInterval(3);
            }

            if (tickPercent != null) {
                wbcp.tickPercentLimit(Math.max(0f, Math.min(tickPercent / 100.0f, 1f)));
            } else {
                tickPercent = aggressive ? 90 : 80;
            }

            if (tickFrequency != null) {
                wbcp.tickInterval(Math.max(1, tickFrequency));
            } else {
                tickFrequency = 4;
            }

            if (onRestart) {
                IWorldDataObject service = Nucleus.getNucleus()
                        .getStorageManager()
                        .getWorldService()
                        .getOrNewOnThread(world.getUniqueId());

                service.set(WorldKeys.WORLD_PREGEN_START, true);
                service.set(WorldKeys.WORLD_PREGEN_AGGRESSIVE, aggressive);
                service.set(WorldKeys.WORLD_PREGEN_SAVE_FREQUENCY, saveTime);
                service.set(WorldKeys.WORLD_PREGEN_TICK_FREQUENCY, tickFrequency);
                service.set(WorldKeys.WORLD_PREGEN_TICK_PERCENT, tickPercent);
                Nucleus.getNucleus().getStorageManager()
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
                NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.pregen.gen.cancelled2",
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
        return MessageChannel.combined(MessageChannel.TO_CONSOLE, MessageChannel.permission(notifyPermission));
    }

    private class Listener implements Consumer<ChunkPreGenerationEvent> {

        private final boolean aggressive;
        private final long timeToSave;
        private boolean highMemTriggered = false;
        private long time = 0;
        private long lastSaveTime;
        private long lastNotifyTime;

        public Listener(boolean aggressive, long timeToSave) {
            this.aggressive = aggressive;
            this.lastSaveTime = System.currentTimeMillis();
            this.timeToSave = timeToSave;
        }

        @Override public void accept(ChunkPreGenerationEvent event) {
            ChunkPreGenerate cpg = event.getChunkPreGenerate();
            if (event instanceof ChunkPreGenerationEvent.Pre) {
                if (!this.aggressive) {
                    long percent = getMemPercent();
                    if (percent >= 90) {
                        if (!this.highMemTriggered) {
                            event.getTargetWorld().getLoadedChunks().forEach(Chunk::unloadChunk);
                            save(event.getTargetWorld());
                            NucleusPlugin.getNucleus().getMessageProvider()
                                .getTextMessageWithFormat("command.pregen.gen.memory.high", String.valueOf(percent));
                            this.highMemTriggered = true;
                            save(event.getTargetWorld());
                        }

                        // Try again next tick.
                        ((ChunkPreGenerationEvent.Pre) event).setSkipStep(true);
                    } else if (this.highMemTriggered && percent <= 80) {
                        // Get the memory usage down to 80% to prevent too much ping pong.
                        this.highMemTriggered = false;
                        NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.pregen.gen.memory.low");
                    }
                }
            } else if (event instanceof ChunkPreGenerationEvent.Post) {

                ChunkPreGenerationEvent.Post cpp = ((ChunkPreGenerationEvent.Post) event);
                Text message;
                this.time += cpp.getTimeTakenForStep().toMillis();
                if (cpp.getChunksSkippedThisStep() > 0) {
                    message = NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.pregen.gen.notifyskipped",
                        String.valueOf(cpp.getChunksGeneratedThisStep()),
                        String.valueOf(cpp.getChunksSkippedThisStep()),
                        DurationFormatUtils.formatDuration(cpp.getTimeTakenForStep().toMillis(), TIME_FORMAT, false),
                        DurationFormatUtils.formatDuration(cpp.getChunkPreGenerate().getTotalTime().toMillis(), TIME_FORMAT, false),
                        String.valueOf(GenericMath.floor((cpg.getTotalGeneratedChunks() * 100) / cpg.getTargetTotalChunks())));
                } else {
                    message = NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.pregen.gen.notify",
                        String.valueOf(cpp.getChunksGeneratedThisStep()),
                        DurationFormatUtils.formatDuration(cpp.getTimeTakenForStep().toMillis(), TIME_FORMAT, false),
                        DurationFormatUtils.formatDuration(cpp.getChunkPreGenerate().getTotalTime().toMillis(), TIME_FORMAT, false),
                        String.valueOf(GenericMath.floor((cpg.getTotalGeneratedChunks() * 100) / cpg.getTargetTotalChunks())));
                }

                if (WorldHelper.this.notify) {
                    getChannel().send(message);
                }

                long time = System.currentTimeMillis();
                if (this.lastSaveTime + this.timeToSave < time) {
                    save(event.getTargetWorld());
                }

                if (WorldHelper.this.display && this.lastNotifyTime + WorldHelper.this.timeToNotify < time) {
                    MessageChannel.TO_ALL.send(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.pregen.gen.all"));

                    double total = 100*(cpg.getTotalGeneratedChunks() + cpg.getTotalSkippedChunks())/(double) cpg.getTargetTotalChunks();

                    getChannel().send(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.pregen.gen.quickstatus",
                            WorldHelper.PERCENT_FORMAT.format(total),
                            String.valueOf(cpg.getTotalGeneratedChunks() + cpg.getTotalSkippedChunks()),
                            String.valueOf(cpg.getTargetTotalChunks())
                    ));
                    this.lastNotifyTime = time;
                }
            } else if (event instanceof ChunkPreGenerationEvent.Complete) {
                getChannel().send(
                    NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.pregen.gen.completed",
                        String.valueOf(cpg.getTotalGeneratedChunks()),
                        String.valueOf(cpg.getTotalSkippedChunks()),
                        DurationFormatUtils.formatDuration(this.time, TIME_FORMAT, false),
                        DurationFormatUtils.formatDuration(cpg.getTotalTime().toMillis(), TIME_FORMAT, false)
                    ));
                Nucleus.getNucleus()
                        .getStorageManager()
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
                NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.pregen.gen.saving"));
            try {
                world.save();
                getChannel().send(
                    NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.pregen.gen.saved"));
            } catch (Throwable e) {
                getChannel().send(
                    NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.pregen.gen.savefailed"));
                e.printStackTrace();
            } finally {
                this.lastSaveTime = System.currentTimeMillis();
            }
        }
    }
}
