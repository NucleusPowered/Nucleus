/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.item.config.ItemConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "item", name = "Item")
public class ItemModule extends ConfigurableModule<ItemConfigAdapter> {

    @Override public ItemConfigAdapter createAdapter() {
        return new ItemConfigAdapter();
    }
}
