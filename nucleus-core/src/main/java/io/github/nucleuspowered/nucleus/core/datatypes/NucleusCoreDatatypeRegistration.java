/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.datatypes;

import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.api.util.data.TimedEntry;
import org.spongepowered.api.data.DataManager;

public final class NucleusCoreDatatypeRegistration {

    public static void registerDataTypes(final DataManager dataManager) {
        dataManager.registerBuilder(NamedLocation.class, new NucleusNamedLocation.DataBuilder());
        dataManager.registerBuilder(TimedEntry.class, new NucleusTimedEntry.DataBuilder());
    }

}
