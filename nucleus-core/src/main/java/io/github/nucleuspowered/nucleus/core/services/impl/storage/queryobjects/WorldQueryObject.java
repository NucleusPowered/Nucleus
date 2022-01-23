/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.queryobjects;

import io.github.nucleuspowered.storage.query.IWorldQueryObject;
import org.spongepowered.api.ResourceKey;

public class WorldQueryObject extends AbstractQueryObject<ResourceKey, IWorldQueryObject> implements IWorldQueryObject {

    @Override
    public Class<ResourceKey> keyType() {
        return ResourceKey.class;
    }
}
