/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

import java.util.List;

public class InfoConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<InfoConfig> {

    public InfoConfigAdapter() {
        super(InfoConfig.class);
    }

    @Override protected List<Transformation> getTransformations() {
        return Lists.newArrayList(
            new Transformation(new Object[] { "show-motd-on-join" }, (inputPath, valueAtPath) -> new Object[] { "motd", "show-motd-on-join" }),
            new Transformation(new Object[] { "motd-title" }, (inputPath, valueAtPath) -> new Object[] { "motd", "motd-title" }),
            new Transformation(new Object[] { "show-motd-on-join" }, (inputPath, valueAtPath) -> new Object[] { "motd", "use-pagination" })
        );
    }
}
