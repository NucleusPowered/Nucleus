/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandlogger;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.commandlogger.config.CommandLoggerConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.commandlogger.handlers.CommandLoggerHandler;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "command-logger", name = "Command Logger")
public class CommandLoggerModule extends ConfigurableModule<CommandLoggerConfigAdapter> {

    @Override
    public CommandLoggerConfigAdapter createAdapter() {
        return new CommandLoggerConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        CommandLoggerHandler clh = new CommandLoggerHandler(plugin, plugin.getInjector().getInstance(CommandLoggerConfigAdapter.class), plugin.getInjector().getInstance(CoreConfigAdapter.class));
        serviceManager.registerService(CommandLoggerHandler.class, clh);
        plugin.registerReloadable(clh::onReload);
        clh.onReload();
    }
}
