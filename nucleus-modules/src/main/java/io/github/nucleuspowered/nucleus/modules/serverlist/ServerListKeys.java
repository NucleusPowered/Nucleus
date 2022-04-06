/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;

import java.time.Instant;

public final class ServerListKeys {

    public static DataKey<String, IGeneralDataObject> LINE_ONE = DataKey.of(
            null,
            TypeTokens.STRING,
            IGeneralDataObject.class,
            "lineone");

    public static DataKey<String, IGeneralDataObject> LINE_TWO = DataKey.of(
            null,
            TypeTokens.STRING,
            IGeneralDataObject.class,
            "linetwo");

    public static DataKey<Instant, IGeneralDataObject> EXPIRY = DataKey.of(
            null,
            TypeTokens.INSTANT,
            IGeneralDataObject.class,
            "expiry");
}
