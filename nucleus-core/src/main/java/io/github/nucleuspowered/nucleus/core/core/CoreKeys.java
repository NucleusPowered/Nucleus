/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core;

import io.github.nucleuspowered.nucleus.api.core.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IWorldDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.core.services.impl.userprefs.PreferenceKeyImpl;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;

import java.time.Instant;
import java.util.Locale;

public final class CoreKeys {

    public static final NucleusUserPreferenceService.PreferenceKey<Locale> LOCALE_PREFERENCE_KEY = new PreferenceKeyImpl.LocaleKey(
            NucleusKeysProvider.PLAYER_LOCALE_KEY,
            Locale.UK,
            CorePermissions.BASE_NUCLEUSLANGUAGE,
            "userpref.player_locale",
            (serviceCollection, uuid, value) -> serviceCollection.messageProvider().invalidateLocaleCacheFor(uuid)
    );

    public static final DataKey<Integer, IGeneralDataObject> GENERAL_VERSION = DataKey.of(TypeTokens.INTEGER, IGeneralDataObject.class, "data_version");

    public static final DataKey<Integer, IWorldDataObject> WORLD_VERSION = DataKey.of(TypeTokens.INTEGER, IWorldDataObject.class, "data_version");

    public static final DataKey<Integer, IUserDataObject> USER_VERSION = DataKey.of(TypeTokens.INTEGER, IUserDataObject.class, "data_version");

    public static final DataKey<String, IUserDataObject> LAST_KNOWN_NAME = DataKey.of(TypeTokens.STRING, IUserDataObject.class, "lastKnownName");

    public static final DataKey<Instant, IUserDataObject> LAST_LOGIN = DataKey.of(TypeTokens.INSTANT, IUserDataObject.class, "lastLogin");

    public static final DataKey<Instant, IUserDataObject> LAST_LOGOUT = DataKey.of(TypeTokens.INSTANT, IUserDataObject.class, "lastLogout");

    public static final DataKey<String, IUserDataObject> IP_ADDRESS = DataKey.of(TypeTokens.STRING, IUserDataObject.class, "lastIP");

    public static final DataKey<Boolean, IUserDataObject> FIRST_JOIN_PROCESSED = DataKey.of(false, TypeTokens.BOOLEAN, IUserDataObject.class, "firstJoinProcessed");
}
