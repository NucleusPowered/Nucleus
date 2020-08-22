/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.services;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.api.core.NucleusWorldUUIDChangeService;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.util.Tuples;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@APIService(NucleusWorldUUIDChangeService.class)
public class UUIDChangeService implements IReloadableService.Reloadable, NucleusWorldUUIDChangeService, ServiceBase {

    private Map<UUID, UUID> mapping = ImmutableMap.of();
    private boolean canLoad = false;

    @Override public Optional<UUID> getMappedUUID(UUID oldUUID) {
        return Optional.ofNullable(this.mapping.get(oldUUID));
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        if (!this.canLoad || !serviceCollection.platformService().isServer()) {
            return;
        }

        this.mapping = serviceCollection.moduleDataProvider().getModuleConfig(CoreConfig.class).getUuidMigration()
                .entrySet().stream()
                .map(x -> {
                    try {
                        UUID u = UUID.fromString(x.getValue());
                        return new Tuples.NullableTuple<>(x.getKey(), u);
                    } catch (Exception e) {
                        return new Tuples.NullableTuple<>(x.getKey(), Sponge.getServer().getWorldProperties(x.getValue())
                                .map(WorldProperties::getUniqueId).orElse(null));
                    }
                })
                .filter(x -> x.getSecond().isPresent())
                .collect(ImmutableMap.toImmutableMap(
                        x -> x.getFirst().get(),
                        x -> x.getSecond().get()));
    }

    public void setStateAndReload(INucleusServiceCollection serviceCollection) {
        this.canLoad = true;
        onReload(serviceCollection);
    }
}
