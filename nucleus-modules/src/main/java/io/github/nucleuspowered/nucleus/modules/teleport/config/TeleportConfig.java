/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class TeleportConfig {

    @Setting(value = "use-safe-teleportation", comment = "config.teleport.safe")
    private boolean useSafeTeleport = true;

    @Setting(value = "default-quiet", comment = "config.teleport.quiet")
    private boolean defaultQuiet = true;

    @Setting(value = "refund-on-deny", comment = "config.teleport.refundondeny")
    private boolean refundOnDeny = true;

    @Setting(value = "only-same-dimension", comment = "config.teleport.onlySameDimension")
    private boolean onlySameDimension = false;

    @Setting(value = "start-cooldown-when-asking", comment = "config.teleport.cooldownOnAsk")
    private boolean cooldownOnAsk = false;

    @Setting(value = "show-clickable-tpa-accept-deny", comment = "config.teleport.clickableAcceptDeny")
    private boolean showClickableAcceptDeny = false;

    @Setting(value = "use-commands-when-clicking-tpa-accept-deny", comment = "config.teleport.useCommandOnClick")
    private boolean useCommandOnClickAcceptOrDeny = false;

    @Setting(value = "use-request-location-on-tp-requests", comment = "config.teleport.useRequestLocation")
    private boolean useRequestLocation = false;

    public boolean isDefaultQuiet() {
        return this.defaultQuiet;
    }

    public boolean isUseSafeTeleport() {
        return this.useSafeTeleport;
    }

    public boolean isRefundOnDeny() {
        return refundOnDeny;
    }

    public boolean isOnlySameDimension() {
        return onlySameDimension;
    }

    public boolean isCooldownOnAsk() {
        return cooldownOnAsk;
    }

    public boolean isUseCommandsOnClickAcceptOrDeny() {
        return this.useCommandOnClickAcceptOrDeny;
    }

    public boolean isShowClickableAcceptDeny() {
        return this.showClickableAcceptDeny;
    }

    public boolean isUseRequestLocation() {
        return this.useRequestLocation;
    }
}
