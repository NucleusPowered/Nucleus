/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarnService;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.warn.config.WarnConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warn.handlers.WarnHandler;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "warn", name = "Warn")
public class WarnModule extends ConfigurableModule<WarnConfigAdapter> {

    @Inject private Game game;
    @Inject private Logger logger;

    @Override
    public WarnConfigAdapter getAdapter() {
        return new WarnConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        try {
            WarnHandler warnHandler = new WarnHandler(nucleus);
            nucleus.getInjector().injectMembers(warnHandler);
            game.getServiceManager().setProvider(nucleus, NucleusWarnService.class, warnHandler);
            serviceManager.registerService(WarnHandler.class, warnHandler);
        } catch (Exception ex) {
            logger.warn("Could not load the mute module for the reason below.");
            ex.printStackTrace();
            throw ex;
        }
    }
}
