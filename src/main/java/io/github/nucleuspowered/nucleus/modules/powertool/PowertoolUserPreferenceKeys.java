/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.internal.userprefs.PreferenceKeyImpl;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPrefKeys;
import io.github.nucleuspowered.nucleus.modules.powertool.commands.TogglePowertoolCommand;

public class PowertoolUserPreferenceKeys implements UserPrefKeys {

    // String key, @Nullable Boolean def, String permission, String descriptionKey
    public static final PreferenceKeyImpl<Boolean> POWERTOOL_ENABLED = new PreferenceKeyImpl.BooleanKey(
            NucleusKeysProvider.POWERTOOL_ENABLED_KEY, true,
            Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(TogglePowertoolCommand.class).getBase(),
            "userpref.powertooltoggle"
    );

}
