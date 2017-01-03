/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandspy;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.commandspy.config.CommandSpyConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = CommandSpyModule.ID, name = "Command Spy")
public class CommandSpyModule extends ConfigurableModule<CommandSpyConfigAdapter> {

    public final static String ID = "command-spy";

    @Override public CommandSpyConfigAdapter createAdapter() {
        return new CommandSpyConfigAdapter();
    }
}
