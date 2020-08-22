/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class PlayerInfoConfig {

    @Setting("list")
    private ListConfig list = new ListConfig();

    @Setting
    private SeenConfig seen = new SeenConfig();

    @Setting
    private NearConfig near = new NearConfig();

    public ListConfig getList() {
        return this.list;
    }

    public SeenConfig getSeen() {
        return this.seen;
    }

    public NearConfig getNear() {
        return this.near;
    }

}
