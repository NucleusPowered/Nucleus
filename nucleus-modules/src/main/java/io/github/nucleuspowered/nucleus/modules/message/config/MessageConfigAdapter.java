/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

import java.util.List;

public class MessageConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<MessageConfig> {

    @Override protected List<Transformation> getTransformations() {
        return Lists.newArrayList(
            // 0.18.0
            Transformation.moveTopLevel("msg-socialspy-prefix", "socialspy", "msg-prefix"),
            Transformation.moveTopLevel("socialspy-cancelled-messages", "socialspy", "show-cancelled-messages"),
            Transformation.moveTopLevel("socialspy-cancelled-tag", "socialspy", "cancelled-messages-tag"),
            Transformation.moveTopLevel("socialspy-only-players", "socialspy", "show-only-players"),

            // 0.25.0 -> 0.26.0
            Transformation.moveFrom("socialspy", "server-level").to("socialspy", "levels", "server"),
            new Transformation(new Object[] { "socialspy", "show-only-players" }, (inputPath, valueAtPath) -> {
                valueAtPath.setValue(!valueAtPath.getBoolean());
                return new Object[] { "socialspy", "senders-to-spy-on", "server" }; // now need to turn OFF server rather than turn ON players only.
            })
        );
    }

    public MessageConfigAdapter() {
        super(MessageConfig.class);
    }
}
