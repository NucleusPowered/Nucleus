/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class ItemConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<ItemConfig> {

    public ItemConfigAdapter() {
        super(ItemConfig.class);
    }
}
