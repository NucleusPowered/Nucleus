/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.config;

import io.github.nucleuspowered.nucleus.core.services.interfaces.annotation.configuratehelper.LocalisedComment;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class CoreConfig {

    @Setting(value = "debug-mode")
    @LocalisedComment("config.debugmode")
    private boolean debugmode = false;

    @Setting(value = "print-on-autosave")
    @LocalisedComment("config.printonautosave")
    private boolean printOnAutosave = false;

    @Setting(value = "use-custom-message-file")
    @LocalisedComment("config.custommessages")
    private boolean custommessages = false;

    @Setting(value = "warmup-canceling")
    @LocalisedComment("config.core.warmup.info")
    private WarmupConfig warmupConfig = new WarmupConfig();

    @Setting(value = "command-on-name-click")
    @LocalisedComment("config.core.commandonname")
    private String commandOnNameClick = "/msg {{subject}}";

    @Setting(value = "kick-on-stop")
    private KickOnStopConfig kickOnStop = new KickOnStopConfig();

    @Setting(value = "safe-teleport-check")
    @LocalisedComment("config.core.safeteleport")
    private SafeTeleportConfig safeTeleportConfig = new SafeTeleportConfig();

    @Setting(value = "console-overrides-exemptions")
    @LocalisedComment("config.core.consoleoverrides")
    private boolean consoleOverride = true;

    @Setting(value = "check-for-wildcard")
    @LocalisedComment("config.core.wildcard")
    private boolean checkForWildcard = true;

    @Setting(value = "more-accurate-visitor-count")
    @LocalisedComment("config.core.accurate")
    private boolean moreAccurate = false;

    @Setting(value = "override-language")
    @LocalisedComment("config.core.language")
    private String serverLocale = "default";

    @Setting(value = "data-file-location")
    @LocalisedComment("config.core.datafilelocation")
    private String dataFileLocation = "default";

    @Setting(value = "offline-user-tab-limit")
    @LocalisedComment("config.core.offlineusertablimit")
    private int nicknameArgOfflineLimit = 20;

    @Setting(value = "enable-parent-perms")
    @LocalisedComment("config.core.parentperms")
    private boolean useParentPerms = true;

    @Setting(value = "enable-partial-name-matching")
    @LocalisedComment("config.core.partialname")
    private boolean partialMatch = true;

    @Setting(value = "use-client-locale-where-possible")
    @LocalisedComment("config.core.clientlocale")
    private boolean clientLocale = false;

    @Setting(value = "give-default-group-user-permissions")
    @LocalisedComment("config.core.defaultperms")
    private boolean giveDefaultsUserPermissions = false;

    @Setting(value = "check-first-date-played-on-first-joined")
    @LocalisedComment("config.core.firstdateplayed")
    private boolean checkFirstDatePlayed = true;

    @Setting(value = "data-storage")
    @LocalisedComment("config.core.datastorage")
    private StorageConfig storageConfig = new StorageConfig();

    public StorageConfig getStorageConfig() {
        return this.storageConfig;
    }

    public String getDataFileLocation() {
        return this.dataFileLocation;
    }

    public boolean isDebugmode() {
        return this.debugmode;
    }

    public boolean isPrintOnAutosave() {
        return this.printOnAutosave;
    }

    public boolean isCustommessages() {
        return this.custommessages;
    }

    public WarmupConfig getWarmupConfig() {
        return this.warmupConfig;
    }

    public String getCommandOnNameClick() {
        return this.commandOnNameClick;
    }

    public boolean isKickOnStop() {
        return this.kickOnStop.isKickOnStop();
    }

    public String getKickOnStopMessage() {
        return this.kickOnStop.getKickOnStopMessage();
    }

    public SafeTeleportConfig getSafeTeleportConfig() {
        return this.safeTeleportConfig;
    }

    public boolean isConsoleOverride() {
        return this.consoleOverride;
    }

    public boolean isCheckForWildcard() {
        return this.checkForWildcard;
    }

    public boolean isMoreAccurate() {
        return this.moreAccurate;
    }

    public String getServerLocale() {
        return this.serverLocale;
    }

    public int getNicknameArgOfflineLimit() {
        return this.nicknameArgOfflineLimit;
    }

    public boolean isUseParentPerms() {
        return this.useParentPerms;
    }

    public boolean isPartialMatch() {
        return this.partialMatch;
    }

    public boolean isClientLocaleWhenPossible() {
        return this.clientLocale;
    }

    public boolean isGiveDefaultsUserPermissions() {
        return this.giveDefaultsUserPermissions;
    }

    public boolean isCheckFirstDatePlayed() {
        return this.checkFirstDatePlayed;
    }
}
