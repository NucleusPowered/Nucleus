/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.userprefs;

import io.github.nucleuspowered.nucleus.api.service.NucleusUserPreferenceService;

import java.util.Optional;

import javax.annotation.Nullable;

public class NucleusKeysProvider implements NucleusUserPreferenceService.Keys {

    public final static String COMMAND_SPY_KEY = "nucleus:command-spy";
    public final static String SOCIAL_SPY_KEY = "nucleus:social-spy";
    public final static String MESSAGE_TOGGLE_KEY = "nucleus:message-receiving-enabled";
    public final static String POWERTOOL_ENABLED_KEY = "nucleus:powertool-toggle";
    public static final String TELEPORT_TARGETABLE_KEY = "nucleus:teleport-targetable";
    public static final String VANISH_ON_LOGIN_KEY = "nucleus:vanish-on-login";
    public static final String VIEW_STAFF_CHAT_KEY = "nucleus:view-staff-chat";

    @TargetID(VANISH_ON_LOGIN_KEY) @Nullable private PreferenceKeyImpl<Boolean> vanishOnLoginKey = null;
    @TargetID(TELEPORT_TARGETABLE_KEY) @Nullable private PreferenceKeyImpl<Boolean> teleportTargetKey = null;
    @TargetID(POWERTOOL_ENABLED_KEY) @Nullable private PreferenceKeyImpl<Boolean> powertoolEnabledKey = null;
    @TargetID(SOCIAL_SPY_KEY) @Nullable private PreferenceKeyImpl<Boolean> socialSpyEnabled = null;
    @TargetID(MESSAGE_TOGGLE_KEY) @Nullable private PreferenceKeyImpl<Boolean> messageToggle = null;
    @TargetID(COMMAND_SPY_KEY) @Nullable private PreferenceKeyImpl<Boolean> commandSpyEnabled = null;
    @TargetID(VIEW_STAFF_CHAT_KEY) @Nullable private PreferenceKeyImpl<Boolean> viewStaffChat = null;

    @Override public Optional<NucleusUserPreferenceService.PreferenceKey<Boolean>> vanishOnLogin() {
        return Optional.ofNullable(this.vanishOnLoginKey);
    }

    @Override public Optional<NucleusUserPreferenceService.PreferenceKey<Boolean>> teleportTarget() {
        return Optional.ofNullable(this.teleportTargetKey);
    }

    @Override public Optional<NucleusUserPreferenceService.PreferenceKey<Boolean>> powertoolsEnabled() {
        return Optional.ofNullable(this.powertoolEnabledKey);
    }

    @Override public Optional<NucleusUserPreferenceService.PreferenceKey<Boolean>> socialSpyEnabled() {
        return Optional.ofNullable(this.socialSpyEnabled);
    }

    @Override public Optional<NucleusUserPreferenceService.PreferenceKey<Boolean>> messageReceivingEnabled() {
        return Optional.ofNullable(this.messageToggle);
    }

    @Override public Optional<NucleusUserPreferenceService.PreferenceKey<Boolean>> commandSpyEnabled() {
        return Optional.ofNullable(this.commandSpyEnabled);
    }

    @Override public Optional<NucleusUserPreferenceService.PreferenceKey<Boolean>> viewStaffChat() {
        return Optional.ofNullable(this.viewStaffChat);
    }

}
