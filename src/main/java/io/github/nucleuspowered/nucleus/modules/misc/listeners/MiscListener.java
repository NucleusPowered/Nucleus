/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.Getter;

public class MiscListener extends ListenerBase {

    @Inject private UserDataManager ucl;
    @Inject private CoreConfigAdapter cca;

    // For /god
    @Listener
    public void onPlayerStruck(DamageEntityEvent event, @Getter("getTargetEntity") Player pl) {
        if (isInvulnerable(pl)) {
            pl.offer(Keys.FIRE_TICKS, 0);
            event.setBaseDamage(0);
            event.setCancelled(true);
        }
    }

    private boolean isInvulnerable(Player pl) {
        try {
            return ucl.getUser(pl).get().isInvulnerable();
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }

            return false;
        }
    }
}
