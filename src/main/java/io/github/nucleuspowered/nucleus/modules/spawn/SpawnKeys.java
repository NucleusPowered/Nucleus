/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.internal.TypeTokens;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IWorldDataObject;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

public class SpawnKeys {

    public static DataKey<LocationNode, IGeneralDataObject> FIRST_SPAWN_LOCATION = DataKey.of(
            TypeTokens.LOCATION_NODE,
            IGeneralDataObject.class,
            "firstSpawn");

    public static DataKey<Vector3d, IWorldDataObject> WORLD_SPAWN_ROTATION = DataKey.of(
            TypeTokens.VECTOR_3D,
            IWorldDataObject.class,
            "s[pawn-rotation");
}
