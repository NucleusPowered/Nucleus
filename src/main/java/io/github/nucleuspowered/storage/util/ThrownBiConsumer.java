/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.util;

@FunctionalInterface
public interface ThrownBiConsumer<A, B, X extends Throwable> {

    void apply(A first, B second) throws X;
}
