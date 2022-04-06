/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IWorldDataObject;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;

public final class EnvironmentKeys {

    public static final DataKey<Boolean, IWorldDataObject> LOCKED_WEATHER =
            DataKey.of(false, TypeTokens.BOOLEAN, IWorldDataObject.class, "lock-weather");
}
