/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

import java.util.List;

public class BackConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<BackConfig> {

    @Override protected List<Transformation> getTransformations() {
        List<Transformation> transformations = super.getTransformations();
        transformations.add(new Transformation(new Object[] { "onTeleport" }, (inputPath, valueAtPath) -> new Object[] { "on-teleport" }));
        transformations.add(new Transformation(new Object[] { "onDeath" }, (inputPath, valueAtPath) -> new Object[] { "on-death" }));
        transformations.add(new Transformation(new Object[] { "onPortal" }, (inputPath, valueAtPath) -> new Object[] { "on-portal" }));
        return transformations;
    }

    public BackConfigAdapter() {
        super(BackConfig.class);
    }
}
