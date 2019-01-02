/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.freezeplayer.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.modules.freezeplayer.FreezePlayerKeys;
import org.spongepowered.api.entity.living.player.User;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.inject.Singleton;

@Singleton
public class FreezeService implements ServiceBase {

    private final LoadingCache<UUID, Boolean> cache = Caffeine.newBuilder().expireAfterAccess(2, TimeUnit.MINUTES)
            .build(uuid -> Nucleus.getNucleus().getStorageManager().getUserService().getOnThread(uuid)
                    .flatMap(x -> x.get(FreezePlayerKeys.FREEZE_PLAYER)).orElse(false));

    public boolean getFromUUID(@Nonnull UUID uuid) {
        Boolean b = this.cache.get(uuid);
        return b != null ? b : false;
    }

    public void invalidate(@Nonnull UUID uuid) {
        this.cache.invalidate(uuid);
    }

}
