/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.util;

public interface ThrownConsumer<R, X extends Exception> {

    void save(R r) throws X;

}
