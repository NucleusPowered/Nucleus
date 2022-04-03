/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname.services;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.nickname.NucleusNicknameService;
import io.github.nucleuspowered.nucleus.api.module.nickname.exception.NicknameException;
import io.github.nucleuspowered.nucleus.modules.nickname.NicknameKeys;
import io.github.nucleuspowered.nucleus.modules.nickname.NicknamePermissions;
import io.github.nucleuspowered.nucleus.modules.nickname.config.NicknameConfig;
import io.github.nucleuspowered.nucleus.modules.nickname.events.ChangeNicknameEventPost;
import io.github.nucleuspowered.nucleus.modules.nickname.events.ChangeNicknameEventPre;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlayerDisplayNameService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITextStyleService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.permission.Subject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@APIService(NucleusNicknameService.class)
public class NicknameService implements NucleusNicknameService, IReloadableService.Reloadable, ServiceBase {

    private final IMessageProviderService messageProviderService;
    private final IStorageManager storageManager;
    private final ITextStyleService textStyleService;
    private final IPlayerDisplayNameService playerDisplayNameService;

    @Inject
    public NicknameService(final INucleusServiceCollection serviceCollection) {
        this.messageProviderService = serviceCollection.messageProvider();
        this.storageManager = serviceCollection.storageManager();
        this.textStyleService = serviceCollection.textStyleService();
        this.playerDisplayNameService = serviceCollection.playerDisplayNameService();
    }

    private Component prefix = Component.empty();
    private Pattern pattern;
    private int min = 3;
    private int max = 16;
    private final List<UUID> cached = new ArrayList<>();
    private final BiMap<UUID, String> cache = HashBiMap.create();
    private final BiMap<UUID, Component> textCache = HashBiMap.create();
    private final TreeMap<String, UUID> reverseLowerCaseCache = new TreeMap<>();

    public void injectResolver(final INucleusServiceCollection serviceCollection) {
        serviceCollection.playerDisplayNameService().provideDisplayNameResolver(this::getNicknameWithPrefix);
        serviceCollection.playerDisplayNameService().provideDisplayNameQuery(
                new IPlayerDisplayNameService.DisplayNameQuery() {
                    @Override public Optional<User> resolve(final String name) {
                        return NicknameService.this.getFromCache(name).map(ServerPlayer::user);
                    }

                    @Override public Map<UUID, String> startsWith(final String name) {
                        return NicknameService.this.startsWithUUIDStringMap(name);
                    }
                }
        );
    }

    public void markRead(final UUID player) {
        this.cached.add(player);
    }

    public void updateCache(final UUID player, final Component text) {
        this.cache.put(player, text.toString());
        this.textCache.put(player, text);
    }

    public Optional<ServerPlayer> getFromCache(final String text) {
        final UUID u = this.cache.inverse().get(text);
        if (u != null) {
            final Optional<ServerPlayer> ret = Sponge.server().player(u);
            if (!ret.isPresent()) {
                this.cache.remove(u);
            }

            return ret;
        }

        return Optional.empty();
    }

    public Map<Player, Component> getFromSubstring(final String search) {
        final String prefix = search.toLowerCase();
        final Collection<UUID> uuidCollection;
        if (prefix.length() > 0) {
            final char nextLetter = (char) (prefix.charAt(prefix.length() -1) + 1);
            final String end = prefix.substring(0, prefix.length()-1) + nextLetter;
            uuidCollection = this.reverseLowerCaseCache.subMap(prefix, end).values();
        } else {
            uuidCollection = this.reverseLowerCaseCache.values();
        }

        final Map<Player, Component> mapToReturn = new HashMap<>();
        Sponge.server().onlinePlayers().stream()
                .filter(x -> !this.cache.containsKey(x.uniqueId()))
                .filter(x -> x.name().toLowerCase().startsWith(prefix))
                .forEach(player -> mapToReturn.put(player, player.get(Keys.CUSTOM_NAME).orElseGet(
                        () -> Component.text(player.name() + "*"))));

        for (final UUID uuid : uuidCollection) {
            final Optional<ServerPlayer> op = Sponge.server().player(uuid);
            op.ifPresent(player -> mapToReturn.put(player, this.textCache.get(uuid)));
        }

        return Collections.unmodifiableMap(mapToReturn);
    }

    public Map<UUID, String> startsWithUUIDStringMap(final String text) {
        return this.cache.inverse().entrySet().stream().filter(x -> x.getKey().startsWith(text.toLowerCase()))
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }


    public List<UUID> startsWith(final String text) {
        return this.cache.inverse().entrySet().stream().filter(x -> x.getKey().startsWith(text.toLowerCase()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public void removeFromCache(final UUID player) {
        this.cache.remove(player);
        this.textCache.remove(player);
        this.cached.remove(player);
    }

    @Override
    public Optional<Component> getNicknameWithPrefix(final UUID user) {
        return this.getNickname(user).map(x -> Component.join(JoinConfiguration.separator(this.prefix), x));
    }

    @Override
    public Optional<Component> getNicknameWithPrefix(final User user) {
        return this.getNickname(user).map(x -> Component.join(JoinConfiguration.separator(this.prefix), x));
    }

    @Override
    public Optional<Component> getNickname(final User user) {
        return this.getNickname(user.uniqueId());
    }

    @Override
    public Optional<Component> getNickname(final UUID user) {
        if (this.cached.contains(user)) {
            return Optional.ofNullable(this.textCache.get(user));
        }
        return this.storageManager.getUserService()
                .getOnThread(user)
                .flatMap(x -> x.get(NicknameKeys.USER_NICKNAME_JSON))
                .map(GsonComponentSerializer.colorDownsamplingGson()::deserialize);
    }

    @Override
    public void setNickname(final UUID user, @Nullable final Component nickname, final boolean bypassRestrictions) throws NicknameException {
        if (nickname != null) {
            this.setNick(user, nickname, bypassRestrictions);
        } else {
            this.removeNick(user);
        }

    }

    public void removeNick(final UUID uuid) throws NicknameException {
        final Component currentNickname = this.getNickname(uuid).orElse(null);
        final Component name = this.playerDisplayNameService.getName(uuid);
        final Cause cause = Sponge.server().causeStackManager().currentCause();

        final ChangeNicknameEventPre cne = new ChangeNicknameEventPre(cause, currentNickname, null, uuid);
        if (Sponge.eventManager().post(cne)) {

            throw new NicknameException(
                    this.messageProviderService.getMessage("command.nick.eventcancel", name),
                    NicknameException.Type.EVENT_CANCELLED
            );
        }

        this.storageManager.getUserService().removeAndSave(uuid, NicknameKeys.USER_NICKNAME_JSON);
        this.removeFromCache(uuid);
        Sponge.eventManager().post(new ChangeNicknameEventPost(cause, currentNickname, null, uuid));

        final Optional<User> user = Sponge.server().userManager().load(uuid).join();
        if (user.isPresent()) {
            final Optional<ServerPlayer> player = user.get().player();
            if (player.isPresent()) {
                this.messageProviderService.sendMessageTo(player.get(), "command.delnick.success.base");
                player.get().remove(Keys.CUSTOM_NAME);
            } else {
                user.get().remove(Keys.CUSTOM_NAME);
            }
        }
    }

    public void setNick(final UUID pl, final Component nickname, final boolean bypass) throws NicknameException {
        final String plain = PlainTextComponentSerializer.plainText().serialize(nickname).trim();
        if (plain.isEmpty()) {
            throw new NicknameException(
                    this.messageProviderService.getMessage("command.nick.tooshort"),
                    NicknameException.Type.TOO_SHORT
            );
        }

        // Does the user exist?
        try {
            final Optional<User> match = Sponge.server().userManager().load(plain).join();

            // The only person who can use such a name is oneself.
            if (match.isPresent() && !match.get().uniqueId().equals(pl)) {
                // Fail - cannot use another's name.
                throw new NicknameException(
                        this.messageProviderService.getMessage("command.nick.nameinuse", plain),
                        NicknameException.Type.NOT_OWN_IGN);
            }
        } catch (final IllegalArgumentException ignored) {
            // We allow some other nicknames too.
        }

        final Cause cause = Sponge.server().causeStackManager().currentCause();
        if (!bypass) {
            // Giving subject must have the colour permissions and whatnot. Also,
            // colour and color are the two spellings we support. (RULE BRITANNIA!)
            final Optional<Subject> os = cause.first(Subject.class);
            if (os.isPresent()) {
                this.stripPermissionless(os.get(), nickname);
            }

            if (!this.pattern.matcher(plain).matches()) {
                throw new NicknameException(
                        this.messageProviderService.getMessage("command.nick.nopattern", this.pattern.pattern()),
                        NicknameException.Type.INVALID_PATTERN);
            }

            final int strippedNameLength = plain.length();

            // Do a regex remove to check minimum length requirements.
            if (strippedNameLength < Math.max(this.min, 1)) {
                throw new NicknameException(
                        this.messageProviderService.getMessage("command.nick.tooshort"),
                        NicknameException.Type.TOO_SHORT
                );
            }

            // Do a regex remove to check maximum length requirements. Will be at least the minimum length
            if (strippedNameLength > Math.max(this.max, this.min)) {
                throw new NicknameException(
                        this.messageProviderService.getMessage("command.nick.toolong"),
                        NicknameException.Type.TOO_SHORT
                );
            }
        }

        final Component name = this.playerDisplayNameService.getName(pl);

        // Send an event
        final Component currentNickname = this.getNickname(pl).orElse(null);
        final ChangeNicknameEventPre cne = new ChangeNicknameEventPre(cause, currentNickname, nickname, pl);
        if (Sponge.eventManager().post(cne)) {
            throw new NicknameException(
                    this.messageProviderService.getMessage("command.nick.eventcancel", name),
                    NicknameException.Type.EVENT_CANCELLED
            );
        }

        this.storageManager.getUserService().setAndSave(pl, NicknameKeys.USER_NICKNAME_JSON, GsonComponentSerializer.gson().serialize(nickname));
        this.updateCache(pl, nickname);

        Sponge.eventManager().post(new ChangeNicknameEventPost(Sponge.server().causeStackManager().currentCause(),
                currentNickname, nickname, pl));
        final Optional<User> user = Sponge.server().userManager().load(pl).join();
        if (user.isPresent()) {
            final Optional<ServerPlayer> player = user.get().player();
            if (player.isPresent()) {
                player.get().sendMessage(LinearComponents.linear(
                        this.messageProviderService.getMessage("command.nick.success.base"),
                        Component.text(" - "),
                        nickname.color(NamedTextColor.WHITE)));
                player.get().offer(Keys.CUSTOM_NAME, nickname);
            } else {
                user.get().offer(Keys.CUSTOM_NAME, nickname);
            }
        }
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        final NicknameConfig nc = serviceCollection.configProvider().getModuleConfig(NicknameConfig.class);
        this.pattern = nc.getPattern();
        this.min = nc.getMinNicknameLength();
        this.max = nc.getMaxNicknameLength();
        this.prefix = LegacyComponentSerializer.legacyAmpersand().deserialize(nc.getPrefix());
    }

    private void stripPermissionless(final Subject source, final Component message) throws NicknameException {
        final Collection<String> strings = this.textStyleService.wouldStrip(
                Arrays.asList(NicknamePermissions.NICKNAME_COLOUR, NicknamePermissions.NICKNAME_COLOR),
                NicknamePermissions.NICKNAME_STYLE,
                source,
                LegacyComponentSerializer.legacyAmpersand().serialize(message));
        if (!strings.isEmpty()) {
            throw new NicknameException(this.messageProviderService.getMessage("command.nick.nopermscolourstyle", String.join(", ", strings)),
                    NicknameException.Type.INVALID_STYLE_OR_COLOUR);
        }
    }

}
