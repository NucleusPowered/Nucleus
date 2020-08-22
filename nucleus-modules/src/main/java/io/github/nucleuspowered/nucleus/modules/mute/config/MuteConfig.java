/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.configurate.config.CommonPermissionLevelConfig;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.List;

@ConfigSerializable
public class MuteConfig {

    @Setting(value = "blocked-commands", comment = "config.mute.blocked")
    private List<String> blockedCommands = Lists.newArrayList("minecraft:me", "say");

    @Setting(value = "maximum-mute-length", comment = "config.mute.maxmutelength")
    private long maxMuteLength = 604800;

    @Setting(value = "see-muted-chat", comment = "config.mute.seemutedchat")
    private boolean showMutedChat = false;

    @Setting(value = "muted-chat-tag", comment = "config.mute.seemutedchattag")
    private String cancelledTag = "&c[cancelled] ";

    @Setting(value = "mute-time-counts-online-only",comment = "config.mute.countonlineonly")
    private boolean muteOnlineOnly = false;

    @Setting(value = "require-separate-unmute-permission", comment = "config.mute.unmute")
    private boolean requireUnmutePermission = false;

    @Setting(value = "mute-permission-levels", comment = "config.mute.permissionlevel")
    private CommonPermissionLevelConfig level = new CommonPermissionLevelConfig();

    public List<String> getBlockedCommands() {
        return this.blockedCommands;
    }

    public long getMaximumMuteLength() {
        return this.maxMuteLength;
    }

    public boolean isShowMutedChat() {
        return this.showMutedChat;
    }

    public String getCancelledTag() {
        return this.cancelledTag;
    }

    public boolean isMuteOnlineOnly() {
        return this.muteOnlineOnly;
    }

    public CommonPermissionLevelConfig getLevelConfig() {
        return this.level;
    }
}
