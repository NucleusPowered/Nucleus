/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool;

import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.util.TypeTokens;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

public class PowertoolKeys {

    public final static DataKey.MapListKey<String, String, IUserDataObject> POWERTOOLS
            = DataKey.ofMapList(
                    TypeTokens.STRING,
                    TypeTokens.STRING,
                    IUserDataObject.class,
                    "powertools"
            );
}
