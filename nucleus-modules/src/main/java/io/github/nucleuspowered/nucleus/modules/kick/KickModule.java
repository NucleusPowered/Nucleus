/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kick;

import io.github.nucleuspowered.nucleus.modules.kick.config.KickConfig;
import io.github.nucleuspowered.nucleus.modules.kick.config.KickConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

@ModuleData(id = "kick", name = "Kick")
public class KickModule extends ConfigurableModule<KickConfig, KickConfigAdapter> {

    public KickModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override public KickConfigAdapter createAdapter() {
        return new KickConfigAdapter();
    }
}
