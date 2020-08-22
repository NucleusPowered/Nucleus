/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nameban;

import io.github.nucleuspowered.nucleus.modules.nameban.config.NameBanConfig;
import io.github.nucleuspowered.nucleus.modules.nameban.config.NameBanConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = "nameban", name = "Name Banning")
public class NameBanModule extends ConfigurableModule<NameBanConfig, NameBanConfigAdapter> {

    @Inject
    public NameBanModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override public NameBanConfigAdapter createAdapter() {
        return new NameBanConfigAdapter();
    }

}
