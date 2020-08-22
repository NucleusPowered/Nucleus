/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.api.module.warning.NucleusWarningService;
import io.github.nucleuspowered.nucleus.api.module.warning.data.Warning;
import io.github.nucleuspowered.nucleus.modules.warn.WarnKeys;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import org.spongepowered.api.entity.living.player.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

@APIService(NucleusWarningService.class)
public class WarnHandler implements NucleusWarningService, ServiceBase {

    private final IStorageManager storageManager;

    @Inject
    public WarnHandler(INucleusServiceCollection serviceCollection) {
        this.storageManager = serviceCollection.storageManager();
    }

    public CompletableFuture<List<Warning>> getWarningsInternal(User user) {
        return this.storageManager
                .getUserService()
                .get(user.getUniqueId())
                .thenApply(y ->
                        y.flatMap(x -> x.get(WarnKeys.WARNINGS))
                            .<List<Warning>>map(Lists::newArrayList)
                            .orElseGet(ImmutableList::of));
    }

    @Override public CompletableFuture<List<Warning>> getWarnings(User user) {
        return getWarningsInternal(user);
    }

}
