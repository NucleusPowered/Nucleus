/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core;

import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.internal.TypeTokens;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;
import io.github.nucleuspowered.storage.dataobjects.keyed.IKeyedDataObject;

import java.time.Instant;

public class CoreKeys {

    public static DataKey<Integer, IKeyedDataObject> VERSION = DataKey.of(TypeTokens.INTEGER, IKeyedDataObject.class, "version");

    public static DataKey<String, IUserDataObject> LAST_KNOWN_NAME = DataKey.of(TypeTokens.STRING, IUserDataObject.class, "lastKnownName");

    @Deprecated
    public static DataKey<LocationNode, IUserDataObject> LOCATION_ON_LOGIN =
            DataKey.of(TypeTokens.LOCATION_NODE, IUserDataObject.class, "locationOnLogin");

    @Deprecated
    public static DataKey<LocationNode, IUserDataObject> LAST_LOCATION = DataKey.of(TypeTokens.LOCATION_NODE, IUserDataObject.class, "lastLocation");

    public static DataKey<Instant, IUserDataObject> LAST_LOGIN = DataKey.of(TypeTokens.INSTANT, IUserDataObject.class, "lastLogin");

    public static DataKey<Instant, IUserDataObject> LAST_LOGOUT = DataKey.of(TypeTokens.INSTANT, IUserDataObject.class, "lastLogout");

    public static DataKey<String, IUserDataObject> IP_ADDRESS = DataKey.of(TypeTokens.STRING, IUserDataObject.class, "lastIP");

    @Deprecated
    public static DataKey<Instant, IUserDataObject> FIRST_JOIN = DataKey.of(TypeTokens.INSTANT, IUserDataObject.class, "firstJoin");

    public static DataKey<Boolean, IUserDataObject> STARTED_FIRST_JOIN =
            DataKey.of(false, TypeTokens.BOOLEAN, IUserDataObject.class, "startedFirstJoin");
}
