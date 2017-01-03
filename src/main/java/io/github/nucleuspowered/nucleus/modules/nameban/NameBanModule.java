/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nameban;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.nameban.config.NameBanConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "nameban", name = "Name Banning")
public class NameBanModule extends ConfigurableModule<NameBanConfigAdapter> {

    @Override public NameBanConfigAdapter createAdapter() {
        return new NameBanConfigAdapter();
    }
}
