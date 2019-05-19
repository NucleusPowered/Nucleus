/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.util;

public interface ThrownSupplier<I, X extends Throwable> {

    I get() throws X;

}
