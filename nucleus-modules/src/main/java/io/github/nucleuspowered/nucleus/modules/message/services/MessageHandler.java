/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.services;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.message.NucleusPrivateMessagingService;
import io.github.nucleuspowered.nucleus.modules.message.MessagePermissions;
import io.github.nucleuspowered.nucleus.modules.message.config.MessageConfig;
import io.github.nucleuspowered.nucleus.modules.message.events.InternalNucleusMessageEvent;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.INucleusTextTemplateFactory;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerDisplayNameService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserPreferenceService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

@APIService(NucleusPrivateMessagingService.class)
public class MessageHandler implements NucleusPrivateMessagingService, IReloadableService.Reloadable, ServiceBase {

    private MessageConfig messageConfig;
    private boolean useLevels = false;
    private boolean sameLevel = false;
    private int serverLevel = 0;

    private final INucleusServiceCollection serviceCollection;
    private final Map<UUID, UUID> messagesReceived = Maps.newHashMap();
    private final Map<UUID, CustomMessageTarget<? extends CommandSource>> targets = Maps.newHashMap();
    private final Map<String, UUID> targetNames = Maps.newHashMap();

    @Inject
    public MessageHandler(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        onReload(serviceCollection);
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.messageConfig = serviceCollection.moduleDataProvider().getModuleConfig(MessageConfig.class);
        this.useLevels = this.messageConfig.isSocialSpyLevels();
        this.sameLevel = this.messageConfig.isSocialSpySameLevel();
        this.serverLevel = this.messageConfig.getServerLevel();
    }

    @Override
    public boolean isSocialSpy(User user) {
        Tristate ts = forcedSocialSpyState(user);
        if (ts == Tristate.UNDEFINED) {
            return this.serviceCollection.userPreferenceService().getUnwrapped(user.getUniqueId(), NucleusKeysProvider.SOCIAL_SPY);
        }

        return ts.asBoolean();
    }

    @Override public boolean isUsingSocialSpyLevels() {
        return this.useLevels;
    }

    @Override public boolean canSpySameLevel() {
        return this.sameLevel;
    }

    @Override public int getCustomTargetLevel() {
        return this.messageConfig.getCustomTargetLevel();
    }

    @Override public int getServerLevel() {
        return this.serverLevel;
    }

    @Override public int getSocialSpyLevel(User user) {
        return this.useLevels ? this.serviceCollection.permissionService().getPositiveIntOptionFromSubject(user, MessagePermissions.SOCIALSPY_LEVEL_KEY).orElse(0) : 0;
    }

    @Override public Tristate forcedSocialSpyState(User user) {
        IPermissionService permissionService = this.serviceCollection.permissionService();
        if (permissionService.hasPermission(user, MessagePermissions.BASE_SOCIALSPY)) {
            if (this.messageConfig.isSocialSpyAllowForced() &&
                    permissionService.hasPermission(user, MessagePermissions.SOCIALSPY_FORCE)) {
                return Tristate.TRUE;
            }

            return Tristate.UNDEFINED;
        }

        return Tristate.FALSE;
    }

    @Override
    public boolean setSocialSpy(User user, boolean isSocialSpy) {
        if (forcedSocialSpyState(user) != Tristate.UNDEFINED) {
            return false;
        }

        this.serviceCollection.userPreferenceService().set(user.getUniqueId(), NucleusKeysProvider.SOCIAL_SPY, isSocialSpy);
        return true;
    }

    @Override
    public boolean canSpyOn(User spyingUser, CommandSource... sourceToSpyOn) throws IllegalArgumentException {
        if (sourceToSpyOn.length == 0) {
            throw new IllegalArgumentException("sourceToSpyOn must have at least one CommandSource");
        }

        if (isSocialSpy(spyingUser)) {
            if (Arrays.stream(sourceToSpyOn).anyMatch(x -> x instanceof User && spyingUser.getUniqueId().equals(((User)x).getUniqueId()))) {
                return false;
            }

            if (this.useLevels) {
                int target = Arrays.stream(sourceToSpyOn).mapToInt(this::getSocialSpyLevelForSource).max().orElse(0);
                if (this.sameLevel) {
                    return target <= getSocialSpyLevel(spyingUser);
                } else {
                    return target < getSocialSpyLevel(spyingUser);
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public Set<CommandSource> onlinePlayersCanSpyOn(boolean includeConsole, CommandSource... sourceToSpyOn)
            throws IllegalArgumentException {
        if (sourceToSpyOn.length == 0) {
            throw new IllegalArgumentException("sourceToSpyOn must have at least one CommandSource");
        }

        // Get the users to scan.
        List<CommandSource> toSpyOn = Arrays.asList(sourceToSpyOn);
        Set<UUID> uuidsToSpyOn = toSpyOn.stream().map(x -> x instanceof User ? ((User)x).getUniqueId() : Util.CONSOLE_FAKE_UUID)
                .collect(Collectors.toSet());

        // Get those who aren't the subjects and have social spy on.
        Set<CommandSource> sources = Sponge.getServer().getOnlinePlayers().stream()
                .filter(x -> !uuidsToSpyOn.contains(x.getUniqueId()))
                .filter(this::isSocialSpy)
                .collect(Collectors.toSet());

        if (!this.useLevels) {
            if (includeConsole) {
                sources.add(Sponge.getServer().getConsole());
            }

            return sources;
        }

        // Get the highest level from the sources to spy on.
        int highestLevel = toSpyOn.stream().mapToInt(this::getSocialSpyLevelForSource).max().orElse(0);
        sources = sources.stream()
            .filter(x -> this.sameLevel ? getSocialSpyLevelForSource(x) >= highestLevel : getSocialSpyLevelForSource(x) > highestLevel)
            .collect(Collectors.toSet());

        if (includeConsole) {
            sources.add(Sponge.getServer().getConsole());
        }

        return sources;
    }

    @Override
    public boolean sendMessage(CommandSource sender, CommandSource receiver, String message) {
        // Message is about to be sent. Send the event out. If canceled, then that's that.
        boolean isBlocked = false;
        boolean isCancelled = Sponge.getEventManager().post(new InternalNucleusMessageEvent(sender, receiver, message));
        if (isCancelled) {
            this.serviceCollection.messageProvider().sendMessageTo(sender, "message.cancel");

            // Only continue to show Social Spy messages if the subject is muted.
            if (!this.messageConfig.isShowMessagesInSocialSpyWhileMuted()) {
                return false;
            }
        }

        // What about msgtoggle?
        IUserPreferenceService userPreferenceService = this.serviceCollection.userPreferenceService();
        // Cancel message if the reciever has message toggle false
        if (receiver instanceof Player && !this.serviceCollection.permissionService().hasPermission(sender, MessagePermissions.MSGTOGGLE_BYPASS) &&
                !userPreferenceService.getUnwrapped(((Player) receiver).getUniqueId(), NucleusKeysProvider.RECEIVING_MESSAGES)) {

            isCancelled = true;
            isBlocked = true;
            this.serviceCollection.messageProvider().sendMessageTo(sender,
                    "message.blocked",
                    this.serviceCollection.playerDisplayNameService().getDisplayName(receiver));

            if (!this.messageConfig.isShowMessagesInSocialSpyWhileMuted()) {
                return false;
            }
        }

        // Social Spies.
        final UUID uuidSender = getUUID(sender);
        final UUID uuidReceiver = getUUID(receiver);

        final Map<String, Object> variables = Maps.newHashMap();
        variables.put("from", sender);
        variables.put("to", receiver);

        // Create the tokens.
        Map<String, Function<CommandSource, Optional<Text>>> tokens = Maps.newHashMap();
        IPlayerDisplayNameService displayNameService = this.serviceCollection.playerDisplayNameService();
        tokens.put("from", cs -> getNameFromCommandSource(sender, displayNameService::getName));
        tokens.put("to", cs -> getNameFromCommandSource(receiver, displayNameService::getName));
        tokens.put("fromdisplay", cs -> getNameFromCommandSource(sender, displayNameService::getDisplayName));
        tokens.put("todisplay", cs -> getNameFromCommandSource(receiver, displayNameService::getDisplayName));

        Text tm = useMessage(sender, message);
        INucleusTextTemplateFactory textTemplateFactory = this.serviceCollection.textTemplateFactory();

        if (!isCancelled) {
            sender.sendMessage(constructMessage(sender, tm, this.messageConfig.getMessageSenderPrefix(textTemplateFactory), tokens, variables));
            receiver.sendMessage(constructMessage(sender, tm, this.messageConfig.getMessageReceiverPrefix(textTemplateFactory), tokens, variables));
        }

        NucleusTextTemplateImpl prefix = this.messageConfig.getMessageSocialSpyPrefix(textTemplateFactory);
        if (isBlocked) {
            prefix = textTemplateFactory.createFromAmpersandString(this.messageConfig.getBlockedTag() + prefix.getRepresentation());
        } if (isCancelled) {
            prefix = textTemplateFactory.createFromAmpersandString(this.messageConfig.getMutedTag() + prefix.getRepresentation());
        }

        MessageConfig.Targets targets = this.messageConfig.spyOn();
        if (sender instanceof Player && targets.isPlayer() || sender instanceof ConsoleSource && targets.isCustom() || targets.isCustom()) {
            Set<CommandSource> lm = onlinePlayersCanSpyOn(
                !uuidSender.equals(Util.CONSOLE_FAKE_UUID) && !uuidReceiver.equals(Util.CONSOLE_FAKE_UUID), sender, receiver
            );

            MessageChannel mc = MessageChannel.fixed(lm);
            if (!mc.getMembers().isEmpty()) {
                mc.send(constructMessage(sender, tm, prefix, tokens, variables));
            }
        }

        // Add the UUIDs to the reply list - the receiver will now reply to the sender.
        if (!isCancelled) {
            this.messagesReceived.put(uuidReceiver, uuidSender);
        }

        return !isCancelled;
    }

    private Optional<Text> getNameFromCommandSource(CommandSource source, Function<CommandSource, Text> standardFn) {
        if (source instanceof Identifiable) {
            CustomMessageTarget<? extends CommandSource> target = this.targets.get(((Identifiable) source).getUniqueId());
            if (target != null) {
                return Optional.of(target.getDisplayName());
            }
        }

        return Optional.of(standardFn.apply(source));
    }

    public boolean replyMessage(CommandSource sender, String message) {
        Optional<CommandSource> cs = getLastMessageFrom(getUUID(sender));
        if (cs.isPresent()) {
            return sendMessage(sender, cs.get(), message);
        }

        this.serviceCollection.messageProvider().sendMessageTo(sender, "message.noreply");
        return false;
    }

    @Override public Optional<CommandSource> getConsoleReplyTo() {
        return getLastMessageFrom(Util.CONSOLE_FAKE_UUID);
    }

    @Override public Optional<CommandSource> getReplyTo(User user) {
        return getLastMessageFrom(user.getUniqueId());
    }

    @Override public <T extends CommandSource & Identifiable> Optional<CommandSource> getCommandSourceReplyTo(T from) {
        return getLastMessageFrom(from.getUniqueId());
    }

    @Override public void setReplyTo(User user, CommandSource toReplyTo) {
        this.messagesReceived.put(user.getUniqueId(), getUUID(Preconditions.checkNotNull(toReplyTo)));
    }

    @Override public void setConsoleReplyTo(CommandSource toReplyTo) {
        this.messagesReceived.put(Util.CONSOLE_FAKE_UUID, getUUID(Preconditions.checkNotNull(toReplyTo)));
    }

    @Override public <T extends CommandSource & Identifiable> void setCommandSourceReplyTo(T source, CommandSource replyTo) {
        this.messagesReceived.put(source.getUniqueId(), getUUID(replyTo));
    }

    @Override public void clearReplyTo(User user) {
        this.messagesReceived.remove(user.getUniqueId());
    }

    @Override public <T extends CommandSource & Identifiable> void clearCommandSourceReplyTo(T user) {
        this.messagesReceived.remove(user.getUniqueId());
    }

    @Override public void clearConsoleReplyTo() {
        this.messagesReceived.remove(Util.CONSOLE_FAKE_UUID);
    }

    @Override
    public <T extends CommandSource & Identifiable> void registerMessageTarget(UUID uniqueId, String targetName, @Nullable Text displayName,
            Supplier<T> target) {
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(targetName);
        Preconditions.checkNotNull(target);
        Preconditions.checkArgument(!uniqueId.equals(Util.CONSOLE_FAKE_UUID), "Cannot use the zero UUID");
        Preconditions.checkArgument(targetName.toLowerCase().matches("[a-z0-9_-]{3,}"),
                "Target name must only contain letters, numbers, hyphens and underscores. and must be at least three characters long.");
        Preconditions.checkState(!this.targets.containsKey(uniqueId), "UUID already registered");
        Preconditions.checkState(!this.targetNames.containsKey(targetName.toLowerCase()), "Target name already registered.");

        // Create it
        this.targets.put(uniqueId, new CustomMessageTarget<>(uniqueId, displayName, target));
        this.targetNames.put(targetName.toLowerCase(), uniqueId);
    }

    private Optional<CommandSource> getLastMessageFrom(UUID from) {
        Preconditions.checkNotNull(from);
        UUID to = this.messagesReceived.get(from);
        if (to == null) {
            return Optional.empty();
        }

        if (to.equals(Util.CONSOLE_FAKE_UUID)) {
            return Optional.of(Sponge.getServer().getConsole());
        }

        if (this.targets.containsKey(to)) {
            Optional<? extends CommandSource> om = this.targets.get(to).get();
            if (om.isPresent()) {
                return om.map(x -> x);
            }
        }

        return Sponge.getServer().getOnlinePlayers().stream().filter(x -> x.getUniqueId().equals(to)).map(y -> (CommandSource) y).findFirst();
    }

    public Optional<CommandSource> getTarget(String target) {
        UUID u = this.targetNames.get(target.toLowerCase());
        if (u != null) {
            CustomMessageTarget<? extends CommandSource> cmt = this.targets.get(u);
            if (cmt != null) {
                return cmt.get().map(x -> x);
            }
        }

        return Optional.empty();
    }

    public ImmutableMap<String, UUID> getTargetNames() {
        return ImmutableMap.copyOf(this.targetNames);
    }

    private UUID getUUID(CommandSource sender) {
        return sender instanceof Identifiable ? ((Identifiable) sender).getUniqueId() : Util.CONSOLE_FAKE_UUID;
    }

    private Text constructMessage(CommandSource sender, Text message, NucleusTextTemplateImpl template,
            Map<String, Function<CommandSource, Optional<Text>>> tokens, Map<String, Object> variables) {
        return this.serviceCollection.textStyleService().joinTextsWithColoursFlowing(template.getForCommandSource(sender, tokens), message);
    }

    private Text useMessage(CommandSource player, String m) {
        this.serviceCollection.textStyleService().stripPermissionless(
                MessagePermissions.MESSAGE_COLOUR,
                MessagePermissions.MESSAGE_STYLE,
                player,
                m
        );

        Text result;
        if (this.serviceCollection.permissionService().hasPermission(player, MessagePermissions.MESSAGE_URLS)) {
            result = this.serviceCollection.textStyleService().addUrls(m);
        } else {
            result = TextSerializers.FORMATTING_CODE.deserialize(m);
        }

        return result;
    }

    private int getSocialSpyLevelForSource(CommandSource source) {
        if (this.useLevels) {
            if (source instanceof User) {
               return getSocialSpyLevel((User) source);
            } else if (source instanceof Identifiable && this.targets.containsKey(((Identifiable) source).getUniqueId())) {
                return this.messageConfig.getCustomTargetLevel();
            }

            return this.messageConfig.getServerLevel();
        }

        return 0;
    }

    @NonnullByDefault
    private static class CustomMessageTarget<T extends CommandSource & Identifiable> implements Identifiable {

        private final UUID uuid;
        @Nullable private final Text displayName;
        private final Supplier<T> supplier;

        private CustomMessageTarget(UUID uuid, @Nullable Text displayName, Supplier<T> supplier) {
            this.uuid = uuid;
            this.displayName = displayName;
            this.supplier = supplier;
        }

        private Optional<T> get() {
            T t = this.supplier.get();
            if (t.getUniqueId().equals(this.uuid)) {
                return Optional.of(t);
            }

            return Optional.empty();
        }

        private Text getDisplayName() {
            return this.displayName == null ? Text.of(this.supplier.get().getName()) : this.displayName;
        }

        @Override public UUID getUniqueId() {
            return this.uuid;
        }
    }
}
