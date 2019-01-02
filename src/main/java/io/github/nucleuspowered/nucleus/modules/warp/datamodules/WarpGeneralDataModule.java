/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.datamodules;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Warp;
import io.github.nucleuspowered.nucleus.api.nucleusdata.WarpCategory;
import io.github.nucleuspowered.nucleus.configurate.datatypes.WarpCategoryDataNode;
import io.github.nucleuspowered.nucleus.configurate.datatypes.WarpNode;
import io.github.nucleuspowered.nucleus.internal.LocationData;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.IGeneralDataModule;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.LocationDataModule;
import io.github.nucleuspowered.storage.dataobjects.modules.DataKey;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

// TODO: POJO
public class WarpGeneralDataModule extends LocationDataModule implements IGeneralDataModule {

    private final BiFunction<String, WarpNode, Warp> getWarpLocation = (s, l) ->
            new WarpData(s, l.getWorld(), l.getPosition(), l.getRotation(), l.getCost(), l.getCategory().orElse(null), l.getDescription());

    @DataKey("warps")
    private Map<String, WarpNode> warps = Maps.newHashMap();

    @DataKey("warpCategories")
    private Map<String, WarpCategoryDataNode> warpCategories = Maps.newHashMap();

    public Optional<Warp> getWarpLocation(String name) {
        return get(this.warps, this.getWarpLocation, name);
    }

    public Map<String, Warp> getWarps() {
        return convert(this.warps, this.getWarpLocation);
    }

    public boolean addWarp(String name, Location<World> loc, Vector3d rot) {
        if (Util.getKeyIgnoreCase(this.warps, name).isPresent()) {
            return false;
        }

        this.warps.put(name, new WarpNode(loc, rot));
        return true;
    }

    public boolean setWarpCost(String name, double cost) {
        Preconditions.checkArgument(cost >= -1);
        Optional<WarpNode> os = Util.getValueIgnoreCase(this.warps, name);
        if (os.isPresent()) {
            // No need to put it back - it's saved automatically.
            os.get().setCost(cost);
            return true;
        }

        return false;
    }

    public boolean setWarpsWarpCategory(String name, String category) {
        Optional<WarpNode> os = Util.getValueIgnoreCase(this.warps, name);
        if (os.isPresent()) {
            // No need to put it back - it's saved automatically.
            os.get().setCategory(category);
            if (category != null) {
                this.warpCategories.putIfAbsent(category.toLowerCase(), new WarpCategoryDataNode());
            }

            return true;
        }

        return false;
    }

    public boolean setWarpDescription(String name, @Nullable Text description) {
        Optional<WarpNode> os = Util.getValueIgnoreCase(this.warps, name);
        if (os.isPresent()) {
            // No need to put it back - it's saved automatically.
            os.get().setDescription(description);
            return true;
        }

        return false;
    }

    public boolean removeWarp(String name) {
        Optional<String> os = Util.getKeyIgnoreCase(this.warps, name);
        if (os.isPresent()) {
            this.warps.remove(os.get());
            return true;
        }

        return false;
    }

    public WarpCategory getWarpCategoryOrDefault(String category) {
        return getWarpCategory(category).orElseGet(() -> new WarpCategoryData(
                category,
                Text.of(category),
                null,
                () -> getWarps().values().stream().filter(x -> x.getCategory().map(y -> y.equals(category)).orElse(false))
                        .collect(Collectors.toList())));
    }

    public Optional<WarpCategory> getWarpCategory(String category) {
        Preconditions.checkArgument(category != null && !category.isEmpty());
        if (this.warps.values().stream().noneMatch(x -> x.getCategory().orElse("").equalsIgnoreCase(category))) {
            return Optional.empty();
        }

        WarpCategoryDataNode w = this.warpCategories.get(category);
        if (w == null) {
            w = new WarpCategoryDataNode();
            updateOrSetWarpCategory(category.toLowerCase(), null, null);
        }

        return Optional.of(new WarpCategoryData(
            category,
            w.getDisplayName().map(TextSerializers.JSON::deserialize).orElse(Text.of(category)),
            w.getDescription().map(TextSerializers.JSON::deserialize).orElse(null),
            () -> getWarps().values().stream().filter(x -> x.getCategory().map(y -> y.equals(category)).orElse(false)).collect(Collectors.toList())
        ));
    }

    public void updateOrSetWarpCategory(String category, @Nullable Text displayName, @Nullable Text description) {
        this.warpCategories.put(category,
            new WarpCategoryDataNode(
                TextSerializers.JSON.serialize(displayName != null ? displayName : Text.of(category)),
                description != null ? TextSerializers.JSON.serialize(description) : null
            ));
    }

    private static class WarpData extends LocationData implements Warp {

        private final double cost;
        @Nullable private final String category;
        @Nullable private final Text description;

        private WarpData(String name, UUID world, Vector3d position, Vector3d rotation, double cost, @Nullable String category,
                @Nullable Text description) {
            super(name, world, position, rotation);
            this.cost = Math.max(-1, cost);
            this.category = category;
            this.description = description;
        }

        public Optional<Double> getCost() {
            if (this.cost > -1) {
                return Optional.of(this.cost);
            }

            return Optional.empty();
        }

        @Override public Optional<Text> getDescription() {
            return Optional.ofNullable(this.description);
        }

        public Optional<String> getCategory() {
            return Optional.ofNullable(this.category);
        }

        @Override
        public String toString() {
            return super.toString() + "category: " + this.category + ", cost: " + this.cost;
        }
    }

    private static class WarpCategoryData implements WarpCategory {

        private final String name;
        private final Text displayName;
        @Nullable private final Text description;
        private final Supplier<Collection<Warp>> getWarps;

        public WarpCategoryData(String name, Text displayName, @Nullable Text description, Supplier<Collection<Warp>> getWarps) {
            this.name = Preconditions.checkNotNull(name);
            this.displayName = Preconditions.checkNotNull(displayName);
            this.description = description;
            this.getWarps = getWarps;
        }

        @Override public String getId() {
            return this.name;
        }

        @Override public Text getDisplayName() {
            return this.displayName;
        }

        @Override public Optional<Text> getDescription() {
            return Optional.ofNullable(this.description);
        }

        @Override public Collection<Warp> getWarps() {
            return this.getWarps.get();
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            WarpCategoryData that = (WarpCategoryData) o;

            return this.name.equals(that.name);
        }

        @Override public int hashCode() {
            return this.name.hashCode();
        }
    }
}
