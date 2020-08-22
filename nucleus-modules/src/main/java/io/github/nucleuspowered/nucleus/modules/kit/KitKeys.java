/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit;

import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.util.TypeTokens;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

import java.time.Instant;

public class KitKeys {

    public static final DataKey.MapKey<String, Instant, IUserDataObject> REDEEMED_KITS
            = DataKey.ofMap(TypeTokens.STRING, TypeTokens.INSTANT, IUserDataObject.class, "usedKits");

    @Deprecated
    public static final DataKey.MapKey<String, Long, IUserDataObject> LEGACY_KIT_LAST_USED_TIME
            = DataKey.ofMap(TypeTokens.STRING, TypeTokens.LONG, IUserDataObject.class, "kitLastUsedTime");
}
