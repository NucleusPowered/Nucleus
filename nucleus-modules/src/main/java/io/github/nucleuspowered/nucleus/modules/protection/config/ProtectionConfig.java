/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.protection.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.neutrino.annotations.ProcessSetting;
import io.github.nucleuspowered.nucleus.configurate.settingprocessor.MobTypeSettingProcessor;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.entity.EntityType;

import java.util.List;

@ConfigSerializable
public class ProtectionConfig {

    @Setting(value = "disable-crop-trample", comment = "config.protection.disablecrop")
    private CropTrample disableCropTrample = new CropTrample();

    @Setting(value = "mob-griefing")
    private BlockBreaking blockBreaking = new BlockBreaking();

    public boolean isDisableAnyCropTrample() {
        return this.disableCropTrample.players || this.disableCropTrample.mobs;
    }

    public boolean isDisablePlayerCropTrample() {
        return this.disableCropTrample.players;
    }

    public boolean isDisableMobCropTrample() {
        return this.disableCropTrample.mobs;
    }

    public boolean isEnableProtection() {
        return this.blockBreaking.enableProtection;
    }

    public List<EntityType> getWhitelistedEntities() {
        return this.blockBreaking.whitelist;
    }

    @ConfigSerializable
    public static class CropTrample {

        @Setting
        private boolean players = false;

        @Setting
        private boolean mobs = false;
    }

    @ConfigSerializable
    public static class BlockBreaking {

        @Setting(value = "enable-protection", comment = "config.protection.mobgriefing.flag")
        private boolean enableProtection = false;

        @Setting(value = "whitelist")
        @ProcessSetting(MobTypeSettingProcessor.class)
        private List<EntityType> whitelist = Lists.newArrayList();


    }
}
