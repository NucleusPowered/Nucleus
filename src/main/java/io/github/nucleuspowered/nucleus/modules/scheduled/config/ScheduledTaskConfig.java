/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.scheduled.config;

import io.github.nucleuspowered.nucleus.util.DelayedTimeValue;
import io.github.nucleuspowered.nucleus.util.WarningTimeList;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import javax.annotation.Nullable;

@ConfigSerializable
public class ScheduledTaskConfig {

    @Setting(value = "use-warning", comment = "loc:config.scheduled.task.usewarning")
    private boolean useWarning = false;

    @Setting(value = "warning-time-list", comment = "loc:config.scheduled.task.warningtimelist")
    private WarningTimeList warningTimeList = new WarningTimeList();

    @Setting(value = "delayed-time-value", comment = "loc:config.scheduled.task.delayedtimevalue")
    private DelayedTimeValue delayedTimeValue = null;

    public boolean useWarning() {
        return this.useWarning;
    }

    public WarningTimeList getWarnings() {
        return this.warningTimeList;
    }

    @Nullable
    public DelayedTimeValue getDelayedTimeValue() {
        return this.delayedTimeValue;
    }
}
