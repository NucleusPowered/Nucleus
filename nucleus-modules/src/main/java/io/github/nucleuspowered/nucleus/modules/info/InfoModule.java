/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info;

import io.github.nucleuspowered.nucleus.io.TextFileController;
import io.github.nucleuspowered.nucleus.modules.info.config.InfoConfig;
import io.github.nucleuspowered.nucleus.modules.info.config.InfoConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = InfoModule.ID, name = "Info")
public class InfoModule extends ConfigurableModule<InfoConfig, InfoConfigAdapter> {

    public static final String ID = "info";
    public static final String MOTD_KEY = "motd";

    @Inject
    public InfoModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public InfoConfigAdapter createAdapter() {
        return new InfoConfigAdapter();
    }

    @Override public void performPreTasks(INucleusServiceCollection serviceCollection) throws Exception {
        super.performPreTasks(serviceCollection);

        serviceCollection.textFileControllerCollection()
                .register(MOTD_KEY,
                new TextFileController(
                        serviceCollection.textTemplateFactory(),
                        Sponge.getAssetManager().getAsset(serviceCollection.pluginContainer(), "motd.txt").get(),
                        serviceCollection.configDir().resolve("motd.txt")));
    }
}
