/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IWorldDataObject;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

public class WorldKeys {

    public static DataKey<Boolean, IWorldDataObject> WORLD_PREGEN_START = DataKey.of(
            false,
            TypeToken.of(Boolean.class),
            IWorldDataObject.class,
            "start-pregen");

    public static DataKey<Long, IWorldDataObject> WORLD_PREGEN_SAVE_FREQUENCY = DataKey.of(
            20L,
            TypeToken.of(Long.class),
            IWorldDataObject.class,
            "save-time");

    public static DataKey<Integer, IWorldDataObject> WORLD_PREGEN_TICK_PERCENT = DataKey.of(
            80,
            TypeToken.of(Integer.class),
            IWorldDataObject.class,
            "tick-percent");

    public static DataKey<Integer, IWorldDataObject> WORLD_PREGEN_TICK_FREQUENCY = DataKey.of(
            4,
            TypeToken.of(Integer.class),
            IWorldDataObject.class,
            "tick-freq");

    public static DataKey<Boolean, IWorldDataObject> WORLD_PREGEN_AGGRESSIVE = DataKey.of(
            false,
            TypeToken.of(Boolean.class),
            IWorldDataObject.class,
            "aggressive");
}
