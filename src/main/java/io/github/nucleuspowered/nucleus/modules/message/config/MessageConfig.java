/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.config;

import io.github.nucleuspowered.neutrino.annotations.Default;
import io.github.nucleuspowered.nucleus.internal.text.NucleusTextTemplateImpl;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class MessageConfig {

    @Setting(value = "helpop-prefix", comment = "config.message.helpop.prefix")
    @Default(value = "&7HelpOp: {{name}} &7> &r", saveDefaultIfNull = true)
    private NucleusTextTemplateImpl helpOpPrefix;

    @Setting(value = "msg-receiver-prefix", comment = "config.message.receiver.prefix")
    @Default(value = "&7[{{fromDisplay}}&7 -> me]: &r", saveDefaultIfNull = true)
    private NucleusTextTemplateImpl messageReceiverPrefix;

    @Setting(value = "msg-sender-prefix", comment = "config.message.sender.prefix")
    @Default(value = "&7[me -> {{toDisplay}}&7]: &r", saveDefaultIfNull = true)
    private NucleusTextTemplateImpl messageSenderPrefix;

    @Setting(value = "socialspy")
    private SocialSpy socialSpy = new SocialSpy();

    public NucleusTextTemplateImpl getHelpOpPrefix() {
        return helpOpPrefix;
    }

    public NucleusTextTemplateImpl getMessageReceiverPrefix() {
        return messageReceiverPrefix;
    }

    public NucleusTextTemplateImpl getMessageSenderPrefix() {
        return messageSenderPrefix;
    }

    public NucleusTextTemplateImpl getMessageSocialSpyPrefix() {
        return socialSpy.messageSocialSpyPrefix;
    }

    public boolean isSocialSpyAllowForced() {
        return socialSpy.allowForced;
    }

    public boolean isSocialSpyLevels() {
        return socialSpy.socialSpyLevels;
    }

    public boolean isSocialSpySameLevel() {
        return socialSpy.socialSpySameLevel;
    }

    public int getCustomTargetLevel() {
        return socialSpy.level.customTargets;
    }

    public int getServerLevel() {
        return socialSpy.level.server;
    }

    public boolean isShowMessagesInSocialSpyWhileMuted() {
        return socialSpy.showMessagesInSocialSpyWhileMuted;
    }

    public String getMutedTag() {
        return socialSpy.mutedTag;
    }

    public Targets spyOn() {
        return socialSpy.targets;
    }

    @ConfigSerializable
    public static class SocialSpy {
        @Setting(value = "msg-prefix", comment = "config.message.socialspy.prefix")
        @Default(value = "&7[SocialSpy] [{{fromDisplay}}&7 -> {{toDisplay}}&7]: &r", saveDefaultIfNull = true)
        private NucleusTextTemplateImpl messageSocialSpyPrefix;

        @Setting(value = "allow-forced", comment = "config.message.socialspy.force")
        private boolean allowForced = false;

        @Setting(value = "use-levels", comment = "config.message.socialspy.levels")
        private boolean socialSpyLevels = false;

        @Setting(value = "same-levels-can-see-each-other", comment = "config.message.socialspy.samelevel")
        private boolean socialSpySameLevel = true;

        @Setting(value = "levels", comment = "config.message.socialspy.serverlevels")
        private Levels level = new Levels();

        @Setting(value = "show-cancelled-messages", comment = "config.message.socialspy.mutedshow")
        private boolean showMessagesInSocialSpyWhileMuted = false;

        @Setting(value = "cancelled-messages-tag", comment = "config.message.socialspy.mutedtag")
        private String mutedTag = "&c[cancelled] ";

        @Setting(value = "senders-to-spy-on", comment = "config.message.socialspy.spyon")
        private Targets targets = new Targets();
    }

    @ConfigSerializable
    public static class Levels {

        @Setting(value = "server", comment = "config.message.socialspy.serverlevel")
        private int server = Integer.MAX_VALUE;

        @Setting(value = "custom-targets", comment = "config.message.socialspy.customlevel")
        private int customTargets = Integer.MAX_VALUE;
    }

    @ConfigSerializable
    public static class Targets {

        @Setting
        private boolean player = true;

        @Setting
        private boolean server = true;

        @Setting(value = "custom-target")
        private boolean custom = true;

        public boolean isPlayer() {
            return player;
        }

        public boolean isServer() {
            return server;
        }

        public boolean isCustom() {
            return custom;
        }
    }
}
