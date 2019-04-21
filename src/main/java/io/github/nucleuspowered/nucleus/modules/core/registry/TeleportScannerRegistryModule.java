/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.registry;

import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanner;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.internal.annotations.Registry;
import io.github.nucleuspowered.nucleus.internal.registry.NucleusRegistryModule;
import io.github.nucleuspowered.nucleus.modules.core.teleport.scanners.NoTeleportScanner;
import io.github.nucleuspowered.nucleus.modules.core.teleport.scanners.VerticalTeleportScanner;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

@NonnullByDefault
@Registry(TeleportScanners.class)
public class TeleportScannerRegistryModule extends NucleusRegistryModule<TeleportScanner> {

    @Override
    public Class<TeleportScanner> catalogClass() {
        return TeleportScanner.class;
    }

    @Override
    public void registerModuleDefaults() {
        registerAdditionalCatalog(new NoTeleportScanner());
        registerAdditionalCatalog(new VerticalTeleportScanner.Ascending());
        registerAdditionalCatalog(new VerticalTeleportScanner.Descending());
    }
}
