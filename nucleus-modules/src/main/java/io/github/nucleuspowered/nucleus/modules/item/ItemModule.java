/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item;

import io.github.nucleuspowered.nucleus.modules.item.config.ItemConfig;
import io.github.nucleuspowered.nucleus.modules.item.config.ItemConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = "item", name = "Item")
public class ItemModule extends ConfigurableModule<ItemConfig, ItemConfigAdapter> {

    @Inject
    public ItemModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override public ItemConfigAdapter createAdapter() {
        return new ItemConfigAdapter();
    }
}
