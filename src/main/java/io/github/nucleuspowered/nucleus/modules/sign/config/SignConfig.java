/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.sign.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class SignConfig {

    @Setting("coloured-signs")
    private boolean colouredSigns = true;

    @Setting("action-signs")
    private boolean actionSigns = true;

    public boolean isColouredSigns() {
        return colouredSigns;
    }

    public boolean isActionSigns() {
        return actionSigns;
    }
}
