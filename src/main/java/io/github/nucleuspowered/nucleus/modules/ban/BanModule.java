/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.ban.config.BanConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "ban", name = "Bans")
public class BanModule extends ConfigurableModule<BanConfigAdapter> {

    @Override
    public BanConfigAdapter getAdapter() {
        return new BanConfigAdapter();
    }
}
