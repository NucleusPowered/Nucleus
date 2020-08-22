/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chatlogger.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ChatLoggingConfig {

    @Setting(value = "enable-logging", comment = "config.chatlog.enable")
    private boolean enableLog = false;

    @Setting(value = "log-chat", comment = "config.chatlog.chat")
    private boolean logChat = true;

    @Setting(value = "log-messages", comment = "config.chatlog.message")
    private boolean logMessages = true;

    @Setting(value = "log-mail", comment = "config.chatlog.mail")
    private boolean logMail = false;

    public boolean isEnableLog() {
        return this.enableLog;
    }

    public boolean isLogChat() {
        return this.logChat;
    }

    public boolean isLogMessages() {
        return this.logMessages;
    }

    public boolean isLogMail() {
        return this.logMail;
    }
}
