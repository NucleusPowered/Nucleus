/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.notification.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class NotificationConfig {

    @Setting(value = "broadcast-message-template", comment = "config.broadcast.template")
    private BroadcastConfig broadcastMessage = new BroadcastConfig();

    @Setting(value = "title-defaults", comment = "config.title.defaults")
    private TitleConfig titleConfig = new TitleConfig();

    public BroadcastConfig getBroadcastMessage() {
        return this.broadcastMessage;
    }

    public TitleConfig getTitleDefaults() {
        return this.titleConfig;
    }
}
