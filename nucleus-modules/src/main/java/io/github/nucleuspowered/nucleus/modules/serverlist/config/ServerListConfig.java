/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.neutrino.annotations.Default;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import uk.co.drnaylor.quickstart.config.NoMergeIfPresent;

import java.util.List;

@ConfigSerializable
public class ServerListConfig {

    @Setting(value = "modify-server-list-messages", comment = "config.serverlist.modify")
    private ServerListSelection modifyServerList = ServerListSelection.FALSE;

    @Setting(value = "hide-vanished-players", comment = "config.serverlist.hidevanished")
    private boolean hideVanishedPlayers = false;

    @Setting(value = "hide-player-count", comment = "config.serverlist.hideall")
    private boolean hidePlayerCount = false;

    @NoMergeIfPresent
    @Default("&bWelcome to the server!\n&cCome join us!")
    @Setting(value = "server-list-messages", comment = "config.serverlist.messages")
    public List<NucleusTextTemplateImpl> messages;

    @NoMergeIfPresent
    @Setting(value = "whitelist-server-list-messages", comment = "config.serverlist.whitelistmessages")
    public List<NucleusTextTemplateImpl> whitelist = Lists.newArrayList();

    public boolean isModifyServerList() {
        return this.modifyServerList == ServerListSelection.TRUE;
    }

    public boolean isHideVanishedPlayers() {
        return this.hideVanishedPlayers;
    }

    public boolean isHidePlayerCount() {
        return this.hidePlayerCount;
    }

    public List<NucleusTextTemplateImpl> getMessages() {
        return this.messages;
    }

    public List<NucleusTextTemplateImpl> getWhitelist() {
        return this.whitelist;
    }

    public boolean enableListener() {
        return this.modifyServerList == ServerListSelection.TRUE || this.hideVanishedPlayers || this.hidePlayerCount;
    }

    public boolean enableWhitelistListener() {
        return this.modifyServerList == ServerListSelection.WHITELIST;
    }

    public ServerListSelection getModifyServerList() {
        return this.modifyServerList;
    }

    public enum ServerListSelection {
        TRUE,
        WHITELIST,
        FALSE
    }
}
