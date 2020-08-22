/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connectionmessages.config;

import io.github.nucleuspowered.neutrino.annotations.Default;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ConnectionMessagesConfig {

    @Setting(value = "show-first-login-message", comment = "config.connectionmessages.enablefirst")
    private boolean showFirstTimeMessage = true;

    @Setting(value = "first-login-message", comment = "config.connectionmessages.firsttime")
    @Default(value = "&dWelcome &f{{name}} &dto the server!",  saveDefaultIfNull = true)
    private NucleusTextTemplateImpl firstTimeMessage;

    @Setting(value = "modify-login-message", comment = "config.connectionmessages.enablelogin")
    private boolean modifyLoginMessage = false;

    @Setting(value = "modify-logout-message", comment = "config.connectionmessages.enablelogout")
    private boolean modifyLogoutMessage = false;

    @Setting(value = "login-message", comment = "config.connectionmessages.loginmessage")
    @Default(value = "&8[&a+&8] &f{{name}}", saveDefaultIfNull = true)
    private NucleusTextTemplateImpl loginMessage;

    @Setting(value = "logout-message", comment = "config.connectionmessages.logoutmessage")
    @Default(value = "&8[&c-&8] &f{{name}}", saveDefaultIfNull = true)
    private NucleusTextTemplateImpl logoutMessage;

    @Setting(value = "disable-with-permission", comment = "config.connectionmessages.disablepermission")
    private boolean disableWithPermission = false;

    @Setting(value = "display-name-change-if-changed", comment = "config.connectionmessages.displayprior")
    private boolean displayPriorName = true;

    @Setting(value = "changed-name-message", comment = "config.connectionmessages.displaypriormessage")
    @Default(value = "&f{{name}} &ewas previously known by a different name - they were known as &f{{previousname}}", saveDefaultIfNull = true)
    private NucleusTextTemplateImpl priorNameMessage;

    @Setting(value = "force-show-all-connection-messages", comment = "config.connectionmessages.showall")
    private boolean forceForAll = true;

    public boolean isShowFirstTimeMessage() {
        return this.showFirstTimeMessage;
    }

    public NucleusTextTemplateImpl getFirstTimeMessage() {
        return this.firstTimeMessage;
    }

    public boolean isModifyLoginMessage() {
        return this.modifyLoginMessage;
    }

    public boolean isModifyLogoutMessage() {
        return this.modifyLogoutMessage;
    }

    public NucleusTextTemplateImpl getLoginMessage() {
        return this.loginMessage;
    }

    public NucleusTextTemplateImpl getLogoutMessage() {
        return this.logoutMessage;
    }

    public boolean isDisableWithPermission() {
        return this.disableWithPermission;
    }

    public boolean isDisplayPriorName() {
        return this.displayPriorName;
    }

    public NucleusTextTemplateImpl getPriorNameMessage() {
        return this.priorNameMessage;
    }

    public boolean isForceForAll() {
        return this.forceForAll;
    }
}
