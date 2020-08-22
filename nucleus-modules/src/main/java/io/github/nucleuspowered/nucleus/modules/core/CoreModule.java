/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

@ModuleData(id = CoreModule.ID, name = "Core", isRequired = true)
public class CoreModule extends ConfigurableModule<CoreConfig, CoreConfigAdapter> {

    public static final String ID = "core";

    @Inject
    public CoreModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder,
            INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public CoreConfigAdapter createAdapter() {
        return new CoreConfigAdapter();
    }

    @Override public void performPreTasks(INucleusServiceCollection serviceCollection) throws Exception {
        super.performPreTasks(serviceCollection);
        this.serviceCollection.messageProvider().reloadMessageFile();
    }

    @Override public void performPostTasks(INucleusServiceCollection serviceCollection) {
        super.performPostTasks(serviceCollection);
    }
}
