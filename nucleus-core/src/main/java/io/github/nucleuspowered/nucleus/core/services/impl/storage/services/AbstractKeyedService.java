/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess.IDataTranslator;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.IKeyedDataObject;
import io.github.nucleuspowered.storage.exceptions.DataLoadException;
import io.github.nucleuspowered.storage.exceptions.DataQueryException;
import io.github.nucleuspowered.storage.exceptions.DataSaveException;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.query.IQueryObject;
import io.github.nucleuspowered.storage.util.KeyedObject;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.plugin.PluginContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class AbstractKeyedService<K, Q extends IQueryObject<K, Q>, D extends IKeyedDataObject<D>, O>
        implements IStorageService.Keyed.KeyedData<K, Q, D> {

    private final LoadingCache<K, ReentrantReadWriteLock> dataLocks =
            Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build(new CacheLoader<K, ReentrantReadWriteLock>() {
                @NonNull
                @Override
                public ReentrantReadWriteLock load(@NonNull final K key) {
                    return new ReentrantReadWriteLock();
                }
            });
    private final Cache<K, D> cache = Caffeine.newBuilder()
            .removalListener(this::onRemoval)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    private final Supplier<IDataTranslator<D, O>> dataTranslator;
    private final Supplier<IStorageRepository.Keyed<K, Q, O>> storageRepositorySupplier;
    private final PluginContainer pluginContainer;
    private final Consumer<D> upgrader;
    private final Consumer<D> versionSetter;

    public AbstractKeyedService(
            final Supplier<IDataTranslator<D, O>> dts,
            final Supplier<IStorageRepository.Keyed<K, Q, O>> storageRepositorySupplier,
            final Consumer<D> upgrader,
            final Consumer<D> versionSetter,
            final PluginContainer pluginContainer
    ) {
        this.pluginContainer = pluginContainer;
        this.dataTranslator = dts;
        this.upgrader = upgrader;
        this.versionSetter = versionSetter;
        this.storageRepositorySupplier = storageRepositorySupplier;
    }

    protected D createNewDataObject() {
        return this.dataTranslator.get().createNew();
    }

    protected void saveObject(final K key, final D object) throws DataSaveException {
        this.storageRepositorySupplier.get().save(key, this.dataTranslator.get().toDataAccessObject(object));
    }

    protected Map<K, D> getAllFromQuery(final Q query) throws DataQueryException, DataLoadException {
        return Stream.ofAll(this.storageRepositorySupplier.get().getAll(query).entrySet())
                .flatMap(x -> Try.of(() -> new Tuple2<>(x.getKey(), this.dataTranslator.get().fromDataAccessObject(x.getValue()))))
                .collect(
                        Collectors.toMap(
                                x -> x._1,
                                x -> x._2
                        )
                );
    }

    protected Optional<D> getFromKey(final K key) throws DataQueryException, DataLoadException {
        final Optional<O> o = this.storageRepositorySupplier.get().get(key);
        if (o.isPresent()) {
            return Optional.of(this.dataTranslator.get().fromDataAccessObject(o.get()));
        }
        return Optional.empty();
    }

    protected Optional<KeyedObject<K, D>> getFromQuery(final Q query) throws DataQueryException, DataLoadException {
        final Optional<KeyedObject<K, O>> o = this.storageRepositorySupplier.get().get(query);
        if (o.filter(x -> x.getValue().isPresent()).isPresent()) {
            final D value = this.dataTranslator.get().fromDataAccessObject(o.get().getValue().get());
            return o.map(x -> x.mapValue(d -> value));
        }
        return Optional.empty();
    }

    public D createNew() {
        final D data = this.createNewDataObject();
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
    public CompletableFuture<Void> clearCacheUnless(final Set<K> keysToKeep) {
        final Set<K> keysToRemove = this.cache.asMap().keySet().stream().filter(x -> !keysToKeep.contains(x)).collect(Collectors.toSet());
        this.cache.invalidateAll(keysToRemove);
        return ServicesUtil.run(() -> {
            this.storageRepositorySupplier.get().clearCache(keysToRemove);
            return null;
        }, this.pluginContainer);
    }

    @Override
    public CompletableFuture<Optional<D>> get(@NonNull final K key) {
        final ReentrantReadWriteLock.ReadLock lock = this.dataLocks.get(key).readLock();
        try {
            lock.lock();
            final D result = this.cache.getIfPresent(key);
            if (result != null) {
                return CompletableFuture.completedFuture(Optional.of(result));
            }
        } finally {
            lock.unlock();
        }

        return ServicesUtil.run(() -> this.getFromRepo(key), this.pluginContainer);
    }

    @Override
    public CompletableFuture<D> getOrNew(@NonNull final K key) {
        return get(key).thenApply(d -> d.orElseGet(() -> {
            final D result = createNew();
            save(key, result);
            return result;
        }));
    }

    @Override
    public Optional<D> getOnThread(@NonNull final K key) {
        // Read lock for the cache
        final ReentrantReadWriteLock.ReadLock lock = this.dataLocks.get(key).readLock();
        try {
            lock.lock();
            final D result = this.cache.getIfPresent(key);
            if (result != null) {
                return Optional.of(result);
            }
        } finally {
            lock.unlock();
        }

        try {
            return this.getFromRepo(key);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<D> getFromRepo(@NonNull final K key) throws Exception {
        // Write lock because of the cache
        final ReentrantReadWriteLock.WriteLock lock = this.dataLocks.get(key).writeLock();
        try {
            lock.lock();
            final Optional<D> r = this.getFromKey(key);
            r.ifPresent(d -> {
                this.upgrader.accept(d);
                this.cache.put(key, d);
            });
            return r;
        } catch (final Throwable e) {
            throw new DataLoadException("Could not get from repo", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompletableFuture<Optional<KeyedObject<K, D>>> get(@NonNull final Q query) {
        return ServicesUtil.run(() -> {
            final Optional<KeyedObject<K, D>> r = this.getFromQuery(query);
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
    public CompletableFuture<Map<K, D>> getAll(@NonNull final Q query) {
        return ServicesUtil.run(() -> {
            final Map<K, D> res = this.getAllFromQuery(query);//.apply(query);
            res.forEach(this.cache::put);
            return res;
        }, this.pluginContainer);
    }

    @Override
    public CompletableFuture<Boolean> exists(@NonNull final K key) {
        return ServicesUtil.run(() -> this.storageRepositorySupplier.get().exists(key), this.pluginContainer);
    }

    @Override
    public CompletableFuture<Integer> count(@NonNull final Q query) {
        return ServicesUtil.run(() -> this.storageRepositorySupplier.get().count(query), this.pluginContainer);
    }

    @Override
    public <T2> CompletableFuture<Void> setAndSave(@NonNull final K key, final DataKey<T2, ? extends D> dataKey, final T2 data) {
        return this.getOrNew(key).thenAccept(x -> {
            x.set(dataKey, data);
            try {
                this.saveOnThread(key, x);
            } catch (final Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public <T2> CompletableFuture<Void> removeAndSave(@NonNull final K key, final DataKey<T2, ? extends D> dataKey) {
        return this.getOrNew(key).handle((x, ex) -> {
            x.remove(dataKey);
            try {
                this.saveOnThread(key, x);
            } catch (final Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> save(@NonNull final K key, @NonNull final D value) {
        return ServicesUtil.run(() -> {
            this.saveOnThread(key, value);
            return null;
        }, this.pluginContainer);
    }

    @SuppressWarnings("ConstantConditions")
    private void saveOnThread(@NonNull final K key, @NonNull final D value) throws Exception {
        final ReentrantReadWriteLock reentrantReadWriteLock = this.dataLocks.get(key);
        final ReentrantReadWriteLock.WriteLock lock = reentrantReadWriteLock.writeLock();
        try {
            lock.lock();
            this.cache.put(key, value);
            this.saveObject(key, value);
            value.markDirty(false);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompletableFuture<Void> delete(@NonNull final K key) {
        return ServicesUtil.run(() -> {
            final ReentrantReadWriteLock reentrantReadWriteLock = this.dataLocks.get(key);
            final ReentrantReadWriteLock.WriteLock lock = reentrantReadWriteLock.writeLock();
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
            for (final Map.Entry<K, D> objectToSave : new HashMap<>(this.cache.asMap()).entrySet()) {
                if (objectToSave.getValue() != null && objectToSave.getValue().isDirty()) {
                    this.save(objectToSave.getKey(), objectToSave.getValue());
                }
            }
            return null;
        }, this.pluginContainer);
    }

    private void onRemoval(@Nullable final K key, @Nullable final D dataObject, @NonNull final RemovalCause removalCause) {
        // If evicted normally, make sure it's saved.
        if (removalCause.wasEvicted() && key != null && dataObject != null && dataObject.isDirty()) {
            this.save(key, dataObject);
        }
    }

    protected abstract void onEviction(final K key, final D dataObject, final BiConsumer<K, D> reAdd);
}
