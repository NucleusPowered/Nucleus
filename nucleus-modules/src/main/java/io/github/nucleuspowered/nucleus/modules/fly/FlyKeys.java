/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fly;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;

public final class FlyKeys {

    public static final DataKey<Boolean, IUserDataObject> FLY_TOGGLE = DataKey.of(false, TypeTokens.BOOLEAN, IUserDataObject.class, "fly");

}
