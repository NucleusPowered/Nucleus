/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

public class VanishKeys {

    public static DataKey<Boolean, IUserDataObject> VANISH_STATUS = DataKey.of(
            false,
            TypeToken.of(Boolean.class),
            IUserDataObject.class,
            "vanish");
}
