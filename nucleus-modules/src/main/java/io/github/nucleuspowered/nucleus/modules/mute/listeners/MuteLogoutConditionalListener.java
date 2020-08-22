/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.listeners;

import io.github.nucleuspowered.nucleus.modules.mute.MuteKeys;
import io.github.nucleuspowered.nucleus.modules.mute.config.MuteConfig;
import io.github.nucleuspowered.nucleus.modules.mute.data.MuteData;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import io.github.nucleuspowered.storage.dataobjects.keyed.IKeyedDataObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.util.Optional;
import java.util.UUID;

public class MuteLogoutConditionalListener implements ListenerBase.Conditional {

    private final IStorageManager storageManager;

    public MuteLogoutConditionalListener(INucleusServiceCollection serviceCollection) {
        this.storageManager = serviceCollection.storageManager();
    }

    @Listener
    public void onLogout(ClientConnectionEvent.Disconnect event, @Getter("getTargetEntity") Player player) {
        final UUID uuid = player.getUniqueId();
        this.storageManager
                .getUserService()
                .getOrNew(uuid)
                .thenAccept(x -> {
                    if (!Sponge.getServer().getPlayer(uuid).map(User::isOnline).orElse(false)) {
                        try (IKeyedDataObject.Value<MuteData> value = x.getAndSet(MuteKeys.MUTE_DATA)) {
                            Optional<MuteData> data = value.getValue();
                            if (data.isPresent()) {
                                MuteData muteData = data.get();
                                muteData.getRemainingTime().ifPresent(muteData::setTimeFromNextLogin);
                            }
                        }

                        this.storageManager.getUserService().save(uuid, x);
                    }
                });
    }

    @Override
    public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(MuteConfig.class).isMuteOnlineOnly();
    }

}
