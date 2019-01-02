/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.util;

public interface ThrownSupplier<R, X extends Throwable> {

    R get() throws X;
}
