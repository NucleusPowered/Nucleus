/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarpService;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.api.spongedata.NucleusKeys;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.ImmutableWarpSignData;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.WarpSignData;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warp.handlers.WarpHandler;
import io.github.nucleuspowered.nucleus.modules.warp.spongedata.manipulators.WarpDataManipulatorBuilder;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.data.value.mutable.Value;
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

            // Data!
            NucleusKeys.class.getDeclaredField("WARP_NAME").set(null, KeyFactory.makeSingleKey(String.class, Value.class, DataQuery.of("nucleus", "warp", "name")));
            NucleusKeys.class.getDeclaredField("WARP_PERMISSION").set(null, KeyFactory.makeOptionalKey(String.class, DataQuery.of("nucleus", "warp", "permission")));
            NucleusKeys.class.getDeclaredField("WARP_WARMUP").set(null, KeyFactory.makeSingleKey(String.class, MutableBoundedValue.class, DataQuery.of("nucleus", "warp", "warmup")));

            Sponge.getDataManager().register(WarpSignData.class, ImmutableWarpSignData.class, new WarpDataManipulatorBuilder());
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
