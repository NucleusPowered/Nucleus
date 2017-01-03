/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.admin.config.AdminConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "admin", name = "Admin")
public class AdminModule extends ConfigurableModule<AdminConfigAdapter> {

    @Override
    public AdminConfigAdapter createAdapter() {
        return new AdminConfigAdapter();
    }
}
