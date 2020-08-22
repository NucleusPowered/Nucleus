/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp;

import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfig;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = "warp", name = "Warp")
public class WarpModule extends ConfigurableModule<WarpConfig, WarpConfigAdapter> {

    @Inject
    public WarpModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder,
            INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public WarpConfigAdapter createAdapter() {
        return new WarpConfigAdapter();
    }

}
