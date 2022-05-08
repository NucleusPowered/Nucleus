/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.util;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.math.vector.Vector3d;

public interface WorldTransform {

    Vector3d getPosition();

    Vector3d getRotation();

    ResourceKey getWorldResourceKey();
}
