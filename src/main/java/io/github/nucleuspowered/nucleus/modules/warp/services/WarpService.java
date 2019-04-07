/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.services;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Warp;
import io.github.nucleuspowered.nucleus.api.nucleusdata.WarpCategory;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarpService;
import io.github.nucleuspowered.nucleus.internal.annotations.APIService;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.modules.warp.WarpKeys;
import io.github.nucleuspowered.nucleus.modules.warp.data.WarpCategoryData;
import io.github.nucleuspowered.nucleus.modules.warp.data.WarpData;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IGeneralDataObject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@NonnullByDefault
@Singleton
@APIService(NucleusWarpService.class)
public class WarpService implements NucleusWarpService, ServiceBase {

    @Nullable private Map<String, Warp> warpCache = null;
    @Nullable private Map<String, WarpCategory> warpCategoryCache = null;
    @Nullable private List<Warp> uncategorised = null;
    private final Map<String, List<Warp>> categoryCollectionMap = new HashMap<>();

    public Map<String, Warp> getWarpCache() {
        if (this.warpCache == null) {
            updateCache();
        }

        return this.warpCache;
    }

    public Map<String, WarpCategory> getWarpCategoryCache() {
        if (this.warpCategoryCache == null) {
            updateCache();
        }

        return this.warpCategoryCache;
    }

    public void updateCache() {
        this.categoryCollectionMap.clear();
        this.warpCache = new HashMap<>();
        this.warpCategoryCache = new HashMap<>();
        this.uncategorised = null;
        IGeneralDataObject dataObject = Nucleus.getNucleus()
                .getStorageManager()
                .getGeneralService()
                .getOrNewOnThread();

        dataObject.get(WarpKeys.WARP_NODES)
                .orElseGet(ImmutableMap::of)
                .forEach((key, value) -> {
                    this.warpCache.put(key.toLowerCase(), value);
                });

                this.warpCategoryCache.putAll(dataObject.get(WarpKeys.WARP_CATEGORIES)
                                .orElseGet(ImmutableMap::of));
    }

    public void saveFromCache() {
        if (this.warpCache == null || this.warpCategoryCache == null) {
            return; // not loaded
        }

        IGeneralDataObject dataObject = Nucleus.getNucleus()
                .getStorageManager()
                .getGeneralService()
                .getOrNewOnThread();
        dataObject.set(WarpKeys.WARP_NODES, new HashMap<>(this.warpCache));
        dataObject.set(WarpKeys.WARP_CATEGORIES, new HashMap<>(this.warpCategoryCache));
        Nucleus.getNucleus().getStorageManager().getGeneralService().save(dataObject);
    }

    @Override
    public Optional<Warp> getWarp(String warpName) {
        return Optional.ofNullable(getWarpCache().get(warpName.toLowerCase()));
    }

    @Override
    public boolean removeWarp(String warpName) {
        if (getWarpCache().remove(warpName.toLowerCase()) != null) {
            saveFromCache();
            return true;
        }

        return false;
    }

    @Override
    public boolean setWarp(String warpName, Location<World> location, Vector3d rotation) {
        Map<String, Warp> cache = getWarpCache();
        String key = warpName.toLowerCase();
        if (!cache.containsKey(key)) {
            cache.put(key, new WarpData(
                    null,
                    0,
                    null,
                    location.getExtent().getUniqueId(),
                    location.getPosition(),
                    rotation,
                    warpName
            ));

            saveFromCache();
            return true;
        }

        return false;
    }

    @Override
    public List<Warp> getAllWarps() {
        return ImmutableList.copyOf(getWarpCache().values());
    }

    @Override
    public List<Warp> getUncategorisedWarps() {
        if (this.uncategorised == null) {
            this.uncategorised = Lists.newArrayList(
                    getAllWarps()
                            .stream()
                            .filter(x -> !x.getCategory().isPresent())
                            .collect(Collectors.toList())
            );
        }

        return ImmutableList.copyOf(this.uncategorised);
    }

    @Override
    public List<Warp> getWarpsForCategory(String category) {
        List<Warp> warps = this.categoryCollectionMap.computeIfAbsent(category.toLowerCase(),
                c -> Lists.newArrayList(getAllWarps().stream().filter(x ->
                        x.getCategory().map(cat -> cat.equalsIgnoreCase(c)).orElse(false))
                        .collect(Collectors.toList())));
        return ImmutableList.copyOf(warps);
    }

    public Map<WarpCategory, List<Warp>> getWarpsWithCategories() {
        return getWarpsWithCategories(t -> true);
    }

    @Override
    public Map<WarpCategory, List<Warp>> getWarpsWithCategories(Predicate<Warp> warpDataPredicate) {
        // Populate cache
        Map<WarpCategory, List<Warp>> map = new HashMap<>();
        this.warpCategoryCache.keySet().forEach(x -> {
            List<Warp> warps = getWarpsForCategory(x).stream().filter(warpDataPredicate).collect(Collectors.toList());
            if (!warps.isEmpty()) {
                map.put(this.warpCategoryCache.get(x.toLowerCase()), warps);
            }
        });
        return map;
    }

    @Override
    public boolean removeWarpCost(String warpName) {
        Optional<Warp> warp = getWarp(warpName);
        if (warp.isPresent()) {
            Warp w = warp.get();
            removeWarp(warpName);
            this.warpCache.put(w.getName().toLowerCase(), new WarpData(
                    w.getCategory().orElse(null),
                    0,
                    w.getDescription().orElse(null),
                    w.getWorldUUID(),
                    w.getPosition(),
                    w.getRotation(),
                    w.getName()
            ));
            saveFromCache();
            return true;
        }
        return false;
    }

    @Override
    public boolean setWarpCost(String warpName, double cost) {
        if (cost < 0) {
            return false;
        }

        Optional<Warp> warp = getWarp(warpName);
        if (warp.isPresent()) {
            Warp w = warp.get();
            removeWarp(warpName);
            this.warpCache.put(w.getName().toLowerCase(), new WarpData(
                    w.getCategory().orElse(null),
                    cost,
                    w.getDescription().orElse(null),
                    w.getWorldUUID(),
                    w.getPosition(),
                    w.getRotation(),
                    w.getName()
            ));
            saveFromCache();
            return true;
        }
        return false;
    }

    @Override
    public boolean setWarpCategory(String warpName, @Nullable String category) {
        if (category != null) {
            Optional<WarpCategory> c = getWarpCategory(category);
            if (!c.isPresent()) {
                WarpCategory wc = new WarpCategoryData(
                        category,
                        null,
                        null);
                this.warpCategoryCache.put(category.toLowerCase(), wc);
            } else {
                this.categoryCollectionMap.remove(category.toLowerCase());
            }

            category = category.toLowerCase();
        } else {
            this.uncategorised = null;
        }

        Optional<Warp> warp = getWarp(warpName);
        if (warp.isPresent()) {
            Warp w = warp.get();
            removeWarp(warpName);
            this.warpCache.put(w.getName().toLowerCase(), new WarpData(
                    category,
                    w.getCost().orElse(0d),
                    w.getDescription().orElse(null),
                    w.getWorldUUID(),
                    w.getPosition(),
                    w.getRotation(),
                    w.getName()
            ));
            saveFromCache();
            return true;
        }
        return false;
    }

    @Override
    public boolean setWarpDescription(String warpName, @Nullable Text description) {
        Optional<Warp> warp = getWarp(warpName);
        if (warp.isPresent()) {
            Warp w = warp.get();
            removeWarp(warpName);
            this.warpCache.put(w.getName().toLowerCase(), new WarpData(
                    w.getCategory().orElse(null),
                    w.getCost().orElse(0d),
                    description,
                    w.getWorldUUID(),
                    w.getPosition(),
                    w.getRotation(),
                    w.getName()
            ));
            saveFromCache();
            return true;
        }
        return false;
    }

    @Override
    public Set<String> getWarpNames() {
        return getWarpCache().keySet();
    }

    @Override
    public Optional<WarpCategory> getWarpCategory(String category) {
        return Optional.ofNullable(getWarpCategoryCache().get(category.toLowerCase()));
    }

    @Override
    public boolean setWarpCategoryDisplayName(String category, @Nullable Text displayName) {
        Optional<WarpCategory> c = getWarpCategory(category);
        if (c.isPresent()) {
            WarpCategory cat = c.get();
            this.warpCategoryCache.remove(category.toLowerCase());
            this.warpCategoryCache.put(category.toLowerCase(), new WarpCategoryData(
                    cat.getId(),
                    displayName,
                    cat.getDescription().orElse(null)
            ));
            saveFromCache();
            return true;
        }

        return false;
    }

    @Override
    public boolean setWarpCategoryDescription(String category, @Nullable Text description) {
        Optional<WarpCategory> c = getWarpCategory(Objects.requireNonNull(category));
        if (c.isPresent()) {
            WarpCategory cat = c.get();
            this.warpCategoryCache.remove(category.toLowerCase());
            this.warpCategoryCache.put(category.toLowerCase(), new WarpCategoryData(
                    cat.getId(),
                    cat.getDisplayName(),
                    description
            ));
            saveFromCache();
            return true;
        }

        return false;
    }
}
