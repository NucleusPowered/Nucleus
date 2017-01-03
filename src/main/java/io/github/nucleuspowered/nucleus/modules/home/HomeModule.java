/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = HomeModule.ID, name = "Home")
public class HomeModule extends ConfigurableModule<HomeConfigAdapter> {

    public static final String ID = "home";
    public static final String DEFAULT_HOME_NAME = "home";

    @Override
    public HomeConfigAdapter createAdapter() {
        return new HomeConfigAdapter();
    }
}
