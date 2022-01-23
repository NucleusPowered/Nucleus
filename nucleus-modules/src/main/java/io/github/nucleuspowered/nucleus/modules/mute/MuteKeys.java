/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute;

import io.github.nucleuspowered.nucleus.modules.mute.data.MuteData;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;
import io.leangen.geantyref.TypeToken;

public final class MuteKeys {

    public static final TypeToken<MuteData> MUTE_DATA_KEY = TypeToken.get(MuteData.class);

    public static final DataKey<MuteData, IUserDataObject> MUTE_DATA = DataKey.of(MuteKeys.MUTE_DATA_KEY, IUserDataObject.class, "muteData");
}
