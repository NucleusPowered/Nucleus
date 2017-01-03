/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo;

import io.github.nucleuspowered.nucleus.api.service.NucleusSeenService;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.playerinfo.config.PlayerInfoConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.playerinfo.handlers.SeenHandler;
import org.spongepowered.api.Sponge;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = PlayerInfoModule.ID, name = "Player Info")
public class PlayerInfoModule extends ConfigurableModule<PlayerInfoConfigAdapter> {

    public static final String ID = "playerinfo";

    @Override
    public PlayerInfoConfigAdapter createAdapter() {
        return new PlayerInfoConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        SeenHandler sh = new SeenHandler();
        Sponge.getServiceManager().setProvider(plugin, NucleusSeenService.class, sh);
        plugin.getInternalServiceManager().registerService(SeenHandler.class, sh);
    }
}
