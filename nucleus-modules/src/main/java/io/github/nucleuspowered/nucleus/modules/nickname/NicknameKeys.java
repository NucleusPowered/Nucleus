/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;
import io.leangen.geantyref.TypeToken;

public final class NicknameKeys {

    public static final DataKey<String, IUserDataObject> USER_NICKNAME_JSON = DataKey.of(
            TypeToken.get(String.class),
            IUserDataObject.class,
            "nickname-text"
    );
}
