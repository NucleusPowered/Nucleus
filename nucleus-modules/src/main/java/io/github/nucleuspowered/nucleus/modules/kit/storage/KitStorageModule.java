/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.storage;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.services.SingleCachedService;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.IStorageModule;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess.IDataTranslator;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.persistence.IStorageRepositoryFactory;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.services.IStorageService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.DataContainer;

public final class KitStorageModule implements IStorageModule<IKitDataObject, IStorageService.SingleCached<IKitDataObject>,
        IStorageRepository.Single<DataContainer>, IDataTranslator<IKitDataObject, DataContainer>> {

    private final INucleusServiceCollection serviceCollection;
    private final IStorageService.SingleCached<IKitDataObject> kitsService;
    private IStorageRepository.@Nullable Single<DataContainer> repository;

    @Inject
    public KitStorageModule(final INucleusServiceCollection serviceCollection) {
        this.kitsService = new SingleCachedService<>(
                this::getRepository,
                this::getDataTranslator,
                serviceCollection.pluginContainer(),
                c -> {},
                c -> {});
        this.serviceCollection = serviceCollection;
    }

    @Override
    public IStorageService.SingleCached<IKitDataObject> getService() {
        return this.kitsService;
    }

    @Override
    public IDataTranslator<IKitDataObject, DataContainer> getDataTranslator() {
        return KitDataTranslator.Holder.INSTANCE;
    }

    @Override
    public IStorageRepository.Single<DataContainer> getRepository() {
        if (this.repository == null) {
            this.repository = this.serviceCollection.storageManager().getFlatFileRepositoryFactory().kitsRepository();
        }
        return this.repository;
    }

    @Override
    public void setRepository(final IStorageRepositoryFactory factory) {
        this.repository = factory.kitsRepository();
    }

    @Override
    public void detach() {
        if (this.repository != null) {
            this.repository.shutdown();
        }
        this.repository = null;
    }

    public IKitDataObject getKits() {
        final IStorageService.SingleCached<IKitDataObject> gs = this.kitsService;
        return gs.getCached().orElseGet(() -> gs.getOrNew().join());
    }

}
