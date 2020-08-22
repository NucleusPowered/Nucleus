/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chat.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

import java.util.List;

public class ChatConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<ChatConfig> {

    public ChatConfigAdapter() {
        super(ChatConfig.class);
    }

    @Override
    protected List<Transformation> getTransformations() {
        return Lists.newArrayList(
                Transformation.moveFrom("use-group-templates").to("templates", "use-group-templates"));
    }
}
