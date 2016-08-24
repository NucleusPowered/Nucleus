/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.handlers;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.ChatUtil;
import io.github.nucleuspowered.nucleus.NameUtil;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.service.NucleusPrivateMessagingService;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.message.config.MessageConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.message.events.InternalNucleusMessageEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MessageHandler implements NucleusPrivateMessagingService {

    @Inject private UserDataManager ucl;
    @Inject private CoreConfigAdapter cca;
    @Inject private MessageConfigAdapter mca;
    @Inject private ChatUtil chatUtil;
    private Supplier<CommandPermissionHandler> cph = null;

    private final Map<String[], Function<String, String>> replacements = createReplacements();
    private final Map<UUID, UUID> messagesReceived = Maps.newHashMap();

    public void setCommandPermissionHandler(Supplier<CommandPermissionHandler> commandPermissionHandler) {
        if (cph == null) {
            cph = commandPermissionHandler;
        }
    }

    @Override
    public boolean isSocialSpy(User user) {
        try {
            return ucl.get(user).get().isSocialSpy();
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }

            return false;
        }
    }

    @Override
    public boolean setSocialSpy(User user, boolean isSocialSpy) {
        try {
            return ucl.get(user).get().setSocialSpy(isSocialSpy);
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }

            return false;
        }
    }

    public boolean replyMessage(CommandSource sender, String message) {
        Optional<CommandSource> cs = getPlayerToReplyTo(getUUID(sender));
        if (cs.isPresent()) {
            return sendMessage(sender, cs.get(), message);
        }

        sender.sendMessage(Util.getTextMessageWithFormat("message.noreply"));
        return false;
    }

    public boolean sendMessage(CommandSource sender, CommandSource receiver, String message) {
        // Message is about to be sent. Send the event out. If canceled, then
        // that's that.
        if (Sponge.getEventManager().post(new InternalNucleusMessageEvent(sender, receiver, message))) {
            sender.sendMessage(Util.getTextMessageWithFormat("message.cancel"));
            return false;
        }

        // Social Spies.
        final UUID uuidSender = getUUID(sender);
        final UUID uuidReceiver = getUUID(receiver);
        List<MessageReceiver> lm =
                ucl.getOnlineUsersInternal().stream().filter(x -> !uuidSender.equals(x.getUniqueID()) && !uuidReceiver.equals(x.getUniqueID()))
                        .filter(UserService::isSocialSpy).map(x -> x.getUser().getPlayer().orElse(null))
                        .filter(x -> x != null && x.isOnline()).collect(Collectors.toList());

        // If the console is not involved, make them involved.
        if (!uuidSender.equals(Util.consoleFakeUUID) && !uuidReceiver.equals(Util.consoleFakeUUID)) {
            lm.add(Sponge.getServer().getConsole());
        }

        // Create the tokens.
        Map<String, BiFunction<CommandSource, String, Text>> tokens = Maps.newHashMap();
        tokens.put("{{from}}", (cs, g) -> chatUtil.addCommandToName(sender));
        tokens.put("{{to}}", (cs, g) -> chatUtil.addCommandToName(receiver));
        tokens.put("{{fromdisplay}}", (cs, g) -> chatUtil.addCommandToDisplayName(sender));
        tokens.put("{{todisplay}}", (cs, g) -> chatUtil.addCommandToDisplayName(receiver));

        MessageChannel mc = MessageChannel.fixed(lm);
        Text tm = useMessage(sender, message);

        sender.sendMessage(constructMessage(sender, tm, mca.getNodeOrDefault().getMessageSenderPrefix(), tokens));
        receiver.sendMessage(constructMessage(sender, tm, mca.getNodeOrDefault().getMessageReceiverPrefix(), tokens));
        mc.send(constructMessage(sender, tm, mca.getNodeOrDefault().getMessageSocialSpyPrefix(), tokens));

        // Add the UUIDs to the reply list - the receiver will now reply to the sender.
        messagesReceived.put(uuidReceiver, uuidSender);
        return true;
    }

    public Optional<CommandSource> getPlayerToReplyTo(UUID from) {
        Preconditions.checkNotNull(from);
        UUID to = messagesReceived.get(from);
        if (to == null) {
            return Optional.empty();
        }

        if (to.equals(Util.consoleFakeUUID)) {
            return Optional.of(Sponge.getServer().getConsole());
        }

        return Sponge.getServer().getOnlinePlayers().stream().filter(x -> x.getUniqueId().equals(to)).map(y -> (CommandSource) y).findFirst();
    }

    private UUID getUUID(CommandSource sender) {
        return sender instanceof Player ? ((Player) sender).getUniqueId() : Util.consoleFakeUUID;
    }

    private Text getName(CommandSource src) {
        if (!(src instanceof Player)) {
            return Text.builder(src.getName()).color(TextColors.LIGHT_PURPLE).onClick(TextActions.suggestCommand("/msg - ")).build();
        }

        return NameUtil.getNameFromCommandSource(src).toBuilder().onClick(TextActions.suggestCommand("/msg " + src.getName() + " ")).build();
    }

    @SuppressWarnings("unchecked")
    private Text constructMessage(CommandSource sender, Text message, String template, Map<String, BiFunction<CommandSource, String, Text>> tokens) {
        return Text.of(chatUtil.getMessageFromTokens(template, sender, false, false, false, tokens), message);
    }

    private Map<String[], Function<String, String>> createReplacements() {
        Map<String[], Function<String, String>> t = new HashMap<>();

        t.put(new String[] { "colour", "color" }, s -> s.replaceAll("&[0-9a-fA-F]", ""));
        t.put(new String[] { "style" }, s -> s.replaceAll("&[l-oL-O]", ""));
        t.put(new String[] { "magic" }, s -> s.replaceAll("&[kK]", ""));

        return t;
    }

    private Text useMessage(CommandSource player, String m) {
        if (cph == null) {
            return Text.of(m);
        }

        for (Map.Entry<String[],  Function<String, String>> r : replacements.entrySet()) {
            // If we don't have the required permission...
            if (Arrays.stream(r.getKey()).noneMatch(x -> cph.get().testSuffix(player, x))) {
                // ...strip the codes.
                m = r.getValue().apply(m);
            }
        }

        Text result;
        if (cph.get().testSuffix(player, "url")) {
            result = chatUtil.addUrlsToAmpersandFormattedString(m);
        } else {
            result = TextSerializers.FORMATTING_CODE.deserialize(m);
        }

        return result;
    }
}
