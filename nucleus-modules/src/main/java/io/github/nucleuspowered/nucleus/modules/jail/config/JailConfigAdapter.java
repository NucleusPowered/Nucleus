/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

import java.util.List;

public class JailConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<JailConfig> {

    public JailConfigAdapter() {
        super(JailConfig.class);
    }

    @Override
    protected List<Transformation> getTransformations() {
        return Lists.newArrayList(
            new Transformation(new Object[] { "allowedCommands" }, ((inputPath, valueAtPath) -> new Object[] { "allowed-commands" }))
        );
    }
}
