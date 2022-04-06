/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.message.event.NucleusMessageEvent;
import io.github.nucleuspowered.nucleus.api.module.mute.data.Mute;
import io.github.nucleuspowered.nucleus.modules.message.events.InternalNucleusHelpOpEvent;
import io.github.nucleuspowered.nucleus.modules.mute.MutePermissions;
import io.github.nucleuspowered.nucleus.modules.mute.config.MuteConfig;
import io.github.nucleuspowered.nucleus.modules.mute.services.MuteService;
import io.github.nucleuspowered.nucleus.modules.mute.services.NucleusMute;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import java.util.Optional;

public class MuteListener implements IReloadableService.Reloadable, ListenerBase {

    private final MuteService handler;
    private final IMessageProviderService messageProvider;
    private final IPermissionService permissionService;
    private MuteConfig muteConfig = new MuteConfig();

    @Inject
    public MuteListener(final INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(MuteService.class);
        this.messageProvider = serviceCollection.messageProvider();
        this.permissionService = serviceCollection.permissionService();
    }

    /**
     * At the time the subject joins, check to see if the subject is muted.
     *
     * @param event The event.
     */
    @Listener
    public void onPlayerLogin(final ServerSideConnectionEvent.Join event) {
        this.handler.onPlayerLogin(event.player());
    }

    @Listener
    public void onPlayerLogout(final ServerSideConnectionEvent.Disconnect event) {
        this.handler.clearCacheFor(event.player().uniqueId());
    }

    @Listener(order = Order.LATE)
    public void onChat(final PlayerChatEvent event, @Root final ServerPlayer player) {
        boolean cancel = false;
        if (this.isMutedNotify(player)) {
            Sponge.systemSubject().sendMessage(player,
                    LinearComponents.linear(
                            Component.text(player.name()),
                            Component.text(" ("),
                            this.messageProvider.getMessageFor(Sponge.systemSubject(), "standard.muted"),
                            Component.text("): "),
                            event.originalMessage()
                    ));
            cancel = true;
        }

        if (this.cancelOnGlobalMute(player, false)) {
            cancel = true;
        }

        if (cancel) {
            if (this.muteConfig.isShowMutedChat()) {
                // Send it to admins only.
                final Component m = LegacyComponentSerializer.legacyAmpersand().deserialize(this.muteConfig.getCancelledTag());
                final Component mutedMessage;
                if (m != Component.empty()) {
                    mutedMessage = LinearComponents.linear(m, event.originalMessage());
                } else {
                    mutedMessage = LinearComponents.linear(
                            Component.text(player.name()),
                            Component.text(" (muted): ", NamedTextColor.GRAY),
                            event.originalMessage());
                }

                this.permissionService.permissionMessageChannel(MutePermissions.MUTE_SEEMUTEDCHAT)
                        .sendMessage(player, mutedMessage, MessageType.SYSTEM);
            }

            event.setCancelled(true);
        }
    }

    private boolean isMutedNotify(final ServerPlayer player) {
        final Optional<Mute> mute = this.handler.getPlayerMuteInfo(player.uniqueId());
        if (mute.filter(x -> x instanceof NucleusMute).isPresent()) {
            this.handler.onMute((NucleusMute) mute.get(), player);
            return true;
        }
        return false;
    }

    @Listener
    public void onPlayerMessage(final NucleusMessageEvent event, @Getter("getSenderAsPlayer") final ServerPlayer source) {
        boolean isCancelled = false;
        if (this.isMutedNotify(source)) {
            isCancelled = true;
        }

        if (this.cancelOnGlobalMute(source, isCancelled)) {
            isCancelled = true;
        }
        event.setCancelled(isCancelled);
    }

    @Listener
    public void onPlayerHelpOp(final InternalNucleusHelpOpEvent event, @Root final ServerPlayer source) {
        if (this.isMutedNotify(source)) {
            event.setCancelled(true);
        }

        //noinspection IsCancelled
        if (this.cancelOnGlobalMute(source, event.isCancelled())) {
            event.setCancelled(true);
        }
    }

    private boolean cancelOnGlobalMute(final ServerPlayer player, final boolean isCancelled) {
        if (isCancelled || !this.handler.isGlobalMuteEnabled() || this.permissionService.hasPermission(player, MutePermissions.VOICE_AUTO)) {
            return false;
        }

        if (this.handler.isVoiced(player.uniqueId())) {
            return false;
        }

        this.messageProvider.sendMessageTo(player, "globalmute.novoice");
        return true;
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.muteConfig = serviceCollection.configProvider().getModuleConfig(MuteConfig.class);
    }

}
