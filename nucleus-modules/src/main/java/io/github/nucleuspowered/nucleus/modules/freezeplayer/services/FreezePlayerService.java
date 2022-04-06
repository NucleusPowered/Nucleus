/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.freezeplayer.services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.nucleuspowered.nucleus.api.module.freezeplayer.NucleusFreezePlayerService;
import io.github.nucleuspowered.nucleus.modules.freezeplayer.FreezePlayerKeys;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.IKeyedDataObject;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;

@APIService(NucleusFreezePlayerService.class)
public class FreezePlayerService implements ServiceBase, NucleusFreezePlayerService {

    private final INucleusServiceCollection serviceCollection;
    private final LoadingCache<UUID, Boolean> cache;

    @Inject
    public FreezePlayerService(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.cache = Caffeine.newBuilder().expireAfterAccess(2, TimeUnit.MINUTES)
                .build(uuid -> this.serviceCollection.storageManager().getUserService().getOnThread(uuid)
                        .flatMap(x -> x.get(FreezePlayerKeys.FREEZE_PLAYER)).orElse(false));
    }

    public boolean getFromUUID(final UUID uuid) {
        final Boolean b = this.cache.get(uuid);
        return b != null ? b : false;
    }

    public void invalidate(final UUID uuid) {
        this.cache.invalidate(uuid);
    }

    @Override
    public boolean isFrozen(final UUID uuid) {
        return this.getFromUUID(uuid);
    }

    @Override
    public void setFrozen(final UUID uuid, final boolean freeze) {
        final IUserDataObject x = this.serviceCollection.storageManager().getUserService().getOrNewOnThread(uuid);
        try (final IKeyedDataObject.Value<Boolean> v = x.getAndSet(FreezePlayerKeys.FREEZE_PLAYER)) {
            v.setValue(freeze);
            this.cache.put(uuid, freeze);
        }
    }

}
