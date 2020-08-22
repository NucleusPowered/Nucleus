/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandspy.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

import java.util.List;

public class CommandSpyConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<CommandSpyConfig> {

    public CommandSpyConfigAdapter() {
        super(CommandSpyConfig.class);
    }

    @Override
    protected List<Transformation> getTransformations() {
        return Lists.newArrayList(
            new Transformation(new Object[] { "use-whitelist" }, (inputPath, valueAtPath) -> new Object[] { "filter-is-whitelist" }),
            new Transformation(new Object[] { "whitelisted-commands-to-spy-on" }, (inputPath, valueAtPath) -> new Object[] { "command-filter" })
        );
    }
}
