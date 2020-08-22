/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.listener;

import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfig;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;

public class VanishSpongeWorkaroundListener implements ListenerBase.Conditional {

    @Listener
    public void onRespawnPlayer(RespawnPlayerEvent event,
            @Getter("getOriginalPlayer") Player originalPlayer,
            @Getter("getTargetEntity") Player newPlayer) {
        boolean o = originalPlayer.get(Keys.VANISH).orElse(false);
        boolean n = newPlayer.get(Keys.VANISH).orElse(false);
        if (o != n) {
            newPlayer.offer(Keys.VANISH, o);
        }
    }

    @Override
    public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(VanishConfig.class).isAttemptSpongeWorkaroundVanish();
    }
}
