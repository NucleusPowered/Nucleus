/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core;

import io.github.nucleuspowered.nucleus.api.service.NucleusMessageTokenService;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.internal.text.NucleusTokenServiceImpl;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import org.spongepowered.api.Sponge;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "core", name = "Core", isRequired = true)
public class CoreModule extends ConfigurableModule<CoreConfigAdapter> {

    @Override
    public CoreConfigAdapter createAdapter() {
        return new CoreConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        plugin.reloadMessages();
    }

    @Override public void onEnable() {
        super.onEnable();

        NucleusTokenServiceImpl nucleusChatService = new NucleusTokenServiceImpl();
        plugin.getTokenHandler().register(nucleusChatService);
        serviceManager.registerService(NucleusTokenServiceImpl.class, nucleusChatService);
        Sponge.getServiceManager().setProvider(plugin, NucleusMessageTokenService.class, nucleusChatService);
    }
}
