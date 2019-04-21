/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.registry;

import io.github.nucleuspowered.nucleus.api.teleport.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.internal.annotations.Registry;
import io.github.nucleuspowered.nucleus.internal.registry.NucleusRegistryModule;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

@NonnullByDefault
@Registry(TeleportScanners.class)
public class TeleportResultRegistryModule extends NucleusRegistryModule<TeleportResult> {

    @Override
    protected boolean allowsAdditional() {
        return false;
    }

    @Override
    public Class<TeleportResult> catalogClass() {
        return TeleportResult.class;
    }

    @Override
    public void registerModuleDefaults() {
        registerAdditionalCatalog(new TeleportResultImpl(
                "nucleus:success",
                "Successful Teleport",
                true
        ));
        registerAdditionalCatalog(new TeleportResultImpl(
                "nucleus:fail_no_location",
                "Failed Teleport - no location found",
                false
        ));
        registerAdditionalCatalog(new TeleportResultImpl(
                "nucleus:fail_cancelled",
                "Failed Teleport - cancelled by plugin",
                false
        ));
    }

    public static class TeleportResultImpl implements TeleportResult {

        private final String id;
        private final String name;
        private final boolean successful;

        TeleportResultImpl(String id, String name, boolean successful) {
            this.id = id;
            this.name = name;
            this.successful = successful;
        }

        @Override
        public boolean isSuccessful() {
            return this.successful;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }
}
