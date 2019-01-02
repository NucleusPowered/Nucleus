/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.datamodules;

import io.github.nucleuspowered.nucleus.dataservices.modular.DataKey;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.IWorldDataModule;

public class WorldgenWorldDataModule implements IWorldDataModule {

    @DataKey("start-pregen")
    private boolean start = false;

    @DataKey("save-time")
    private long saveTime = 20L;

    @DataKey("tick-percent")
    private int tickPercent = 80;

    @DataKey("tick-freq")
    private int tickFreq = 4;

    @DataKey("aggressive")
    private boolean aggressive = false;

    public boolean isStart() {
        return start;
    }

    public WorldgenWorldDataModule setStart(boolean start) {
        this.start = start;
        return this;
    }

    public long getSaveTime() {
        return saveTime;
    }

    public WorldgenWorldDataModule setSaveTime(long saveTime) {
        this.saveTime = saveTime;
        return this;
    }

    public int getTickPercent() {
        return tickPercent;
    }

    public WorldgenWorldDataModule setTickPercent(int tickPercent) {
        this.tickPercent = tickPercent;
        return this;
    }

    public int getTickFreq() {
        return tickFreq;
    }

    public WorldgenWorldDataModule setTickFreq(int tickFreq) {
        this.tickFreq = tickFreq;
        return this;
    }

    public boolean isAggressive() {
        return aggressive;
    }

    public WorldgenWorldDataModule setAggressive(boolean aggressive) {
        this.aggressive = aggressive;
        return this;
    }
}
