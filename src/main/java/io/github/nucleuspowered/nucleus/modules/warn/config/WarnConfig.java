/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class WarnConfig {

    @Setting(value = "show-login", comment = "loc:config.warn.showonlogin")
    private boolean showOnLogin = true;

    @Setting(value = "expire-warnings", comment = "loc:config.warn.expire")
    private boolean expireWarnings = true;

    @Setting(value = "minimum-warn-length", comment = "loc:config.warn.minwarnlength")
    private long minWarnLength = -1;

    @Setting(value = "maximum-warn-length", comment = "loc:config.warn.maxwarnlength")
    private long maxWarnLength = -1;

    @Setting(value = "default-length", comment = "loc:config.warn.defaultlength")
    private long defaultLength = -1;

    public boolean isShowOnLogin() {
        return showOnLogin;
    }

    public boolean isExpireWarnings() {
        return expireWarnings;
    }

    public long getMinimumWarnLength() {
        return minWarnLength;
    }

    public long getMaximumWarnLength() {
        return maxWarnLength;
    }

    public long getDefaultLength() {
        return defaultLength;
    }
}
