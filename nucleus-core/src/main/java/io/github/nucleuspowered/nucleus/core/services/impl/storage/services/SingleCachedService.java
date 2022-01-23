/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.services;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess.IDataTranslator;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IDataObject;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.plugin.PluginContainer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SingleCachedService<O extends IDataObject> implements IStorageService.SingleCached<O> {

    private final Supplier<O> createNew;
    private final Supplier<IStorageRepository.Single<DataContainer>> repositorySupplier;
    private final Supplier<IDataTranslator<O, DataContainer>> dataAccessSupplier;
    private final PluginContainer pluginContainer;
    private final Consumer<O> dataMigrator;
    private O cached = null;

    public SingleCachedService(
            final Supplier<IStorageRepository.Single<DataContainer>> repositorySupplier,
            final Supplier<IDataTranslator<O, DataContainer>> dataAccessSupplier,
            final PluginContainer pluginContainer,
            final Consumer<O> createVersion,
            final Consumer<O> dataMigrator) {
        this.pluginContainer = pluginContainer;
        this.repositorySupplier = repositorySupplier;
        this.dataAccessSupplier = dataAccessSupplier;
        this.dataMigrator = dataMigrator;
        this.createNew = () -> {
            final O create = this.dataAccessSupplier.get().createNew();
            createVersion.accept(create);
            return create;
        };
    }

    @Override
    public O createNew() {
        return this.createNew.get();
    }

    @Override
    public CompletableFuture<Void> ensureSaved() {
        return ServicesUtil.run(() -> {
            if (this.cached != null) {
                this.save(this.cached);
            }
            return null;
        }, this.pluginContainer);
    }

    @Override
    public void saveCached() {
        this.ensureSaved();
    }

    @Override
    public CompletableFuture<Void> reload() {
        return ServicesUtil.run(this::getFromRepo, this.pluginContainer).thenApply(x -> null);
    }

    @Override
    public Optional<O> getCached() {
        return Optional.ofNullable(this.cached);
    }

    @Override
    public CompletableFuture<Optional<O>> get() {
        if (this.cached != null) {
            return CompletableFuture.completedFuture(Optional.of(this.cached));
        }

        return ServicesUtil.run(this::getFromRepo, this.pluginContainer);
    }

    @Override
    public Optional<O> getOnThread() {
        if (this.cached != null) {
            return Optional.of(this.cached);
        }

        try {
            return this.getFromRepo();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<O> getFromRepo() throws Exception {
        final Optional<DataContainer> gdo = this.repositorySupplier.get().get();
        if (gdo.isPresent()) {
            final O r = this.dataAccessSupplier.get().fromDataAccessObject(gdo.get());
            this.dataMigrator.accept(r);
            this.cached = r;
            return Optional.of(r);
        }
        return Optional.empty();
    }

    @Override
    public CompletableFuture<O> getOrNew() {
        final CompletableFuture<O> d = IStorageService.SingleCached.super.getOrNew();
        d.whenComplete((r, x) -> {
            if (r != null) {
                this.cached = r;
            }
        });
        return d;
    }

    @Override
    public CompletableFuture<Void> save(@NonNull final O value) {
        return ServicesUtil.run(() -> {
            this.repositorySupplier.get().save(this.dataAccessSupplier.get().toDataAccessObject(value));
            this.cached = value;
            return null;
        }, this.pluginContainer);
    }

    @Override
    public CompletableFuture<Void> clearCache() {
        this.cached = null;
        if (this.repositorySupplier.get().hasCache()) {
            return ServicesUtil.run(() -> {
                this.repositorySupplier.get().clearCache();
                return null;
            }, this.pluginContainer);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

}
