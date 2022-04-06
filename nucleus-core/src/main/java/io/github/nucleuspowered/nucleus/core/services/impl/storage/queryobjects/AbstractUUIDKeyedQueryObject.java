/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.queryobjects;

import io.github.nucleuspowered.storage.query.IQueryObject;

import java.util.UUID;

public abstract class AbstractUUIDKeyedQueryObject<T extends IQueryObject<UUID, T>>
        extends AbstractQueryObject<UUID, T> {

    @Override
    public final Class<UUID> keyType() {
        return UUID.class;
    }

}
