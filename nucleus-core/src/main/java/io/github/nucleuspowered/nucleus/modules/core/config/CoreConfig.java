/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.config;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.neutrino.annotations.DoNotGenerate;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import uk.co.drnaylor.quickstart.config.NoMergeIfPresent;

import java.util.Map;
import java.util.UUID;

@ConfigSerializable
public class CoreConfig {

    @Setting(value = "debug-mode", comment = "config.debugmode")
    private boolean debugmode = false;

    @Setting(value = "print-on-autosave", comment = "config.printonautosave")
    private boolean printOnAutosave = false;

    @Setting(value = "use-custom-message-file", comment = "config.custommessages")
    private boolean custommessages = false;

    @Setting(value = "warmup-canceling", comment = "config.core.warmup.info")
    private WarmupConfig warmupConfig = new WarmupConfig();

    @Setting(value = "command-on-name-click", comment = "config.core.commandonname")
    private String commandOnNameClick = "/msg {{subject}}";

    @Setting(value = "kick-on-stop")
    private KickOnStopConfig kickOnStop = new KickOnStopConfig();

    @DoNotGenerate
    @Setting(value = "simulate-error-on-startup")
    private boolean errorOnStartup = false;

    @Setting(value = "safe-teleport-check", comment = "config.core.safeteleport")
    private SafeTeleportConfig safeTeleportConfig = new SafeTeleportConfig();

    @Setting(value = "console-overrides-exemptions", comment = "config.core.consoleoverrides")
    private boolean consoleOverride = true;

    @NoMergeIfPresent
    @Setting(value = "world-uuid-migration", comment = "config.core.worlduuidmigration")
    private Map<UUID, String> uuidMigration = Maps.newHashMap();

    @Setting(value = "check-for-wildcard", comment = "config.core.wildcard")
    private boolean checkForWildcard = true;

    @Setting(value = "show-warning-on-startup", comment = "config.core.warning-on-startup")
    private boolean warningOnStartup = true;

    @Setting(value = "more-accurate-visitor-count", comment = "config.core.accurate")
    private boolean moreAccurate = false;

    @Setting(value = "override-language", comment = "config.core.language")
    private String serverLocale = "default";

    @Setting(value = "data-file-location", comment = "config.core.datafilelocation")
    private String dataFileLocation = "default";

    @Setting(value = "offline-user-tab-limit", comment = "config.core.offlineusertablimit")
    private int nicknameArgOfflineLimit = 20;

    @Setting(value = "enable-parent-perms", comment = "config.core.parentperms")
    private boolean useParentPerms = true;

    @Setting(value = "enable-partial-name-matching", comment = "config.core.partialname")
    private boolean partialMatch = true;

    @Setting(value = "use-client-locale-where-possible", comment = "config.core.clientlocale")
    private boolean clientLocale = false;

    @Setting(value = "give-default-group-user-permissions", comment = "config.core.defaultperms")
    private boolean giveDefaultsUserPermissions = false;

    @Setting(value = "check-first-date-played-on-first-joined", comment = "config.core.firstdateplayed")
    private boolean checkFirstDatePlayed = true;

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

    public NucleusTextTemplateImpl getKickOnStopMessage() {
        return this.kickOnStop.getKickOnStopMessage();
    }

    public boolean isErrorOnStartup() {
        return this.errorOnStartup;
    }

    public SafeTeleportConfig getSafeTeleportConfig() {
        return this.safeTeleportConfig;
    }

    public boolean isConsoleOverride() {
        return this.consoleOverride;
    }

    public Map<UUID, String> getUuidMigration() {
        return this.uuidMigration;
    }

    public boolean isCheckForWildcard() {
        return this.checkForWildcard;
    }

    public boolean isWarningOnStartup() {
        return this.warningOnStartup;
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
