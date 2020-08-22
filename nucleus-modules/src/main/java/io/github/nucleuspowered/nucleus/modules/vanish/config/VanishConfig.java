/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class VanishConfig {

    @Setting(value = "hide-connection-messages-on-vanish", comment = "config.vanish.connectionmessages")
    private boolean suppressMessagesOnVanish = false;

    //@RequiresProperty("nucleus.vanish.tablist.enable")
    @Setting(value = "alter-tab-list", comment = "config.vanish.altertablist")
    private boolean alterTabList = false;

    @Setting(value = "force-nucleus-vanish", comment = "config.vanish.force")
    private boolean forceNucleusVanish = true;

    @Setting(value = "workaround-sponge-vanish-issue", comment = "config.vanish.sponge")
    private boolean attemptSpongeWorkaroundVanish = true;

    @Setting(value = "try-hide-players-in-seen", comment = "config.vanish.hideseen")
    private boolean tryHidePlayers = true;

    public boolean isSuppressMessagesOnVanish() {
        return this.suppressMessagesOnVanish;
    }

    public boolean isAlterTabList() {
        return this.alterTabList;
    }

    public boolean isForceNucleusVanish() {
        return this.forceNucleusVanish;
    }

    public boolean isAttemptSpongeWorkaroundVanish() {
        return this.attemptSpongeWorkaroundVanish;
    }

    public boolean isTryHidePlayers() {
        return this.tryHidePlayers;
    }
}
