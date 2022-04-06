/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.persistence;

import io.github.nucleuspowered.storage.exceptions.DataDeleteException;
import io.github.nucleuspowered.storage.exceptions.DataLoadException;
import io.github.nucleuspowered.storage.exceptions.DataQueryException;
import io.github.nucleuspowered.storage.exceptions.DataSaveException;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.query.IQueryObject;
import io.github.nucleuspowered.storage.util.KeyedObject;
import io.vavr.CheckedFunction1;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

abstract class FlatFileStorageRepository implements IStorageRepository {

    private final Logger logger;

    private static Optional<DataContainer> readFromFile(final Path path) throws IOException {
        if (Files.exists(path)) {
            try (final BufferedReader bufferedReader = Files.newBufferedReader(path)) {
                return Optional.of(DataFormats.JSON.get().readFrom(bufferedReader));
            }
        }
        return Optional.empty();
    }

    private static void writeToFile(final DataContainer container, final Path path) throws IOException {
        if (Files.exists(path)) {
            // make a backup
            Files.copy(path, path.resolveSibling(path.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.createDirectories(path.getParent());
        }
        try (final BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            DataFormats.JSON.get().writeTo(bufferedWriter, container);
        }
    }

    protected FlatFileStorageRepository(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean startup() {
        return true;
    }

    Optional<DataContainer> get(@Nullable final Path path) throws DataLoadException {
        if (path != null) {
            try {
                if (Files.size(path) == 0) {
                    return Optional.empty(); // nothing in the file, don't do anything with it.
                }
                // Read the file.
                return FlatFileStorageRepository.readFromFile(path);
            } catch (final Exception e) {
                throw new DataLoadException("Could not load file at " + path.toAbsolutePath(), e);
            }
        }

        return Optional.empty();
    }

    synchronized void save(final Path file, final DataContainer object) throws DataSaveException {
        try {
            FlatFileStorageRepository.writeToFile(object, file);
        } catch (final Exception ex) {
            this.logger.error("Could not save " + file.toString());
            ex.printStackTrace();
            throw new DataSaveException("Could not save " + file, ex);
        }
    }

    @Override
    public void shutdown() {
        // nothing to do
    }

    @Override public void clearCache() {
        // noop
    }

    @Override public boolean hasCache() {
        return false;
    }

    static class Single extends FlatFileStorageRepository implements IStorageRepository.Single<DataContainer> {

        private final Supplier<Path> FILENAME_RESOLVER;

        Single(final Logger logger, final Supplier<Path> filename_resolver) {
            super(logger);
            this.FILENAME_RESOLVER = filename_resolver;
        }

        @Override
        public Optional<DataContainer> get() throws DataLoadException {
            if (Files.exists(this.FILENAME_RESOLVER.get())) {
                return this.get(this.FILENAME_RESOLVER.get());
            }

            return Optional.empty();
        }

        @Override
        public void save(final DataContainer object) throws DataSaveException {
            this.save(this.FILENAME_RESOLVER.get(), object);
        }
    }

    abstract static class AbstractKeyed<K, Q extends IQueryObject<K, Q>>
            extends FlatFileStorageRepository
            implements Keyed<K, Q, DataContainer> {

        private final CheckedFunction1<Q, Path> FILENAME_RESOLVER;
        protected final Supplier<Path> BASE_PATH;
        private final Function<K, Path> KEY_FILENAME_RESOLVER;

        AbstractKeyed(
                final Logger logger,
                final CheckedFunction1<Q, Path> filename_resolver,
                final Function<K, Path> uuid_filename_resolver,
                final Supplier<Path> basePath) {
            super(logger);
            this.FILENAME_RESOLVER = filename_resolver;
            this.KEY_FILENAME_RESOLVER = uuid_filename_resolver;
            this.BASE_PATH = basePath;
        }

        @Override
        public boolean exists(final Q query) {
            return this.existsInternal(query).fold(thr -> {
                thr.printStackTrace();
                return false;
            }, Option::isDefined);
        }

        @Override
        public Optional<KeyedObject<K, DataContainer>> get(final Q query) throws DataLoadException {
            final Path path = this.existsInternal(query)
                    .getOrElseThrow(thr -> new DataLoadException("Query not valid", thr))
                    .orNull();

            return this.get(path).map(x -> new KeyedObject<>(query.keys().iterator().next(), x));
        }

        @Override
        public boolean exists(final K uuid) {
            return this.existsInternal(uuid) != null;
        }

        @Override
        public Optional<DataContainer> get(final K uuid) throws DataLoadException {
            return this.get(this.existsInternal(uuid));
        }

        @Override
        public Collection<K> getAllKeys() throws DataLoadException {
            return Collections.unmodifiableSet(this.getAllKeysInternal());
        }

        @Override
        public Map<K, DataContainer> getAll(final Q query) throws DataLoadException, DataQueryException {
            final HashMap<K, DataContainer> j = new HashMap<>();
            for (final K key : this.getAllKeys(query)) {
                j.put(key, this.get(key).get()); // should be there
            }

            return Collections.unmodifiableMap(j);
        }

        @Override
        public Collection<K> getAllKeys(final Q query) throws DataLoadException, DataQueryException {
            if (query.restrictedToKeys()) {
                this.getAllKeysInternal().retainAll(query.keys());
            }

            throw new DataQueryException("There must only a key", query);
        }

        protected abstract Set<K> getAllKeysInternal() throws DataLoadException;

        @Nullable
        private Path existsInternal(final K uuid) {
            final Path path = this.KEY_FILENAME_RESOLVER.apply(uuid);
            if (Files.exists(this.KEY_FILENAME_RESOLVER.apply(uuid))) {
                return path;
            }

            return null;
        }


        @Override
        public int count(final Q query) {
            return this.exists(query) ? 1 : 0;
        }

        @Override
        public void save(final K key, final DataContainer object) throws DataSaveException {
            final Path file = this.KEY_FILENAME_RESOLVER.apply(key);
            this.save(file, object);
        }

        @Override
        public void delete(final K key) throws DataDeleteException {
            final Path filename = this.KEY_FILENAME_RESOLVER.apply(key);

            try {
                Files.delete(filename);
            } catch (final IOException e) {
                throw new DataDeleteException("Could not delete " + filename, e);
            }
        }

        private Either<Throwable, Option<Path>> existsInternal(final Q query) {
            return Try.of(() -> {
                final Path path = this.FILENAME_RESOLVER.apply(query);
                if (Files.exists(this.FILENAME_RESOLVER.apply(query))) {
                    return Option.of(path);
                }

                return Option.<Path>none();
            }).toEither();
        }
    }

    // ** WORLD

    final static class ResourceKeyed<Q extends IQueryObject<ResourceKey, Q>> extends AbstractKeyed<ResourceKey, Q> {

        ResourceKeyed(final Logger logger,
                final CheckedFunction1<Q, Path> filename_resolver,
                final Function<ResourceKey, Path> uuid_filename_resolver, final Supplier<Path> basePath) {
            super(logger, filename_resolver, uuid_filename_resolver, basePath);
        }

        @Override
        protected Set<ResourceKey> getAllKeysInternal() throws DataLoadException {
            final FileWalker u = new FileWalker();
            try {
                Files.walkFileTree(this.BASE_PATH.get(), u);
                return u.keys;
            } catch (final IOException e) {
                throw new DataLoadException("Could not walk the file tree", e);
            }
        }

        @Override
        public void clearCache(final Iterable<ResourceKey> keys) {
            // no-op here
        }

        private static class FileWalker extends SimpleFileVisitor<Path> {

            private final Set<ResourceKey> keys = new HashSet<>();
            private Option<String> inDirectory;

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                if (this.inDirectory.isEmpty()) {
                    final FileVisitResult result = super.preVisitDirectory(dir, attrs);
                    if (result == FileVisitResult.CONTINUE) {
                        this.inDirectory = Option.some(dir.getFileName().toString());
                    }
                    return result;
                }

                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                this.inDirectory = null;
                return super.postVisitDirectory(dir, exc);
            }

            // Print information about
            // each type of file.
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attr) {
                if (attr.isRegularFile()) {
                    if (file.endsWith(".json")) {
                        final String f = file.getFileName().toString();
                        try {
                            this.keys.add(ResourceKey.of(this.inDirectory.getOrElseThrow(RuntimeException::new), f.replace(".json", "")));
                        } catch (final Exception e) {
                            // ignored
                        }
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        }

    }

    // ** USER

    final static class UUIDKeyed<Q extends IQueryObject<UUID, Q>> extends AbstractKeyed<UUID, Q> {

        UUIDKeyed(final Logger logger,
                final CheckedFunction1<Q, Path> filename_resolver,
                final Function<UUID, Path> uuid_filename_resolver, final Supplier<Path> basePath) {
            super(logger, filename_resolver, uuid_filename_resolver, basePath);
        }

        @Override
        protected Set<UUID> getAllKeysInternal() throws DataLoadException {
            final UUIDFileWalker u = new UUIDFileWalker();
            try {
                Files.walkFileTree(this.BASE_PATH.get(), u);
                return u.uuidSet;
            } catch (final IOException e) {
                throw new DataLoadException("Could not walk the file tree", e);
            }
        }

        @Override
        public void clearCache(final Iterable<UUID> keys) {
            // no-op here
        }

        private static class UUIDFileWalker extends SimpleFileVisitor<Path> {

            private final Set<UUID> uuidSet = new HashSet<>();

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                if (dir.getFileName().toString().length() == 2) {
                    return super.preVisitDirectory(dir, attrs);
                }

                return FileVisitResult.SKIP_SUBTREE;
            }

            // Print information about
            // each type of file.
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attr) {
                if (attr.isRegularFile()) {
                    if (file.endsWith(".json")) {
                        final String f = file.getFileName().toString();
                        if (f.length() == 41 && f.startsWith(file.getParent().toString().toLowerCase())) {
                            try {
                                this.uuidSet.add(UUID.fromString(f.substring(0, 36)));
                            } catch (final Exception e) {
                                // ignored
                            }
                        }
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        }

    }

}
