package io.github.nucleuspowered.nucleus.modules.mute;

import io.github.nucleuspowered.nucleus.internal.TypeTokens;
import io.github.nucleuspowered.nucleus.modules.mute.data.MuteData;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

public class MuteKeys {

    public static final DataKey<MuteData, IUserDataObject> MUTE_DATA =
            DataKey.of(TypeTokens.MUTE_DATA, IUserDataObject.class, "muteData");
}
