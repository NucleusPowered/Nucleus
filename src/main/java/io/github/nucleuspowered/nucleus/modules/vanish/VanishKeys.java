package io.github.nucleuspowered.nucleus.modules.vanish;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

public class VanishKeys {

    public static DataKey<Boolean, IUserDataObject> VANISH_STATUS = DataKey.of(
            false,
            TypeToken.of(Boolean.class),
            IUserDataObject.class,
            "vanish");
}
