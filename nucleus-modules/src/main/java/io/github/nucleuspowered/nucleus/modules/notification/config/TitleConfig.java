/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.notification.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class TitleConfig {

    @Setting("fade-in")
    private double fadeIn = 1;

    @Setting("fade-out")
    private double fadeOut = 1;

    @Setting("time-on-screen")
    private double time = 5;

    public double getFadeIn() {
        return this.fadeIn;
    }

    public double getFadeOut() {
        return this.fadeOut;
    }

    public double getTime() {
        return this.time;
    }
}
