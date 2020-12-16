/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.storage.dataaccess.IDataTranslator;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;
import io.github.nucleuspowered.storage.dataobjects.keyed.IKeyedDataObject;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.queryobjects.IQueryObject;
import io.github.nucleuspowered.storage.util.KeyedObject;
import io.github.nucleuspowered.storage.util.ThrownBiConsumer;
import io.github.nucleuspowered.storage.util.ThrownFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractKeyedService<Q extends IQueryObject<UUID, Q>, D extends IKeyedDataObject<D>>
        implements IStorageService.Keyed.KeyedData<UUID, Q, D> {

    private final LoadingCache<UUID, ReentrantReadWriteLock> dataLocks =
            Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build(new CacheLoader<UUID, ReentrantReadWriteLock>() {
                @NonNull
                @Override
                public ReentrantReadWriteLock load(@NonNull UUID key) {
                    return new ReentrantReadWriteLock();
                }
            });
    private final Cache<UUID, D> cache = Caffeine
            .newBuilder()
            .removalListener(this::onRemoval)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    private final Supplier<IStorageRepository.Keyed<UUID, Q, ?>> storageRepositorySupplier;
    private final Supplier<D> createNew;
    private final ThrownBiConsumer<UUID, D, Exception> save;
    private final ThrownFunction<Q, Map<UUID, D>, Exception> getAll;
    private final ThrownFunction<Q, Optional<KeyedObject<UUID, D>>, Exception> getQuery;
    private final ThrownFunction<UUID, Optional<D>, Exception> get;
    private final PluginContainer pluginContainer;
    private final Consumer<D> upgrader;
    private final Consumer<D> versionSetter;

    public <O> AbstractKeyedService(
        Supplier<IDataTranslator<D, O>> dts,
        Supplier<IStorageRepository.Keyed<UUID, Q, O>> srs,
        Consumer<D> upgrader,
        Consumer<D> versionSetter,
        PluginContainer pluginContainer
    ) {
        this(
                () -> dts.get().createNew(),
                (key, udo) -> srs.get().save(
                        key,
                        dts.get().toDataAccessObject(udo)
                ),
                query -> srs.get()
                        .getAll(query)
                        .entrySet().stream()
                        .filter(x -> x.getValue() != null)
                        .collect(
                                ImmutableMap.toImmutableMap(
                                        Map.Entry::getKey,
                                        x -> dts.get().fromDataAccessObject(x.getValue())
                                )
                        ),
                uuid -> srs.get().get(uuid).map(dts.get()::fromDataAccessObject),
                query -> srs.get().get(query).map(x -> x.mapValue(dts.get()::fromDataAccessObject)),
                srs::get,
                upgrader,
                versionSetter,
                pluginContainer);
    }

    private AbstractKeyedService(
            Supplier<D> createNew,
            ThrownBiConsumer<UUID, D, Exception> save,
            ThrownFunction<Q, Map<UUID, D>, Exception> getAll,
            ThrownFunction<UUID, Optional<D>, Exception> get,
            ThrownFunction<Q, Optional<KeyedObject<UUID, D>>, Exception> getQuery,
            Supplier<IStorageRepository.Keyed<UUID, Q, ?>> storageRepositorySupplier,
            Consumer<D> upgrader,
            Consumer<D> versionSetter,
            PluginContainer pluginContainer
    ) {
        this.pluginContainer = pluginContainer;
        this.createNew = createNew;
        this.save = save;
        this.getAll = getAll;
        this.get = get;
        this.getQuery = getQuery;
        this.upgrader = upgrader;
        this.versionSetter = versionSetter;
        this.storageRepositorySupplier = storageRepositorySupplier;
    }

    public D createNew() {
        final D data = this.createNew.get();
        this.versionSetter.accept(data);
        return data;
    }

    @Override
    public CompletableFuture<Void> clearCache() {
        this.cache.invalidateAll();
        return ServicesUtil.run(() -> {
            this.storageRepositorySupplier.get().clearCache();
            return null;
        }, this.pluginContainer);
    }

    @Override
    public CompletableFuture<Void> clearCacheUnless(final Set<UUID> keysToKeep) {
        final Set<UUID> keysToRemove = this.cache.asMap().keySet().stream().filter(x -> !keysToKeep.contains(x)).collect(Collectors.toSet());
        this.cache.invalidateAll(keysToRemove);
        return ServicesUtil.run(() -> {
            this.storageRepositorySupplier.get().clearCache(keysToRemove);
            return null;
        }, this.pluginContainer);
    }

    @Override
    public CompletableFuture<Optional<D>> get(@NonNull final UUID key) {
        ReentrantReadWriteLock.ReadLock lock = this.dataLocks.get(key).readLock();
        try {
            lock.lock();
            D result = this.cache.getIfPresent(key);
            if (result != null) {
                return CompletableFuture.completedFuture(Optional.of(result));
            }
        } finally {
            lock.unlock();
        }

        return ServicesUtil.run(() -> getFromRepo(key), this.pluginContainer);
    }

    @Override
    public CompletableFuture<D> getOrNew(@Nonnull final UUID key) {
        return get(key).thenApply(d -> d.orElseGet(() -> {
            D result = createNew();
            save(key, result);
            return result;
        }));
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public Optional<D> getOnThread(@NonNull UUID key) {
        // Read lock for the cache
        ReentrantReadWriteLock.ReadLock lock = this.dataLocks.get(key).readLock();
        try {
            lock.lock();
            D result = this.cache.getIfPresent(key);
            if (result != null) {
                return Optional.of(result);
            }
        } finally {
            lock.unlock();
        }

        try {
            return getFromRepo(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private Optional<D> getFromRepo(@NonNull UUID key) throws Exception {
        // Write lock because of the cache
        ReentrantReadWriteLock.WriteLock lock = this.dataLocks.get(key).writeLock();
        try {
            lock.lock();
            Optional<D> r = this.get.apply(key);
            r.ifPresent(d -> {
                this.upgrader.accept(d);
                this.cache.put(key, d);
            });
            return r;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompletableFuture<Optional<KeyedObject<UUID, D>>> get(@NonNull final Q query) {
        return ServicesUtil.run(() -> {
            Optional<KeyedObject<UUID, D>> r = this.getQuery.apply(query);
            r.ifPresent(d -> {
                if (d.getValue().isPresent()) {
                    this.cache.put(d.getKey(), d.getValue().get());
                } else {
                    this.cache.invalidate(d.getKey());
                }
            });
            return r;
        }, this.pluginContainer);
    }

    @Override
    public CompletableFuture<Map<UUID, D>> getAll(@NonNull Q query) {
        return ServicesUtil.run(() -> {
            Map<UUID, D> res = this.getAll.apply(query);
            res.forEach(this.cache::put);
            return res;
        }, this.pluginContainer);
    }

    @Override
    public CompletableFuture<Boolean> exists(@NonNull UUID key) {
        return ServicesUtil.run(() -> this.storageRepositorySupplier.get().exists(key), this.pluginContainer);
    }

    @Override
    public CompletableFuture<Integer> count(@NonNull Q query) {
        return ServicesUtil.run(() -> this.storageRepositorySupplier.get().count(query), this.pluginContainer);
    }

    @Override
    public <T2> CompletableFuture<Void> setAndSave(@NonNull final UUID key, final DataKey<T2, ? extends D> dataKey, final T2 data) {
        return getOrNew(key).thenAccept(x -> {
            x.set(dataKey, data);
            try {
                this.saveOnThread(key, x);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public <T2> CompletableFuture<Void> removeAndSave(@NonNull UUID key, DataKey<T2, ? extends D> dataKey) {
        return this.getOrNew(key).handle((x, ex) -> {
            x.remove(dataKey);
            try {
                this.saveOnThread(key, x);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> save(@NonNull final UUID key, @NonNull final D value) {
        return ServicesUtil.run(() -> {
            this.saveOnThread(key, value);
            return null;
        }, this.pluginContainer);
    }

    private void saveOnThread(@NonNull final UUID key, @NonNull final D value) throws Exception {
        ReentrantReadWriteLock reentrantReadWriteLock = this.dataLocks.get(key);
        ReentrantReadWriteLock.WriteLock lock = reentrantReadWriteLock.writeLock();
        try {
            lock.lock();
            this.cache.put(key, value);
            this.save.apply(key, value);
            value.markDirty(false);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompletableFuture<Void> delete(@NonNull UUID key) {
        return ServicesUtil.run(() -> {
            ReentrantReadWriteLock reentrantReadWriteLock = this.dataLocks.get(key);
            ReentrantReadWriteLock.WriteLock lock = reentrantReadWriteLock.writeLock();
            try {
                lock.lock();
                this.storageRepositorySupplier.get().delete(key);
                final D o = this.cache.getIfPresent(key);
                if (o != null) {
                    o.markDirty(false); // don't want to save it
                }
                this.cache.invalidate(key);
                return null;
            } finally {
                lock.unlock();
            }
        }, this.pluginContainer);
    }

    @Override
    public CompletableFuture<Void> ensureSaved() {
        return ServicesUtil.run(() -> {
            for (final Map.Entry<UUID, D> objectToSave : new HashMap<>(this.cache.asMap()).entrySet()) {
                if (objectToSave.getValue() != null && objectToSave.getValue().isDirty()) {
                    this.save(objectToSave.getKey(), objectToSave.getValue());
                }
            }
            return null;
        }, this.pluginContainer);
    }

    void onRemoval(@Nullable UUID uuid, @Nullable D dataObject, @Nonnull RemovalCause removalCause) {
        // If evicted normally, make sure it's saved.
        if (removalCause.wasEvicted() && uuid != null && dataObject != null && dataObject.isDirty()) {
            this.save(uuid, dataObject);
        }
    }
}
