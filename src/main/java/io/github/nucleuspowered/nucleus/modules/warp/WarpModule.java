/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarpService;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warp.handlers.WarpHandler;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "warp", name = "Warp")
public class WarpModule extends ConfigurableModule<WarpConfigAdapter> {

    @Inject private Game game;
    @Inject private Logger logger;

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        try {
            // Put the warp service into the service manager.
            WarpHandler wh = new WarpHandler();
            plugin.getInjector().injectMembers(wh);
            serviceManager.registerService(WarpHandler.class, wh);
            game.getServiceManager().setProvider(plugin, NucleusWarpService.class, wh);
        } catch (Exception ex) {
            logger.warn("Could not load the warp module for the reason below.");
            ex.printStackTrace();
            throw ex;
        }
    }

    @Override
    public WarpConfigAdapter getAdapter() {
        return new WarpConfigAdapter();
    }
}
