/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;
import ninja.leaping.configurate.ConfigurationNode;

import java.util.List;

public class RTPConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<RTPConfig> {

    @Override
    protected void manualTransform(ConfigurationNode node) {
        nodeTransform(node);
        node.getNode("world-overrides").getChildrenMap().forEach((key, value) -> nodeTransform(value));
    }

    private void nodeTransform(ConfigurationNode node) {
        if (node.getNode("default-method").isVirtual() && !node.getNode("center-on-player").isVirtual() && !node.getNode("surface-only").isVirtual()) {
            boolean centreOnPlayer = node.getNode("center-on-player").getBoolean(false);
            boolean surfaceonly = node.getNode("surface-only").getBoolean(false);

            if (centreOnPlayer) {
                if (surfaceonly) {
                    node.getNode("default-method").setValue("nucleus:around_player_surface");
                } else {
                    node.getNode("default-method").setValue("nucleus:around_player");
                }
            } else {
                if (surfaceonly) {
                    node.getNode("default-method").setValue("nucleus:surface_only");
                } else {
                    node.getNode("default-method").setValue("nucleus:default");
                }
            }
        }
    }

    @Override
    protected List<Transformation> getTransformations() {
        return Lists.newArrayList(
                new Transformation(new Object[] { "center-on-player" }, ((inputPath, valueAtPath) -> null)),
                new Transformation(new Object[] { "surface-only" }, ((inputPath, valueAtPath) -> null))
        );
    }

    public RTPConfigAdapter() {
        super(RTPConfig.class);
    }
}
