/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn;

import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = SpawnModule.ID, name = "Spawn")
public class SpawnModule extends ConfigurableModule<SpawnConfig, SpawnConfigAdapter> {

    public static final String ID = "spawn";

    @Inject
    public SpawnModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder,
            INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public SpawnConfigAdapter createAdapter() {
        return new SpawnConfigAdapter();
    }

}
