/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.internal.userprefs.PreferenceKeyImpl;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPrefKeys;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportToggleCommand;

public class TeleportUserPrefKeys implements UserPrefKeys {

    // String key, @Nullable Boolean def, String permission, String descriptionKey
    public static final PreferenceKeyImpl<Boolean> TELEPORT_TARGETABLE = new PreferenceKeyImpl.BooleanKey(
            NucleusKeysProvider.TELEPORT_TARGETABLE_KEY,
            true,
            Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(TeleportToggleCommand.class).getBase(),
            "userpref.teleporttarget"
    );
}
