/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class KitConfig {

    @Setting(value = "must-get-all-items", comment = "config.kits.mustgetall")
    private boolean mustGetAll = false;

    @Setting(value = "drop-items-if-inventory-full", comment = "config.kits.full")
    private boolean dropKitIfFull = false;

    @Setting(value = "process-tokens-in-lore", comment = "config.kits.process-tokens")
    private boolean processTokens = false;

    @Setting(value = "auto-redeem")
    private AutoRedeem autoRedeem = new AutoRedeem();

    public boolean isMustGetAll() {
        return this.mustGetAll;
    }

    public boolean isDropKitIfFull() {
        return this.dropKitIfFull;
    }

    public boolean isProcessTokens() {
        return this.processTokens;
    }

    private AutoRedeem getAutoRedeem() {
        if (this.autoRedeem == null) {
            this.autoRedeem = new AutoRedeem();
        }

        return this.autoRedeem;
    }

    public boolean isEnableAutoredeem() {
        return getAutoRedeem().enableAutoRedeem;
    }

    public boolean isLogAutoredeem() {
        return getAutoRedeem().logAutoredeem;
    }

    @ConfigSerializable
    public static class AutoRedeem {
        @Setting(value = "log", comment = "config.kits.logauto")
        private boolean logAutoredeem = false;

        @Setting(value = "enable", comment = "config.kits.enableauto")
        private boolean enableAutoRedeem = false;
    }

}
