/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.storage.services;

import io.github.nucleuspowered.nucleus.storage.INucleusStorageManager;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IGeneralDataObject;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.services.ServicesUtil;
import io.github.nucleuspowered.storage.util.ThrownConsumer;
import io.github.nucleuspowered.storage.util.ThrownSupplier;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

public class GeneralService implements IGeneralDataService {

    private final Supplier<IGeneralDataObject> createNew;
    private final ThrownSupplier<Optional<IGeneralDataObject>, Exception> get;
    private final ThrownConsumer<IGeneralDataObject, Exception> save;
    private final Supplier<IStorageRepository.Single<?>> repositorySupplier;

    private IGeneralDataObject cached = null;

    public <O> GeneralService(INucleusStorageManager<O> storageManager) {
        this.createNew = () -> storageManager.getGeneralDataAccess().createNew();
        this.get = () -> storageManager.getGeneralRepository().get()
                .map(x -> storageManager.getGeneralDataAccess().fromDataAccessObject(x));
        this.save = r -> storageManager.getGeneralRepository().save(storageManager.getGeneralDataAccess().toDataAccessObject(r));
        this.repositorySupplier = storageManager::getGeneralRepository;
    }

    @Override
    public IGeneralDataObject createNew() {
        return this.createNew.get();
    }

    @Override
    public CompletableFuture<Void> ensureSaved() {
        return null;
    }

    @Override
    public Optional<IGeneralDataObject> getCached() {
        return Optional.ofNullable(this.cached);
    }

    @Override
    public CompletableFuture<Optional<IGeneralDataObject>> get() {
        if (this.cached != null) {
            return CompletableFuture.completedFuture(Optional.of(this.cached));
        }

        return ServicesUtil.run(this::getFromRepo);
    }

    @Override
    public Optional<IGeneralDataObject> getOnThread() {
        if (this.cached != null) {
            return Optional.of(this.cached);
        }

        try {
            return getFromRepo();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<IGeneralDataObject> getFromRepo() throws Exception {
        Optional<IGeneralDataObject> gdo = this.get.get();
        gdo.ifPresent(x -> this.cached = x);
        return gdo;
    }

    @Override
    public CompletableFuture<IGeneralDataObject> getOrNew() {
        CompletableFuture<IGeneralDataObject> d = IGeneralDataService.super.getOrNew();
        d.whenComplete((r, x) -> {
            if (r != null) {
                this.cached = r;
            }
        });
        return d;
    }

    @Override
    public CompletableFuture<Void> save(@Nonnull IGeneralDataObject value) {
        return ServicesUtil.run(() -> {
            this.save.save(value);
            this.cached = value;
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> clearCache() {
        this.cached = null;
        if (this.repositorySupplier.get().hasCache()) {
            return ServicesUtil.run(() -> {
                this.repositorySupplier.get().clearCache();
                return null;
            });
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }
}
