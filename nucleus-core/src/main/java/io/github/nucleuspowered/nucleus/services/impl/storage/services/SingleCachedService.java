/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.storage.services;

import com.google.gson.JsonObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IDataVersioning;
import io.github.nucleuspowered.storage.dataaccess.IDataTranslator;
import io.github.nucleuspowered.storage.dataobjects.IDataObject;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.services.IStorageService;
import io.github.nucleuspowered.storage.services.ServicesUtil;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

public class SingleCachedService<O extends IDataObject> implements IStorageService.SingleCached<O> {

    private final Supplier<O> createNew;
    private final Supplier<IStorageRepository.Single<JsonObject>> repositorySupplier;
    private final Supplier<IDataTranslator<O, JsonObject>> dataAccessSupplier;
    private final PluginContainer pluginContainer;
    private final Consumer<O> dataMigrator;
    private O cached = null;

    public SingleCachedService(
            final Supplier<IStorageRepository.Single<JsonObject>> repositorySupplier,
            final Supplier<IDataTranslator<O, JsonObject>> dataAccessSupplier,
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
                save(this.cached);
            }
            return null;
        }, this.pluginContainer);
    }

    @Override
    public void saveCached() {
        ensureSaved();
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
            return getFromRepo();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<O> getFromRepo() throws Exception {
        Optional<O> gdo = this.repositorySupplier.get().get().map(x -> this.dataAccessSupplier.get().fromDataAccessObject(x));
        gdo.ifPresent(x -> {
            this.dataMigrator.accept(x);
            this.cached = x;
        });
        return gdo;
    }

    @Override
    public CompletableFuture<O> getOrNew() {
        CompletableFuture<O> d = IStorageService.SingleCached.super.getOrNew();
        d.whenComplete((r, x) -> {
            if (r != null) {
                this.cached = r;
            }
        });
        return d;
    }

    @Override
    public CompletableFuture<Void> save(@Nonnull O value) {
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
