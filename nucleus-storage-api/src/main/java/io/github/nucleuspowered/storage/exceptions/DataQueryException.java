/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.exceptions;

import io.github.nucleuspowered.storage.query.IQueryObject;

public class DataQueryException extends Exception {

    public DataQueryException(final String message, final IQueryObject<?, ?> queryObject) {
        super(message + " - " + queryObject.toString());
    }
}
