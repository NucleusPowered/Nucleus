/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.services;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.module.message.NucleusPrivateMessagingService;
import io.github.nucleuspowered.nucleus.api.module.message.target.CustomMessageTarget;
import io.github.nucleuspowered.nucleus.api.module.message.target.MessageTarget;
import io.github.nucleuspowered.nucleus.api.module.message.target.SystemMessageTarget;
import io.github.nucleuspowered.nucleus.api.module.message.target.UserMessageTarget;
import io.github.nucleuspowered.nucleus.modules.message.MessageKeys;
import io.github.nucleuspowered.nucleus.modules.message.MessagePermissions;
import io.github.nucleuspowered.nucleus.modules.message.config.MessageConfig;
import io.github.nucleuspowered.nucleus.modules.message.events.InternalNucleusMessageEvent;
import io.github.nucleuspowered.nucleus.modules.message.services.target.AbstractMessageTarget;
import io.github.nucleuspowered.nucleus.modules.message.services.target.PlayerMessageTarget;
import io.github.nucleuspowered.nucleus.modules.message.services.target.SystemSubjectMessageTarget;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusTextTemplateFactory;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.util.Tristate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@APIService(NucleusPrivateMessagingService.class)
public class MessageHandler implements NucleusPrivateMessagingService, IReloadableService.Reloadable, ServiceBase {

    private MessageConfig messageConfig;
    private boolean useLevels = false;
    private boolean sameLevel = false;
    private int serverLevel = 0;

    private final INucleusServiceCollection serviceCollection;
    private final SystemMessageTarget systemMessageTarget;
    private final Map<UUID, PlayerMessageTarget> players = new HashMap<>();
    private final Map<String, CustomMessageTarget> targetNames = new HashMap<>();

    @Inject
    public MessageHandler(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.systemMessageTarget =
                new SystemSubjectMessageTarget(serviceCollection.playerDisplayNameService().getDisplayName(Util.CONSOLE_FAKE_UUID));
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.messageConfig = serviceCollection.configProvider().getModuleConfig(MessageConfig.class);
        this.useLevels = this.messageConfig.isSocialSpyLevels();
        this.sameLevel = this.messageConfig.isSocialSpySameLevel();
        this.serverLevel = this.messageConfig.getServerLevel();
    }

    @Override
    public boolean isSocialSpy(final UUID user) {
        final Tristate ts = this.forcedSocialSpyState(user);
        if (ts == Tristate.UNDEFINED) {
            return this.serviceCollection.userPreferenceService().getUnwrapped(user, MessageKeys.SOCIAL_SPY);
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

    @Override
    public int getSocialSpyLevel(final UUID uuid) {
        if (this.useLevels) {
            final User user = Sponge.server().userManager().load(uuid).join().orElseThrow(() -> new IllegalArgumentException("Cannot get user with "
                    + "UUID " + uuid.toString()));
            return this.serviceCollection.permissionService().getPositiveIntOptionFromSubject(user, MessagePermissions.SOCIALSPY_LEVEL_KEY).orElse(0);
        }
        return 0;
    }

    @Override
    public Tristate forcedSocialSpyState(final UUID user) {
        final IPermissionService permissionService = this.serviceCollection.permissionService();
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
    public boolean setSocialSpy(final UUID user, final boolean isSocialSpy) {
        if (this.forcedSocialSpyState(user) != Tristate.UNDEFINED) {
            return false;
        }

        this.serviceCollection.userPreferenceService().set(user, MessageKeys.SOCIAL_SPY, isSocialSpy);
        return true;
    }

    @Override
    public boolean canSpyOn(final UUID spyingUser, final MessageTarget... sourceToSpyOn) throws IllegalArgumentException {
        if (sourceToSpyOn.length == 0) {
            throw new IllegalArgumentException("sourceToSpyOn must have at least one MessageTarget");
        }

        if (this.isSocialSpy(spyingUser)) {
            if (Arrays.stream(sourceToSpyOn).anyMatch(x -> x instanceof Identified && spyingUser.equals(((Identified) x).identity().uuid()))) {
                return false;
            }

            if (this.useLevels) {
                final int target = Arrays.stream(sourceToSpyOn).mapToInt(this::getSocialSpyLevelForSource).max().orElse(0);
                if (this.sameLevel) {
                    return target <= this.getSocialSpyLevel(spyingUser);
                } else {
                    return target < this.getSocialSpyLevel(spyingUser);
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public Set<UUID> onlinePlayersCanSpyOn(final boolean includeConsole, final MessageTarget... sourceToSpyOn)
            throws IllegalArgumentException {
        if (sourceToSpyOn.length == 0) {
            throw new IllegalArgumentException("sourceToSpyOn must have at least one MessageTarget");
        }

        // Get the users to scan.
        final List<MessageTarget> toSpyOn = Arrays.asList(sourceToSpyOn);
        final Set<UUID> uuidsToSpyOn = toSpyOn.stream()
                .filter(x -> x instanceof UserMessageTarget)
                .map(x -> ((UserMessageTarget) x).getUserUUID())
                .collect(Collectors.toSet());

        // Get those who aren't the subjects and have social spy on.
        final Set<ServerPlayer> sources = Sponge.server().onlinePlayers()
                .stream()
                .filter(x -> !uuidsToSpyOn.contains(x.uniqueId()))
                .filter(x -> this.isSocialSpy(x.uniqueId()))
                .collect(Collectors.toSet());

        if (!this.useLevels) {
            return sources.stream().map(Identifiable::uniqueId).collect(Collectors.toSet());
        }

        // Get the highest level from the sources to spy on.
        final int highestLevel = toSpyOn.stream().mapToInt(this::getSocialSpyLevelForSource).max().orElse(0);
        return sources.stream()
            .filter(x -> this.sameLevel ? this.getSocialSpyLevel(x.uniqueId()) >= highestLevel :
                    this.getSocialSpyLevel(x.uniqueId()) > highestLevel)
            .map(Identifiable::uniqueId)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean sendMessage(final MessageTarget sender, final MessageTarget receiver, final String message) {
        // Message is about to be sent. Send the event out. If canceled, then that's that.
        boolean isBlocked = false;
        this.checkValid(sender);
        this.checkValid(receiver);
        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            @Nullable final ServerPlayer senderAsPlayer = sender instanceof UserMessageTarget ?
                    Sponge.server().player(((UserMessageTarget) sender).getUserUUID()).orElseThrow(() -> new IllegalArgumentException("Sender"
                            + " is not online")) : null;
            if (sender instanceof AbstractMessageTarget) {
                ((AbstractMessageTarget) sender).pushCauseToFrame(frame);
            } else {
                frame.pushCause(sender);
            }
            boolean isCancelled = Sponge.eventManager().post(new InternalNucleusMessageEvent(frame.currentCause(), sender, receiver, message));
            if (isCancelled) {

                sender.getRepresentedAudience().ifPresent(x -> this.serviceCollection.messageProvider().sendMessageTo(x, "message.cancel"));

                // Only continue to show Social Spy messages if the subject is muted.
                if (!this.messageConfig.isShowMessagesInSocialSpyWhileMuted()) {
                    return false;
                }
            }

            // Cancel message if the receiver has message toggle false
            if (!sender.isAvailableForMessages() && !receiver.canBypassMessageToggle()) {

                isCancelled = true;
                isBlocked = true;
                sender.getRepresentedAudience().ifPresent(x ->
                        this.serviceCollection.messageProvider().sendMessageTo(x,
                        "message.blocked",
                        receiver.getDisplayName()));

                if (!this.messageConfig.isShowMessagesInSocialSpyWhileMuted()) {
                    return false;
                }
            }

            // Social Spies.
            final UUID uuidSender = this.getUUID(sender);
            final UUID uuidReceiver = this.getUUID(receiver);

            // Create the tokens.
            final Map<String, Function<Object, Optional<ComponentLike>>> tokens = new HashMap<>();
            tokens.put("from", cs -> Optional.of(Component.text(sender.getName())));
            tokens.put("to", cs -> Optional.of(Component.text(receiver.getName())));
            tokens.put("fromdisplay", cs -> Optional.of(sender.getDisplayName()));
            tokens.put("todisplay", cs -> Optional.of(receiver.getDisplayName()));

            final Component tm = this.useMessage(senderAsPlayer, message);
            final INucleusTextTemplateFactory textTemplateFactory = this.serviceCollection.textTemplateFactory();

            if (!isCancelled) {
                sender.getRepresentedAudience().ifPresent(x ->
                        x.sendMessage(this.constructMessage(sender, tm, this.messageConfig.getMessageSenderPrefix(textTemplateFactory), tokens)));
                receiver.receiveMessageFrom(sender,
                        this.constructMessage(sender, tm, this.messageConfig.getMessageReceiverPrefix(textTemplateFactory), tokens));
            }

            NucleusTextTemplateImpl prefix = this.messageConfig.getMessageSocialSpyPrefix(textTemplateFactory);
            if (isBlocked) {
                prefix = textTemplateFactory.createFromAmpersandString(this.messageConfig.getBlockedTag() + prefix.asComponent());
            }
            if (isCancelled) {
                prefix = textTemplateFactory.createFromAmpersandString(this.messageConfig.getMutedTag() + prefix.asComponent());
            }

            final MessageConfig.Targets targets = this.messageConfig.spyOn();
            if (sender instanceof PlayerMessageTarget && targets.isPlayer() || sender instanceof SystemMessageTarget && targets.isServer() || targets.isCustom()) {
                final Set<UUID> lm = this.onlinePlayersCanSpyOn(
                        !uuidSender.equals(Util.CONSOLE_FAKE_UUID) && !uuidReceiver.equals(Util.CONSOLE_FAKE_UUID), sender, receiver
                );

                if (!lm.isEmpty()) {
                    final Component socialSpyToSend = this.constructMessage(sender, tm, prefix, tokens);
                    for (final UUID uuid : lm) {
                        Sponge.server().player(uuid).ifPresent(x -> x.sendMessage(socialSpyToSend));
                    }
                }
            }

            return !isCancelled;
        }
    }

    @Override
    public SystemMessageTarget getSystemMessageTarget() {
        return this.systemMessageTarget;
    }

    @Override
    public Optional<UserMessageTarget> getUserMessageTarget(final UUID uuid) {
        return Optional.ofNullable(this.players.get(uuid));
    }

    @Override
    public Optional<CustomMessageTarget> getCustomMessageTarget(final String identifier) {
        return Optional.ofNullable(this.targetNames.get(identifier));
    }

    @Override
    public Optional<MessageTarget> getCurrentReplyTarget(final MessageTarget target) {
        return target.replyTarget().map(Function.identity());
    }

    @Override
    public void setReplyTarget(final MessageTarget source, final MessageTarget newTarget) {
        this.checkValid(source);
        this.checkValid(newTarget);
        source.setReplyTarget(newTarget);
    }

    @Override
    public void clearReplyTarget(final MessageTarget source) {
        this.checkValid(source);
        source.setReplyTarget(null);
    }

    @Override
    public void registerMessageTarget(final CustomMessageTarget messageTarget)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        if (this.targetNames.containsKey(messageTarget.getIdentifier())) {
            throw new IllegalArgumentException("Message target identifier " + messageTarget.getIdentifier() + " already in use.");
        }
        this.targetNames.put(messageTarget.getIdentifier(), messageTarget);
    }

    public Optional<MessageTarget> getTarget(final String target) {
        return Optional.ofNullable(this.targetNames.get(target));
    }

    public Set<String> getCustomTargets() {
        return this.targetNames.keySet();
    }

    public void addPlayer(final UUID uuid) {
        this.players.put(uuid, new PlayerMessageTarget(uuid,
                this.serviceCollection.userPreferenceService(),
                this.serviceCollection.permissionService(),
                this.serviceCollection.playerDisplayNameService()));
    }

    public void removePlayer(final UUID uuid) {
        this.players.remove(uuid);
    }

    private UUID getUUID(final MessageTarget sender) {
        return sender instanceof UserMessageTarget ? ((UserMessageTarget) sender).getUserUUID() : Util.CONSOLE_FAKE_UUID;
    }

    private Component constructMessage(final MessageTarget sender, final Component message, final NucleusTextTemplateImpl template,
            final Map<String, Function<Object, Optional<ComponentLike>>> tokens) {
        return this.serviceCollection.textStyleService().joinTextsWithColoursFlowing(
                template.getForObjectWithTokens(sender.getRepresentedAudience().orElseGet(Sponge::systemSubject), tokens),
                message);
    }

    private Component useMessage(@Nullable final ServerPlayer player, final String m) {
        if (player != null) {
            this.serviceCollection.textStyleService().stripPermissionless(
                    MessagePermissions.MESSAGE_COLOUR,
                    MessagePermissions.MESSAGE_STYLE,
                    player,
                    m
            );
        }

        final Component result;
        if (player == null || this.serviceCollection.permissionService().hasPermission(player, MessagePermissions.MESSAGE_URLS)) {
            result = this.serviceCollection.textStyleService().addUrls(m);
        } else {
            result = LegacyComponentSerializer.legacyAmpersand().deserialize(m);
        }

        return result;
    }

    private int getSocialSpyLevelForSource(final MessageTarget source) {
        if (this.useLevels) {
            if (source instanceof UserMessageTarget) {
               return this.getSocialSpyLevel(((UserMessageTarget) source).getUserUUID());
            } else if (source instanceof CustomMessageTarget) {
                return this.messageConfig.getCustomTargetLevel();
            }

            return this.messageConfig.getServerLevel();
        }

        return 0;
    }

    private void checkValid(final MessageTarget messageTarget) {
        if ((messageTarget == this.systemMessageTarget) ||
                (messageTarget instanceof PlayerMessageTarget && this.players.containsValue(messageTarget)) ||
                (messageTarget instanceof CustomMessageTarget && this.targetNames.containsValue(messageTarget))) {
            return;
        }
        throw new IllegalStateException("Target is not online or not registered.");
    }

}
