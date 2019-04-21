/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.teleport;

import org.spongepowered.api.util.generator.dummy.DummyObjectProvider;

public final class TeleportScanners {

    public static final TeleportScanner ASCENDING_SCAN = DummyObjectProvider.createFor(TeleportScanner.class, "ASCENDING_SCAN");

    public static final TeleportScanner DESCENDING_SCAN = DummyObjectProvider.createFor(TeleportScanner.class, "DESCENDING_SCAN");

    public static final TeleportScanner NO_SCAN = DummyObjectProvider.createFor(TeleportScanner.class, "NO_SCAN");
}
