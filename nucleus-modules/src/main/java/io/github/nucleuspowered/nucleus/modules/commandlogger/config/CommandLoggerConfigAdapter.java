/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandlogger.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class CommandLoggerConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<CommandLoggerConfig> {

    public CommandLoggerConfigAdapter() {
        super(CommandLoggerConfig.class);
    }
}
