/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chat.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.ChatUtil;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatConfig;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatTemplateConfig;
import io.github.nucleuspowered.nucleus.modules.staffchat.StaffChatMessageChannel;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ChatListener extends ListenerBase {

    private final String prefix = PermissionRegistry.PERMISSIONS_PREFIX + "chat.";

    private final Map<String[], Function<String, String>> replacements;

    private final ChatConfigAdapter cca;
    private final ChatUtil chatUtil;

    @Inject
    public ChatListener(ChatUtil chatUtil, ChatConfigAdapter cca) {
        this.chatUtil = chatUtil;
        this.cca = cca;
        replacements = createReplacements();
    }

    @Override
    public Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> mp = new HashMap<>();
        mp.put(prefix + "color", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.chat.color"), SuggestedLevel.ADMIN));
        mp.put(prefix + "colour", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.chat.colour"), SuggestedLevel.ADMIN));
        mp.put(prefix + "style", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.chat.style"), SuggestedLevel.ADMIN));
        mp.put(prefix + "magic", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.chat.magic"), SuggestedLevel.ADMIN));
        mp.put(prefix + "url", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.chat.urls"), SuggestedLevel.ADMIN));
        return mp;
    }

    private Map<String[], Function<String, String>> createReplacements() {
        Map<String[], Function<String, String>> t = new HashMap<>();

        t.put(new String[] { prefix + "colour", prefix + "color" }, s -> s.replaceAll("&[0-9a-fA-F]", ""));
        t.put(new String[] { prefix + "style" }, s -> s.replaceAll("&[l-oL-O]", ""));
        t.put(new String[] { prefix + "magic" }, s -> s.replaceAll("&[kK]", ""));

        return t;
    }

    private Text useMessage(Player player, Text rawMessage) {
        String m = rawMessage.toPlain();

        for (Map.Entry<String[],  Function<String, String>> r : replacements.entrySet()) {
            // If we don't have the required permission...
            if (Arrays.stream(r.getKey()).noneMatch(player::hasPermission)) {
                // ...strip the codes.
                m = r.getValue().apply(m);
            }
        }

        Text result;
        if (player.hasPermission(prefix + "url")) {
            result = chatUtil.addUrlsToAmpersandFormattedString(m);
        } else {
            result = TextSerializers.formattingCode('&').deserialize(m);
        }

        Optional<String> chatcol = Util.getOptionFromSubject(player, "chatcolour", "chatcolor");
        if (chatcol.isPresent()) {
            return Text.of(plugin.getNameUtil().getColourFromString(chatcol.get()), result);
        }

        return result;
    }

    // We do this first so that other plugins can alter it later if needs be.
    @Listener(order = Order.EARLY)
    public void onPlayerChat(MessageChannelEvent.Chat event, @Root Player player) {
        if (event.getChannel().isPresent() && event.getChannel().get() instanceof StaffChatMessageChannel) {
            // Staff chat. Not interested in applying these transforms.
            return;
        }

        ChatConfig config = cca.getNodeOrDefault();
        if (!config.isModifychat()) {
            return;
        }

        Text rawMessage = event.getRawMessage();
        ChatTemplateConfig ctc = config.getTemplate(player);
        event.setMessage(
                chatUtil.getPlayerMessageFromTemplate(ctc.getPrefix(), player, true),
                useMessage(player, rawMessage),
                chatUtil.getPlayerMessageFromTemplate(ctc.getSuffix(), player, false));
    }
}
