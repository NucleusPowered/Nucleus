/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core;

import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IWorldDataObject;
import io.github.nucleuspowered.nucleus.util.TypeTokens;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

import java.time.Instant;

@SuppressWarnings("UnstableApiUsage")
public class CoreKeys {

    public static final DataKey<Integer, IGeneralDataObject> GENERAL_VERSION = DataKey.of(TypeTokens.INTEGER, IGeneralDataObject.class, "data_version");

    public static final DataKey<Integer, IWorldDataObject> WORLD_VERSION = DataKey.of(TypeTokens.INTEGER, IWorldDataObject.class, "data_version");

    public static final DataKey<Integer, IUserDataObject> USER_VERSION = DataKey.of(TypeTokens.INTEGER, IUserDataObject.class, "data_version");

    public static final DataKey<String, IUserDataObject> LAST_KNOWN_NAME = DataKey.of(TypeTokens.STRING, IUserDataObject.class, "lastKnownName");

    public static final DataKey<Instant, IUserDataObject> LAST_LOGIN = DataKey.of(TypeTokens.INSTANT, IUserDataObject.class, "lastLogin");

    public static final DataKey<Instant, IUserDataObject> LAST_LOGOUT = DataKey.of(TypeTokens.INSTANT, IUserDataObject.class, "lastLogout");

    public static final DataKey<String, IUserDataObject> IP_ADDRESS = DataKey.of(TypeTokens.STRING, IUserDataObject.class, "lastIP");

    @Deprecated
    public static final DataKey<Instant, IUserDataObject> FIRST_JOIN = DataKey.of(TypeTokens.INSTANT, IUserDataObject.class, "firstJoin");

    public static final DataKey<Boolean, IUserDataObject> FIRST_JOIN_PROCESSED =
            DataKey.of(false, TypeTokens.BOOLEAN, IUserDataObject.class, "firstJoinProcessed");
}
