/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

import java.util.List;

public class WorldConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<WorldConfig> {

    @Override protected List<Transformation> getTransformations() {
        return Lists.newArrayList(
                new Transformation(new Object[] { "display-generation-warning" },
                        ((inputPath, valueAtPath) -> new Object[] { "pre-generation", "display-generation-warning" }))
        );
    }

    public WorldConfigAdapter() {
        super(WorldConfig.class);
    }
}
