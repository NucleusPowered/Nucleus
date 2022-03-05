/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool;

import io.github.nucleuspowered.nucleus.api.core.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.core.services.impl.userprefs.PreferenceKeyImpl;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;

public final class PowertoolKeys {

    public static final NucleusUserPreferenceService.PreferenceKey<Boolean> POWERTOOL_ENABLED = new PreferenceKeyImpl.BooleanKey(
                    NucleusKeysProvider.POWERTOOL_ENABLED_KEY,
                    true,
                    PowertoolPermissions.BASE_POWERTOOL,
                    "userpref.powertooltoggle"
            );

    public final static DataKey.StringKeyedMapListKey<String, IUserDataObject> POWERTOOLS = DataKey.ofMapList(
                    TypeTokens.STRING,
                    IUserDataObject.class,
                    "powertools"
            );
}
