/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.service.NucleusBackService;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.back.config.BackConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.back.handlers.BackHandler;
import org.spongepowered.api.Game;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "back", name = "Back")
public class BackModule extends ConfigurableModule<BackConfigAdapter> {

    @Inject private Game game;

    @Override
    public BackConfigAdapter createAdapter() {
        return new BackConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        BackHandler m = new BackHandler();
        plugin.getInjector().injectMembers(m);
        serviceManager.registerService(BackHandler.class, m);
        game.getServiceManager().setProvider(plugin, NucleusBackService.class, m);
    }
}
