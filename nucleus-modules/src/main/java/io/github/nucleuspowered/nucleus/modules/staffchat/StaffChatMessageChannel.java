/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.api.module.staffchat.NucleusStaffChatChannel;
import io.github.nucleuspowered.nucleus.modules.staffchat.config.StaffChatConfig;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.chatmessageformatter.AbstractNucleusChatChannel;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.IChatMessageFormatterService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserPreferenceService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class StaffChatMessageChannel implements
        IChatMessageFormatterService.Channel.External<StaffChatMessageChannel.APIChannel>,
        IReloadableService.Reloadable {

    private static StaffChatMessageChannel INSTANCE = null;

    private boolean formatting = false;

    public static StaffChatMessageChannel getInstance() {
        Preconditions.checkState(INSTANCE != null, "StaffChatMessageChannel#Instance");
        return INSTANCE;
    }

    private final IPermissionService permissionService;
    private final IUserPreferenceService userPreferenceService;
    private NucleusTextTemplateImpl template;
    private TextColor colour;

    @Inject
    StaffChatMessageChannel(INucleusServiceCollection serviceCollection) {
        serviceCollection.reloadableService().registerReloadable(this);
        this.permissionService = serviceCollection.permissionService();
        this.userPreferenceService = serviceCollection.userPreferenceService();
        this.onReload(serviceCollection);
        INSTANCE = this;
    }

    @Override
    public boolean willFormat() {
        return true;
    }

    public APIChannel createChannel(MessageChannel delegated) {
        return new APIChannel(
                delegated,
                receivers().stream().filter(x -> delegated.getMembers().contains(x)).collect(Collectors.toList())
        );
    }

    public boolean formatMessages() {
        return this.formatting;
    }

    public void sendMessageFrom(CommandSource source, Text text) {
        MessageEvent.MessageFormatter formatters = new MessageEvent.MessageFormatter();
        formatters.setBody(text);
        formatMessageEvent(source, formatters);
        MessageChannel.fixed(receivers()).send(source, formatters.format(), ChatTypes.CHAT);
    }

    @Override
    public void formatMessageEvent(CommandSource source, MessageEvent.MessageFormatter formatters) {
        Text prefix = this.template.getForCommandSource(source);
        if (TextSerializers.PLAIN.serialize(formatters.getHeader().toText()).contains("<" + source.getName() + ">")) {
            // Remove it.
            Text p = formatters.getHeader().toText().replace("<" + source.getName() + ">", Text.of(), true);
            if (p.toPlain().trim().isEmpty()) {
                formatters.setHeader(prefix);
            } else {
                formatters.setHeader(Text.of(p, prefix));
            }
        } else {
            formatters.setHeader(Text.of(formatters.getHeader(), prefix));
        }
        formatters.setBody(Text.of(this.colour, formatters.getBody()));
    }

    @Override
    public Collection<MessageReceiver> receivers() {
        List<MessageReceiver> c =
                Sponge.getServer().getOnlinePlayers().stream()
                        .filter(this::test)
                        .collect(Collectors.toList());
        c.add(Sponge.getServer().getConsole());
        return c;
    }

    @Override
    public boolean ignoreIgnoreList() {
        return true;
    }

    private boolean test(Player player) {
        if (this.permissionService.hasPermission(player, StaffChatPermissions.BASE_STAFFCHAT)) {
            return this.userPreferenceService
                    .getPreferenceFor(player, NucleusKeysProvider.VIEW_STAFF_CHAT)
                    .orElse(true);
        }

        return false;
    }

    public void onReload(INucleusServiceCollection serviceCollection) {
        StaffChatConfig sc = serviceCollection.moduleDataProvider().getModuleConfig(StaffChatConfig.class);
        this.formatting = sc.isIncludeStandardChatFormatting();
        this.template = sc.getMessageTemplate();
        this.colour = serviceCollection.textStyleService().getColourFromString(sc.getMessageColour());
    }

    public static class APIChannel extends AbstractNucleusChatChannel.Mutable<APIChannel>
            implements NucleusStaffChatChannel {

        public APIChannel(MessageChannel messageChannel, Collection<MessageReceiver> messageReceivers) {
            super(messageChannel, messageReceivers);
        }
    }

}
