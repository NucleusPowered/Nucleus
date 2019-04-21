/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.service.NucleusHomeService;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.home.services.HomeService;
import org.spongepowered.api.Sponge;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = HomeModule.ID, name = "Home")
public class HomeModule extends ConfigurableModule<HomeConfigAdapter> {

    public static final String ID = "home";

    @Override
    public HomeConfigAdapter createAdapter() {
        return new HomeConfigAdapter();
    }

    @Override protected void performPreTasks() throws Exception {
        super.performPreTasks();

        HomeService homeService = new HomeService();
        Nucleus.getNucleus().getInternalServiceManager().registerService(HomeService.class, homeService);
        Sponge.getServiceManager().setProvider(Nucleus.getNucleus(), NucleusHomeService.class, homeService);
    }
}
