/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.core.event.NucleusFirstJoinEvent;
import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.core.core.CoreKeys;
import io.github.nucleuspowered.nucleus.core.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.core.core.events.NucleusOnLoginEvent;
import io.github.nucleuspowered.nucleus.core.core.events.OnFirstLoginEvent;
import io.github.nucleuspowered.nucleus.core.core.events.UserDataLoadedEvent;
import io.github.nucleuspowered.nucleus.core.core.services.UniqueUserService;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.storage.query.IUserQueryObject;
import io.github.nucleuspowered.nucleus.core.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.util.AdventureUtils;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.services.IStorageService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.scheduler.Task;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CoreListener implements IReloadableService.Reloadable, ListenerBase {

    private final INucleusServiceCollection serviceCollection;
    @Nullable private NucleusTextTemplate getKickOnStopMessage = null;
    @Nullable private final URL url;
    private boolean warnOnWildcard = true;
    private boolean checkSponge = false;

    @Inject
    public CoreListener(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        URL u = null;
        try {
            u = new URL("https://ore.spongepowered.org/Nucleus/Nucleus/pages/The-Permissions-Wildcard-(And-Why-You-Shouldn't-Use-It)");
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        }
        this.url = u;
    }

    @Listener(order = Order.POST)
    public void onPlayerAuth(final ServerSideConnectionEvent.Auth event) {
        final UUID userId = event.profile().uniqueId();
        if (userId == null) { // it could be, I guess?
            return;
        }

        // Create user data if required, and place into cache.
        // As this is already async, load on thread.
        final IUserDataObject dataObject = this.serviceCollection.storageManager().getUserService().getOrNewOnThread(userId);

        // Fire the event, which will be async too, perhaps unsurprisingly.
        // The main use for this will be migrations.
        final UserDataLoadedEvent eventToFire = new UserDataLoadedEvent(
                event.cause().with(this.serviceCollection.pluginContainer()),
                dataObject,
                event.profile()
        );
        Sponge.eventManager().post(eventToFire);
        if (eventToFire.shouldSave()) {
            this.serviceCollection.storageManager().getUserService().save(userId, dataObject);
        }
        this.serviceCollection.messageProvider().invalidateLocaleCacheFor(userId);
    }

    /* (non-Javadoc)
     * We do this last to avoid interfering with other modules.
     */
    @Listener(order = Order.LATE)
    public void onPlayerLoginLast(final ServerSideConnectionEvent.Login event, @Getter("profile") final GameProfile profile,
        @Getter("user") final User user) {

        final IUserDataObject udo = this.serviceCollection.storageManager().getUserService().getOrNewOnThread(user.uniqueId());

        if (event.fromLocation().equals(event.toLocation())) {
            try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
                frame.pushCause(profile);
                // Check this
                final NucleusOnLoginEvent onLoginEvent = new NucleusOnLoginEvent(frame.currentCause(), user, udo, event.fromLocation());

                Sponge.eventManager().post(onLoginEvent);
                if (onLoginEvent.getTo().isPresent()) {
                    event.setToLocation(onLoginEvent.getTo().get());
                }
            }
        }

        this.serviceCollection.userCacheService().updateCacheForPlayer(user.uniqueId(), udo);
    }

    /* (non-Javadoc)
     * We do this first to try to get the first play status as quick as possible.
     */
    @Listener(order = Order.FIRST)
    public void onPlayerJoinFirst(final ServerSideConnectionEvent.Join event, @Getter("player") final ServerPlayer player) {
        try {
            final IUserDataObject qsu = this.serviceCollection.storageManager().getUserService().getOrNewOnThread(player.uniqueId());
            qsu.set(CoreKeys.LAST_LOGIN, Instant.now());
            if (this.serviceCollection.platformService().isServer()) {
                qsu.set(CoreKeys.IP_ADDRESS, player.connection().address().getAddress().toString());
            }

            // We'll do this bit shortly - after the login events have resolved.
            final String name = player.name();
            Task.builder().execute(() -> qsu.set(CoreKeys.LAST_KNOWN_NAME, name)).delay(Duration.ofSeconds(1)).plugin(this.serviceCollection.pluginContainer()).build();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Listener
    public void onPlayerJoinLast(final ServerSideConnectionEvent.Join event, @Getter("player") final ServerPlayer player) {
        // created before
        final UUID uuid = player.uniqueId();
        final IStorageService.Keyed.KeyedData<UUID, IUserQueryObject, IUserDataObject> userService =
                this.serviceCollection.storageManager().getUserService();
        if (!userService
                .getOnThread(uuid)
                .flatMap(x -> x.get(CoreKeys.FIRST_JOIN_PROCESSED))
                .orElse(false)) {

            if (!this.checkSponge || !Util.hasPlayedBeforeSponge(player.user())) {
                this.serviceCollection.getServiceUnchecked(UniqueUserService.class).resetUniqueUserCount();

                final NucleusFirstJoinEvent firstJoinEvent = new OnFirstLoginEvent(
                        event.cause(), player, event.originalAudience(), event.audience().orElse(null), event.originalMessage(),
                        event.isMessageCancelled());

                Sponge.eventManager().post(firstJoinEvent);
                event.setAudience(firstJoinEvent.audience().get());
                event.setMessageCancelled(firstJoinEvent.isMessageCancelled());
            }

            userService.getOrNew(player.uniqueId())
                    .thenAccept(x -> {
                        x.set(CoreKeys.FIRST_JOIN_PROCESSED, true);
                    });
        }

        // Warn about wildcard.
        if (player.hasPermission("nucleus")) {
            final IMessageProviderService provider = this.serviceCollection.messageProvider();
            this.serviceCollection.logger().warn("The player " + player.name() + " has got either the nucleus wildcard or the * wildcard "
                    + "permission. This may cause unintended side effects.");

            if (this.warnOnWildcard) {
                // warn
                final List<Component> text = new ArrayList<>();
                text.add(provider.getMessageFor(player, "core.permission.wildcard2"));
                text.add(provider.getMessageFor(player, "core.permission.wildcard3"));
                if (this.url != null) {
                    text.add(
                            Component.text().append(provider.getMessageFor(player, "core.permission.wildcard4"))
                                    .clickEvent(ClickEvent.openUrl(this.url)).build()
                    );
                }
                text.add(provider.getMessageFor(player, "core.permission.wildcard5"));
                Sponge.serviceProvider().paginationService()
                        .builder()
                        .title(provider.getMessageFor(player, "core.permission.wildcard"))
                        .contents(text)
                        .padding(Component.text("-", NamedTextColor.GOLD))
                        .sendTo(player);
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onPlayerQuit(final ServerSideConnectionEvent.Disconnect event, @Getter("player") final ServerPlayer player) {
        this.serviceCollection.storageManager().getUser(player.uniqueId()).thenAccept(x -> x.ifPresent(y -> this.onPlayerQuit(player, y)));
    }

    private void onPlayerQuit(final ServerPlayer player, final IUserDataObject udo) {
        final InetAddress address = player.connection().address().getAddress();

        try {
            udo.set(CoreKeys.IP_ADDRESS, address.toString());
            this.serviceCollection.userCacheService().updateCacheForPlayer(player.uniqueId(), udo);
            this.serviceCollection.storageManager().saveUser(player.uniqueId(), udo);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        final CoreConfig c = this.serviceCollection.configProvider().getModuleConfig(CoreConfig.class);
        this.getKickOnStopMessage = c.isKickOnStop() ?
                serviceCollection.textTemplateFactory().createFromAmpersandStringIgnoringExceptions(c.getKickOnStopMessage())
                    .orElseGet(NucleusTextTemplateImpl::empty) : null;
        this.warnOnWildcard = c.isCheckForWildcard();
        this.checkSponge = c.isCheckFirstDatePlayed();
    }

    @Listener
    public void onServerAboutToStop(final StoppingEngineEvent<Server> event) {
        for (final ServerPlayer player : Sponge.server().onlinePlayers()) {
            this.serviceCollection.storageManager().getUserOnThread(player.uniqueId()).ifPresent(x -> this.onPlayerQuit(player, x));
        }

        if (this.getKickOnStopMessage != null) {
            for (final ServerPlayer p : Sponge.server().onlinePlayers()) {
                final Component msg = this.getKickOnStopMessage.getForObject(p);
                if (AdventureUtils.isEmpty(msg)) {
                    p.kick();
                } else {
                    p.kick(msg);
                }
            }
        }

    }

    @Listener
    public void onGameReload(final RefreshGameEvent event) {
        final Audience requester = event.cause().first(Audience.class).orElse(Sponge.systemSubject());
        final IMessageProviderService messageProviderService = this.serviceCollection.messageProvider();
        try {
            this.serviceCollection.reloadableService().fireReloadables(this.serviceCollection);
            requester.sendMessage(Component.text().content("[Nucleus] ")
                            .color(NamedTextColor.YELLOW)
                            .append(messageProviderService.getMessageFor(requester, "command.reload.one")).build());
            requester.sendMessage(Component.text().content("[Nucleus] ")
                    .color(NamedTextColor.YELLOW)
                    .append(messageProviderService.getMessageFor(requester, "command.reload.two")).build());
        } catch (final Exception e) {
            e.printStackTrace();
            requester.sendMessage(
                    Component.text().content("[Nucleus] ")
                            .color(NamedTextColor.RED)
                            .append(messageProviderService.getMessageFor(requester, "command.reload.errorone")).build());
        }
    }
}
