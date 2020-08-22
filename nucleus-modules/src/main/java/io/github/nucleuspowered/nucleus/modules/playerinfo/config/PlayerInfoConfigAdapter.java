/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

import java.util.List;

public class PlayerInfoConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<PlayerInfoConfig> {

    @Override protected List<Transformation> getTransformations() {
        return Lists.newArrayList(
            Transformation.moveFrom("list", "multicraft-compatibility").to("list", "server-panel-compatibility")
        );
    }

    public PlayerInfoConfigAdapter() {
        super(PlayerInfoConfig.class);
    }
}
