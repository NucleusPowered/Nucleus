/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.config;

import com.google.common.collect.ImmutableList;
import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

import java.util.List;

public class AFKConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<AFKConfig> {

    @Override protected List<Transformation> getTransformations() {
        return ImmutableList.of(
                Transformation.moveTopLevel("afktime", "afk-time"),
                Transformation.moveTopLevel("afktimetokick", "afk-time-to-kick"),
                Transformation.moveTopLevel("afk-when-vanished", "broadcast-afk-when-vanished")
        );
    }

    public AFKConfigAdapter() {
        super(AFKConfig.class);
    }
}
