/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.persistence.configurate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.nucleuspowered.nucleus.util.ThrownFunction;
import io.github.nucleuspowered.storage.exceptions.DataDeleteException;
import io.github.nucleuspowered.storage.exceptions.DataLoadException;
import io.github.nucleuspowered.storage.exceptions.DataQueryException;
import io.github.nucleuspowered.storage.exceptions.DataSaveException;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.queryobjects.IQueryObject;
import io.github.nucleuspowered.storage.util.KeyedObject;

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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

abstract class FlatFileStorageRepository implements IStorageRepository {

    Optional<JsonObject> get(@Nullable Path path) throws DataLoadException {
        if (path != null) {
            try {
                // Write the new file
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    return Optional.of(new JsonPrimitive(reader.lines().collect(Collectors.joining())).getAsJsonObject());
                }
            } catch (Exception e) {
                throw new DataLoadException("Could not load file at " + path.toAbsolutePath().toString(), e);
            }
        }

        return Optional.empty();
    }

    void save(Path file, JsonObject object) throws DataSaveException {
        try {
            // Backup the file
            Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            // Write the new file
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(object.toString());
            }
        } catch (Exception ex) {
            throw new DataSaveException("Could not save " + file.toString(), ex);
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

    static class Single extends FlatFileStorageRepository implements IStorageRepository.Single {

        private final Supplier<Path> FILENAME_RESOLVER;

        Single(Supplier<Path> filename_resolver) {
            this.FILENAME_RESOLVER = filename_resolver;
        }

        @Override
        public Optional<JsonObject> get() throws DataLoadException {
            if (Files.exists(FILENAME_RESOLVER.get())) {
                return get(FILENAME_RESOLVER.get());
            }

            return Optional.empty();
        }

        @Override
        public void save(JsonObject object) throws DataSaveException {
            save(FILENAME_RESOLVER.get(), object);
        }

    }

    static class UUIDKeyed<Q extends IQueryObject<UUID, Q>>
            extends FlatFileStorageRepository
            implements Keyed<UUID, Q> {

        private final ThrownFunction<Q, Path, DataQueryException> FILENAME_RESOLVER;
        private final Supplier<Path> BASE_PATH;
        private final Function<UUID, Path> UUID_FILENAME_RESOLVER;

        UUIDKeyed(ThrownFunction<Q, Path, DataQueryException> filename_resolver,
                Function<UUID, Path> uuid_filename_resolver,
                Supplier<Path> basePath) {
            this.FILENAME_RESOLVER = filename_resolver;
            this.UUID_FILENAME_RESOLVER = uuid_filename_resolver;
            this.BASE_PATH = basePath;
        }

        @Override
        public boolean exists(Q query) {
            try {
                return existsInternal(query) != null;
            } catch (DataQueryException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public Optional<KeyedObject<UUID, JsonObject>> get(Q query) throws DataLoadException {
            Path path;
            try {
                path = existsInternal(query);
            } catch (Exception e) {
                throw new DataLoadException("Query not valid", e);
            }

            return get(path).map(x -> new KeyedObject<>(query.keys().iterator().next(), x));
        }

        @Override
        public boolean exists(UUID uuid) {
            return existsInternal(uuid) != null;
        }

        @Override
        public Optional<JsonObject> get(UUID uuid) throws DataLoadException {
            return get(existsInternal(uuid));
        }

        @Override
        public Collection<UUID> getAllKeys() throws DataLoadException {
            return ImmutableSet.copyOf(getAllKeysInternal());
        }

        @Override
        public Map<UUID, JsonObject> getAll(Q query) throws DataLoadException, DataQueryException {
            ImmutableMap.Builder<UUID, JsonObject> j = ImmutableMap.builder();
            for (UUID key : getAllKeys(query)) {
                j.put(key, get(key).get()); // should be there
            }

            return j.build();
        }

        @Override
        public Collection<UUID> getAllKeys(Q query) throws DataLoadException, DataQueryException {
            if (query.restrictedToKeys()) {
                getAllKeysInternal().retainAll(query.keys());
            }

            throw new DataQueryException("There must only a key", query);
        }

        private Set<UUID> getAllKeysInternal() throws DataLoadException {
            UUIDFileWalker u = new UUIDFileWalker();
            try {
                Files.walkFileTree(BASE_PATH.get(), u);
                return u.uuidSet;
            } catch (IOException e) {
                throw new DataLoadException("Could not walk the file tree", e);
            }
        }

        @Nullable
        private Path existsInternal(UUID uuid) {
            Path path = UUID_FILENAME_RESOLVER.apply(uuid);
            if (Files.exists(UUID_FILENAME_RESOLVER.apply(uuid))) {
                return path;
            }

            return null;
        }


        @Override
        public int count(Q query) {
            return exists(query) ? 1 : 0;
        }

        @Override
        public void save(UUID key, JsonObject object) throws DataSaveException {
            Path file = UUID_FILENAME_RESOLVER.apply(key);
            save(file, object);
        }

        @Override
        public void delete(UUID key) throws DataDeleteException {
            Path filename = UUID_FILENAME_RESOLVER.apply(key);

            try {
                Files.delete(filename);
            } catch (IOException e) {
                throw new DataDeleteException("Could not delete " + filename, e);
            }
        }

        @Nullable
        private Path existsInternal(Q query) throws DataQueryException {
            Path path = FILENAME_RESOLVER.apply(query);
            if (Files.exists(FILENAME_RESOLVER.apply(query))) {
                return path;
            }

            return null;
        }

        private class UUIDFileWalker extends SimpleFileVisitor<Path> {

            private final Set<UUID> uuidSet = new HashSet<>();

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.getFileName().toString().length() == 2) {
                    return super.preVisitDirectory(dir, attrs);
                }

                return FileVisitResult.SKIP_SUBTREE;
            }

            // Print information about
            // each type of file.
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
                if (attr.isRegularFile()) {
                    if (file.endsWith(".json")) {
                        String f = file.getFileName().toString();
                        if (f.length() == 41 && f.startsWith(file.getParent().toString().toLowerCase())) {
                            try {
                                this.uuidSet.add(UUID.fromString(f.substring(0, 36)));
                            } catch (Exception e) {
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
