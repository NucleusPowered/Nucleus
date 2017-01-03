/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.info.config.InfoConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.info.handlers.InfoHandler;
import org.spongepowered.api.Sponge;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = InfoModule.ID, name = "Info")
public class InfoModule extends ConfigurableModule<InfoConfigAdapter> {

    public static final String ID = "info";
    public static final String MOTD_KEY = "motd";

    @Override
    public InfoConfigAdapter createAdapter() {
        return new InfoConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        plugin.addTextFileController(
                MOTD_KEY,
                Sponge.getAssetManager().getAsset(plugin, "motd.txt").get(),
                plugin.getConfigDirPath().resolve("motd.txt"));

        InfoHandler ih = new InfoHandler(plugin);
        serviceManager.registerService(InfoHandler.class, ih);
        plugin.registerReloadable(ih::onReload);
        ih.onReload();
    }
}
