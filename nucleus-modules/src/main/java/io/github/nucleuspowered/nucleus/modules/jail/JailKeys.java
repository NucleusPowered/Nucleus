/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail;

import io.github.nucleuspowered.nucleus.api.module.jail.data.Jail;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.modules.jail.data.NucleusJailing;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;
import io.github.nucleuspowered.nucleus.modules.jail.services.NucleusJail;
import io.leangen.geantyref.TypeToken;

public final class JailKeys {

    public static final TypeToken<NucleusJailing> JAIL_DATA_KEY = TypeToken.get(NucleusJailing.class);

    public static final DataKey<NucleusJailing, IUserDataObject> JAIL_DATA =
            DataKey.of(JailKeys.JAIL_DATA_KEY, IUserDataObject.class, "jailData");

    public static final DataKey.MapKey<String, Jail, IGeneralDataObject> JAILS =
            DataKey.ofMap(TypeTokens.STRING, TypeTokens.JAIL, IGeneralDataObject.class, "jails");
}
