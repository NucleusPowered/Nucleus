/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.api.module.warp.NucleusWarpService;
import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.api.module.warp.data.WarpCategory;
import io.github.nucleuspowered.nucleus.core.datatypes.NucleusNamedLocation;
import io.github.nucleuspowered.nucleus.modules.warp.WarpKeys;
import io.github.nucleuspowered.nucleus.modules.warp.data.NucleusWarpCategory;
import io.github.nucleuspowered.nucleus.modules.warp.data.NucleusWarp;
import io.github.nucleuspowered.nucleus.modules.warp.parameters.WarpCategoryParameter;
import io.github.nucleuspowered.nucleus.modules.warp.parameters.WarpParameter;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IGeneralDataObject;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Singleton
@APIService(NucleusWarpService.class)
public class WarpService implements NucleusWarpService, ServiceBase {

    public static final String WARP_KEY = "warp";
    public static final String WARP_CATEGORY_KEY = "warp category";

    @Nullable private Map<String, Warp> warpCache = null;
    @Nullable private Map<String, WarpCategory> warpCategoryCache = null;
    @Nullable private List<Warp> uncategorised = null;
    private final Map<String, List<Warp>> categoryCollectionMap = new HashMap<>();

    private final INucleusServiceCollection serviceCollection;

    private final Parameter.Value<Warp> warpPermissionArgument;
    private final Parameter.Value<Warp> warpNoPermissionArgument;
    private final Parameter.Value<WarpCategory> warpCategoryParameter;

    @Inject
    public WarpService(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.warpPermissionArgument = Parameter.builder(Warp.class)
                .addParser(new WarpParameter(
                    serviceCollection.permissionService(),
                    serviceCollection.messageProvider(),
                    this,
                    true))
                .key("warp")
                .build();
        this.warpNoPermissionArgument = Parameter.builder(Warp.class)
                .addParser(new WarpParameter(
                        serviceCollection.permissionService(),
                        serviceCollection.messageProvider(),
                        this,
                        false))
                .key("warp")
                .build();
        this.warpCategoryParameter = Parameter.builder(WarpCategory.class)
                .key("category")
                .addParser(new WarpCategoryParameter(
                        serviceCollection,
                        this))
                .build();
    }

    private Map<String, Warp> getWarpCache() {
        if (this.warpCache == null) {
            this.updateCache();
        }

        return this.warpCache;
    }

    private Map<String, WarpCategory> getWarpCategoryCache() {
        if (this.warpCategoryCache == null) {
            this.updateCache();
        }

        return this.warpCategoryCache;
    }

    private void updateCache() {
        this.categoryCollectionMap.clear();
        this.warpCache = new HashMap<>();
        this.warpCategoryCache = new HashMap<>();
        this.uncategorised = null;
        final IGeneralDataObject dataObject =
                this.serviceCollection
                        .storageManager()
                        .getGeneralService()
                        .getOrNewOnThread();

        dataObject.get(WarpKeys.WARP_NODES)
                .orElseGet(Collections::emptyMap)
                .forEach((key, value) -> this.warpCache.put(key.toLowerCase(), value));

                this.warpCategoryCache
                        .putAll(dataObject.get(WarpKeys.WARP_CATEGORIES)
                                .map(x -> x.stream().collect(Collectors.toMap(WarpCategory::getId, v -> v)))
                                .orElseGet(Collections::emptyMap));
    }

    private void saveFromCache() {
        if (this.warpCache == null || this.warpCategoryCache == null) {
            return; // not loaded
        }

        final IGeneralDataObject dataObject =
                this.serviceCollection
                        .storageManager()
                        .getGeneralService()
                        .getOrNewOnThread();
        dataObject.set(WarpKeys.WARP_NODES, new HashMap<>(this.warpCache));
        dataObject.set(WarpKeys.WARP_CATEGORIES, new ArrayList<>(this.warpCategoryCache.values()));
        this.serviceCollection.storageManager().getGeneralService().save(dataObject);
    }

    public Parameter.Value<Warp> warpElement(final boolean requirePermission) {
        if (requirePermission) {
            return this.warpPermissionArgument;
        } else {
            return this.warpNoPermissionArgument;
        }
    }

    public Parameter.Value<WarpCategory> warpCategoryElement() {
        return this.warpCategoryParameter;
    }

    @Override
    public Optional<Warp> getWarp(final String warpName) {
        return Optional.ofNullable(this.getWarpCache().get(warpName.toLowerCase()));
    }

    @Override
    public boolean removeWarp(final String warpName) {
        if (this.getWarpCache().remove(warpName.toLowerCase()) != null) {
            this.saveFromCache();
            return true;
        }

        return false;
    }

    @Override
    public boolean setWarp(final String warpName, final ServerLocation location, final Vector3d rotation) {
        final Map<String, Warp> cache = this.getWarpCache();
        final String key = warpName.toLowerCase();
        if (!cache.containsKey(key)) {
            cache.put(key, new NucleusWarp(
                    null,
                    0,
                    null,
                    new NucleusNamedLocation(warpName, location.worldKey(), location.position(), rotation)
            ));

            this.saveFromCache();
            return true;
        }

        return false;
    }

    @Override
    public List<Warp> getAllWarps() {
        return Collections.unmodifiableList(new ArrayList<>(this.getWarpCache().values()));
    }

    @Override
    public List<Warp> getUncategorisedWarps() {
        if (this.uncategorised == null) {
            this.uncategorised = this.getAllWarps()
                    .stream()
                    .filter(x -> !x.getCategory().isPresent())
                    .collect(Collectors.toList());
        }

        return Collections.unmodifiableList(this.uncategorised);
    }

    @Override
    public List<Warp> getWarpsForCategory(final String category) {
        final List<Warp> warps = this.categoryCollectionMap.computeIfAbsent(category.toLowerCase(),
                c -> this.getAllWarps().stream().filter(x ->
                        x.getCategory().map(cat -> cat.equalsIgnoreCase(c)).orElse(false))
                        .collect(Collectors.toList()));
        return Collections.unmodifiableList(warps);
    }

    public Map<WarpCategory, List<Warp>> getWarpsWithCategories() {
        return this.getWarpsWithCategories(t -> true);
    }

    @Override
    public Map<WarpCategory, List<Warp>> getWarpsWithCategories(final Predicate<Warp> warpDataPredicate) {
        // Populate cache
        final Map<WarpCategory, List<Warp>> map = new HashMap<>();
        this.getWarpCategoryCache().keySet().forEach(x -> {
            final List<Warp> warps = this.getWarpsForCategory(x).stream().filter(warpDataPredicate).collect(Collectors.toList());
            if (!warps.isEmpty()) {
                map.put(this.getWarpCategoryCache().get(x.toLowerCase()), warps);
            }
        });
        return map;
    }

    @Override
    public boolean removeWarpCost(final String warpName) {
        final Optional<Warp> warp = this.getWarp(warpName);
        if (warp.isPresent()) {
            final Warp w = warp.get();
            this.removeWarp(warpName);
            this.getWarpCache().put(w.getNamedLocation().getName().toLowerCase(), new NucleusWarp(
                    w.getCategory().orElse(null),
                    0,
                    w.getDescription().orElse(null),
                    w.getNamedLocation()
            ));
            this.saveFromCache();
            return true;
        }
        return false;
    }

    @Override
    public boolean setWarpCost(final String warpName, final double cost) {
        if (cost < 0) {
            return false;
        }

        final Optional<Warp> warp = this.getWarp(warpName);
        if (warp.isPresent()) {
            final Warp w = warp.get();
            this.removeWarp(warpName);
            this.getWarpCache().put(w.getNamedLocation().getName().toLowerCase(), new NucleusWarp(
                    w.getCategory().orElse(null),
                    cost,
                    w.getDescription().orElse(null),
                    w.getNamedLocation()
            ));
            this.saveFromCache();
            return true;
        }
        return false;
    }

    @Override
    public boolean setWarpCategory(final String warpName, @Nullable String category) {
        if (category != null) {
            final Optional<WarpCategory> c = this.getWarpCategory(category);
            if (!c.isPresent()) {
                final WarpCategory wc = new NucleusWarpCategory(
                        category,
                        null,
                        null);
                this.getWarpCategoryCache().put(category.toLowerCase(), wc);
            } else {
                this.categoryCollectionMap.remove(category.toLowerCase());
            }

            category = category.toLowerCase();
        } else {
            this.uncategorised = null;
        }

        final Optional<Warp> warp = this.getWarp(warpName);
        if (warp.isPresent()) {
            final Warp w = warp.get();
            this.removeWarp(warpName);
            this.getWarpCache().put(w.getNamedLocation().getName().toLowerCase(), new NucleusWarp(
                    category,
                    w.getCost().orElse(0d),
                    w.getDescription().orElse(null),
                    w.getNamedLocation()
            ));
            this.saveFromCache();
            return true;
        }
        return false;
    }

    @Override
    public boolean setWarpDescription(final String warpName, @Nullable final Component description) {
        final Optional<Warp> warp = this.getWarp(warpName);
        if (warp.isPresent()) {
            final Warp w = warp.get();
            this.removeWarp(warpName);
            this.getWarpCache().put(w.getNamedLocation().getName().toLowerCase(), new NucleusWarp(
                    w.getCategory().orElse(null),
                    w.getCost().orElse(0d),
                    description,
                    w.getNamedLocation()
            ));
            this.saveFromCache();
            return true;
        }
        return false;
    }

    @Override
    public Set<String> getWarpNames() {
        return this.getWarpCache().keySet();
    }

    @Override
    public Optional<WarpCategory> getWarpCategory(final String category) {
        return Optional.ofNullable(this.getWarpCategoryCache().get(category.toLowerCase()));
    }

    @Override
    public boolean setWarpCategoryDisplayName(final String category, @Nullable final Component displayName) {
        final Optional<WarpCategory> c = this.getWarpCategory(category);
        if (c.isPresent()) {
            final WarpCategory cat = c.get();
            this.getWarpCategoryCache().remove(category.toLowerCase());
            this.getWarpCategoryCache().put(category.toLowerCase(), new NucleusWarpCategory(
                    cat.getId(),
                    displayName,
                    cat.getDescription().orElse(null)
            ));
            this.saveFromCache();
            return true;
        }

        return false;
    }

    @Override
    public boolean setWarpCategoryDescription(final String category, @Nullable final Component description) {
        final Optional<WarpCategory> c = this.getWarpCategory(Objects.requireNonNull(category));
        if (c.isPresent()) {
            final WarpCategory cat = c.get();
            this.getWarpCategoryCache().remove(category.toLowerCase());
            this.getWarpCategoryCache().put(category.toLowerCase(), new NucleusWarpCategory(
                    cat.getId(),
                    cat.getDisplayName(),
                    description
            ));
            this.saveFromCache();
            return true;
        }

        return false;
    }
}
