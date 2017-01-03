/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.world.config.WorldConfigAdapter;
import org.slf4j.Logger;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "world", name = "World")
public class WorldModule extends ConfigurableModule<WorldConfigAdapter> {

    @Inject private Logger logger;

    @Override public WorldConfigAdapter createAdapter() {
        return new WorldConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        try {
            WorldHelper wh = new WorldHelper();
            plugin.getInjector().injectMembers(wh);
            serviceManager.registerService(WorldHelper.class, wh);
        } catch (Exception ex) {
            logger.warn("Could not load the world module for the reason below.");
            ex.printStackTrace();
            throw ex;
        }
    }
}
