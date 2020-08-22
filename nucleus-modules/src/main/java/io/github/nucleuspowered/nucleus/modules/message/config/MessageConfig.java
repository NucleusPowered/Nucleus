/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.config;

import io.github.nucleuspowered.neutrino.annotations.Default;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.services.interfaces.INucleusTextTemplateFactory;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class MessageConfig {

    private static final String MESSAGE_SENDER_DEFAULT = "&7[me -> {{toDisplay}}&7]: &r";
    private static final String MESSAGE_RECEIVER_DEFAULT = "&7[{{fromDisplay}}&7 -> me]: &r";
    private static final String MESSAGE_SOCIAL_SPY_DEFAULT = "&7[SocialSpy] [{{fromDisplay}}&7 -> {{toDisplay}}&7]: &r";
    private static final String HELP_OP_DEFAULT = "&7HelpOp: {{name}} &7> &r";

    @Setting(value = "can-message-self", comment = "config.message.canmessageself")
    private boolean canMessageSelf = false;

    @Setting(value = "helpop-prefix", comment = "config.message.helpop.prefix")
    @Default(value = HELP_OP_DEFAULT, saveDefaultIfNull = true)
    private NucleusTextTemplateImpl helpOpPrefix;

    @Setting(value = "msg-receiver-prefix", comment = "config.message.receiver.prefix")
    @Default(value = MESSAGE_RECEIVER_DEFAULT, saveDefaultIfNull = true)
    private NucleusTextTemplateImpl messageReceiverPrefix;

    @Setting(value = "msg-sender-prefix", comment = "config.message.sender.prefix")
    @Default(value = MESSAGE_SENDER_DEFAULT, saveDefaultIfNull = true)
    private NucleusTextTemplateImpl messageSenderPrefix;

    @Setting(value = "socialspy")
    private SocialSpy socialSpy = new SocialSpy();

    public boolean isCanMessageSelf() {
        return this.canMessageSelf;
    }

    public NucleusTextTemplateImpl getHelpOpPrefix(INucleusTextTemplateFactory textTemplateFactory) {
        if (this.helpOpPrefix == null) {
            // set default
            this.helpOpPrefix = textTemplateFactory.createFromAmpersandString(HELP_OP_DEFAULT);
        }

        return this.helpOpPrefix;
    }

    public NucleusTextTemplateImpl getMessageReceiverPrefix(INucleusTextTemplateFactory textTemplateFactory) {
        if (this.messageReceiverPrefix == null) {
            // set default
            this.messageReceiverPrefix = textTemplateFactory.createFromAmpersandString(MESSAGE_RECEIVER_DEFAULT);
        }

        return this.messageReceiverPrefix;
    }

    public NucleusTextTemplateImpl getMessageSenderPrefix(INucleusTextTemplateFactory textTemplateFactory) {
        if (this.messageSenderPrefix == null) {
            // set default
            this.messageSenderPrefix = textTemplateFactory.createFromAmpersandString(MESSAGE_SENDER_DEFAULT);
        }

        return this.messageSenderPrefix;
    }

    public NucleusTextTemplateImpl getMessageSocialSpyPrefix(INucleusTextTemplateFactory textTemplateFactory) {
        if (this.socialSpy.messageSocialSpyPrefix == null) {
            // set default
            this.socialSpy.messageSocialSpyPrefix = textTemplateFactory.createFromAmpersandString(MESSAGE_SOCIAL_SPY_DEFAULT);
        }

        return this.socialSpy.messageSocialSpyPrefix;
    }

    public boolean isSocialSpyAllowForced() {
        return this.socialSpy.allowForced;
    }

    public boolean isSocialSpyLevels() {
        return this.socialSpy.socialSpyLevels;
    }

    public boolean isSocialSpySameLevel() {
        return this.socialSpy.socialSpySameLevel;
    }

    public int getCustomTargetLevel() {
        return this.socialSpy.level.customTargets;
    }

    public int getServerLevel() {
        return this.socialSpy.level.server;
    }

    public boolean isShowMessagesInSocialSpyWhileMuted() {
        return this.socialSpy.showMessagesInSocialSpyWhileMuted;
    }

    public String getMutedTag() {
        return this.socialSpy.mutedTag;
    }

    public String getBlockedTag() {
        return this.socialSpy.blocked;
    }

    public Targets spyOn() {
        return this.socialSpy.targets;
    }

    @ConfigSerializable
    public static class SocialSpy {
        @Setting(value = "msg-prefix", comment = "config.message.socialspy.prefix")
        @Default(value = MESSAGE_SOCIAL_SPY_DEFAULT, saveDefaultIfNull = true)
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

        @Setting(value = "msgtoggle-blocked-messages-tag", comment = "config.message.socialspy.msgtoggle")
        private String blocked = "&c[blocked] ";

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
            return this.player;
        }

        public boolean isServer() {
            return this.server;
        }

        public boolean isCustom() {
            return this.custom;
        }
    }
}
