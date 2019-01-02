package io.github.nucleuspowered.nucleus.modules.ignore;

import io.github.nucleuspowered.nucleus.internal.TypeTokens;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

import java.util.List;
import java.util.UUID;

public class IgnoreKeys {

    public static DataKey<List<UUID>, IUserDataObject> IGNORED = DataKey.of(TypeTokens.UUID_LIST, IUserDataObject.class, "ignoreList");

}
