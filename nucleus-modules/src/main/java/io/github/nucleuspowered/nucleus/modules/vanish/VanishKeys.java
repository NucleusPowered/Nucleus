/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish;

import io.github.nucleuspowered.nucleus.api.core.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.core.services.impl.userprefs.PreferenceKeyImpl;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;

public final class VanishKeys {

    public static final NucleusUserPreferenceService.PreferenceKey<Boolean> VANISH_ON_LOGIN = new PreferenceKeyImpl.BooleanKey(
            NucleusKeysProvider.VANISH_ON_LOGIN_KEY,
            false,
            VanishPermissions.VANISH_ONLOGIN,
            "userpref.vanishonlogin"
    );

    public static final DataKey<Boolean, IUserDataObject> VANISH_STATUS = DataKey.of(
            false,
            TypeTokens.BOOLEAN,
            IUserDataObject.class,
            "vanish");
}
