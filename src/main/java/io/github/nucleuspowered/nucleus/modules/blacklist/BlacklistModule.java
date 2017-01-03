/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.blacklist;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.blacklist.config.BlacklistConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = BlacklistModule.ID, name = "Blacklist")
public class BlacklistModule extends ConfigurableModule<BlacklistConfigAdapter> {

    public final static String ID = "blacklist";

    @Override
    public BlacklistConfigAdapter createAdapter() {
        return new BlacklistConfigAdapter();
    }
}
