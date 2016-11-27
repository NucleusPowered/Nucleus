/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fly.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.annotations.SkipOnError;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.RideEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;

/**
 * This will only work in later Sponge API 5 versions.
 */
@SkipOnError
public class RideEntityFlyListener extends ListenerBase {

    @Inject
    private UserDataManager ucl;
    @Inject private CoreConfigAdapter cca;

    @Listener
    public void onPlayerDismount(RideEntityEvent.Dismount event, @Root Player player) {
        // If I'm right, this will work around Pixelmon when dismounting pokemon.
        if (FlyListener.shouldIgnoreFromGameMode(player)) {
            return;
        }

        try {
            ucl.get(player).ifPresent(x -> player.offer(Keys.CAN_FLY, x.isFlyingSafe()));
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }
        }
    }
}
