/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.teleport.data;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.registry.DefaultedRegistryType;
import org.spongepowered.api.registry.RegistryRoots;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.api.registry.RegistryTypes;

import java.util.function.Supplier;

public final class TeleportScanners {

    public static final ResourceKey REGISTRY_KEY = ResourceKey.of("nucleus", "teleport_scanner");

    public static final DefaultedRegistryType<TeleportScanner> REGISTRY =
            RegistryType.of(RegistryRoots.SPONGE, TeleportScanners.REGISTRY_KEY).asDefaultedType(Sponge::game);

    public static final Supplier<TeleportScanner> ASCENDING_SCAN =
            TeleportScanners.REGISTRY.defaultReferenced(ResourceKey.of("nucleus", "ascending_scan"));

    public static final Supplier<TeleportScanner> DESCENDING_SCAN =
            TeleportScanners.REGISTRY.defaultReferenced(ResourceKey.of("nucleus", "descending_scan"));

    public static final Supplier<TeleportScanner> NO_SCAN =
            TeleportScanners.REGISTRY.defaultReferenced(ResourceKey.of("nucleus", "no_scan"));
}
