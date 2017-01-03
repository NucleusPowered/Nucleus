/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.protection;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.protection.config.ProtectionConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = ProtectionModule.ID, name = "Protection")
public class ProtectionModule extends ConfigurableModule<ProtectionConfigAdapter> {

    public static final String ID = "protection";

    @Override
    public ProtectionConfigAdapter createAdapter() {
        return new ProtectionConfigAdapter();
    }
}
