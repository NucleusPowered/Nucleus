/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.freezeplayer;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;

public final class FreezePlayerKeys {

    public static DataKey<Boolean, IUserDataObject> FREEZE_PLAYER = DataKey.of(TypeTokens.BOOLEAN, IUserDataObject.class, "isFrozen");
}
