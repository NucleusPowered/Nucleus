/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mob.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.neutrino.annotations.ProcessSetting;
import io.github.nucleuspowered.neutrino.settingprocessor.LowercaseListSettingProcessor;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import uk.co.drnaylor.quickstart.config.NoMergeIfPresent;

import java.util.List;

@ConfigSerializable
public class BlockSpawnsConfig {

    @Setting(value = "block-vanilla-mobs", comment = "config.blockspawn.vanilla")
    private boolean blockVanillaMobs = false;

    @NoMergeIfPresent
    @Setting(value = "block-mobs-with-ids", comment = "config.blockspawn.ids")
    @ProcessSetting(LowercaseListSettingProcessor.class)
    private List<String> idsToBlock = Lists.newArrayList();

    public boolean isBlockVanillaMobs() {
        return this.blockVanillaMobs;
    }

    public List<String> getIdsToBlock() {
        return this.idsToBlock;
    }
}
