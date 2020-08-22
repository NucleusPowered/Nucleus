/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

public class NicknameKeys {

    public static final DataKey<String, IUserDataObject> USER_NICKNAME_JSON = DataKey.of(
            TypeToken.of(String.class),
            IUserDataObject.class,
            "nickname-text"
    );
}
