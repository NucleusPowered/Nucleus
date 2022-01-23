/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;

import java.time.Instant;

public final class KitKeys {

    public static final DataKey.MapKey<String, Instant, IUserDataObject> REDEEMED_KITS
            = DataKey.ofMap(TypeTokens.STRING, TypeTokens.INSTANT, IUserDataObject.class, "usedKits");

}
