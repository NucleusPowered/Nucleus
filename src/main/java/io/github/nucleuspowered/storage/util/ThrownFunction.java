/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.util;

public interface ThrownFunction<A, R, X extends Exception> {

    R apply(A a) throws X;
}
