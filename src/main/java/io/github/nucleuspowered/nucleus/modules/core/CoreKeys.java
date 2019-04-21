/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.internal.TypeTokens;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;
import io.github.nucleuspowered.storage.dataobjects.keyed.IKeyedDataObject;

import java.time.Instant;

public class CoreKeys {

    @SuppressWarnings("unchecked")
    private static final Class<IKeyedDataObject<?>> keyedDataObjectClass = (Class<IKeyedDataObject<?>>) new TypeToken<IKeyedDataObject<?>>() {}.getRawType();

    public static DataKey<Integer, IKeyedDataObject<?>> VERSION = DataKey.of(TypeTokens.INTEGER, keyedDataObjectClass, "version");

    public static DataKey<String, IUserDataObject> LAST_KNOWN_NAME = DataKey.of(TypeTokens.STRING, IUserDataObject.class, "lastKnownName");

    public static DataKey<Instant, IUserDataObject> LAST_LOGIN = DataKey.of(TypeTokens.INSTANT, IUserDataObject.class, "lastLogin");

    public static DataKey<Instant, IUserDataObject> LAST_LOGOUT = DataKey.of(TypeTokens.INSTANT, IUserDataObject.class, "lastLogout");

    public static DataKey<String, IUserDataObject> IP_ADDRESS = DataKey.of(TypeTokens.STRING, IUserDataObject.class, "lastIP");

    @Deprecated
    public static DataKey<Instant, IUserDataObject> FIRST_JOIN = DataKey.of(TypeTokens.INSTANT, IUserDataObject.class, "firstJoin");

}
