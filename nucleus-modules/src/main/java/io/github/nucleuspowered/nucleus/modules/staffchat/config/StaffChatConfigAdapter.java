/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

import java.util.List;

public class StaffChatConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<StaffChatConfig> {

    @Override protected List<Transformation> getTransformations() {
        return Lists.newArrayList(
            new Transformation(new Object[] { "messageTemplate" }, (inputPath, valueAtPath) -> new Object[] { "message-template" }),
            new Transformation(new Object[] { "messageColour" }, (inputPath, valueAtPath) -> new Object[] { "message-colour" })
        );
    }

    public StaffChatConfigAdapter() {
        super(StaffChatConfig.class);
    }
}
