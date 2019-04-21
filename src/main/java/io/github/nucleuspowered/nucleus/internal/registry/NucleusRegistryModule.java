/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.github.nucleuspowered.nucleus.annotationprocessor.Store;
import io.github.nucleuspowered.nucleus.internal.CatalogTypeFinalStaticProcessor;
import io.github.nucleuspowered.nucleus.internal.Constants;
import io.github.nucleuspowered.nucleus.internal.annotations.Registry;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.registry.AdditionalCatalogRegistryModule;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Store(Constants.REGISTRY)
@NonnullByDefault
public abstract class NucleusRegistryModule<T extends CatalogType>
        implements AdditionalCatalogRegistryModule<T> {

    private boolean registered = false;
    private final Map<String, T> entries = new HashMap<>();

    public abstract Class<T> catalogClass();

    public abstract void registerModuleDefaults();

    public final void registerDefaults() {
        // no-op, I'll do it myself and not rely on the current impl that's being used.
        // API 8 will rely on impl.
    }

    protected boolean allowsAdditional() {
        return true;
    }

    public final void init() throws Exception {
        if (!this.registered) {
            registerModuleDefaults();
            this.registered = true;
            if (getClass().isAnnotationPresent(Registry.class)) {
                for (Class<?> clazz : getClass().getAnnotation(Registry.class).value()) {
                    CatalogTypeFinalStaticProcessor.setFinalStaticFields(clazz, this.entries);
                }
            }
            Sponge.getRegistry().registerModule(catalogClass(), this);
        }
    }

    @Override
    public void registerAdditionalCatalog(T entry) {
        Preconditions.checkNotNull(entry, "entry");
        if (this.entries.containsKey(entry.getId().toLowerCase(Locale.ENGLISH))) {
            throw new IllegalArgumentException("Cannot register that ID as it already has been registered");
        }

        if (this.registered && entry.getId().toLowerCase(Locale.ENGLISH).startsWith("nucleus:")) {
            // no
            throw new IllegalArgumentException("Cannot register that ID, additional catalogs must not start "
                    + "with the nucleus namespace");
        }

        if (!this.registered || allowsAdditional()) {
            this.entries.put(entry.getId().toLowerCase(Locale.ENGLISH), entry);
        }

        throw new IllegalArgumentException("Cannot register additional types for this catalog");
    }

    @Override
    public Optional<T> getById(String id) {
        final String lowerId = id.toLowerCase(Locale.ENGLISH);
        T instance = this.entries.get(lowerId);
        if (instance == null && !lowerId.contains(":")) {
            instance = this.entries.get("nucleus:" + lowerId);
        }

        return Optional.ofNullable(instance);
    }

    @Override
    public Collection<T> getAll() {
        return ImmutableList.copyOf(this.entries.values());
    }
}
