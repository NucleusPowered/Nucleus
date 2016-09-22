/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata.warp;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarpService;
import io.github.nucleuspowered.nucleus.api.spongedata.NucleusKeys;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.ImmutableWarpSignData;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.WarpSignData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.data.value.mutable.OptionalValue;

import java.util.Optional;

public class NucleusWarpSignData extends AbstractData<WarpSignData, ImmutableWarpSignData> implements WarpSignData {

    private String warpName = null;
    private String permission = null;
    private int warmupTime = 0;
    private int warpCost = 0;

    public NucleusWarpSignData() {
        this(null, null, 0, 0);
    }

    public NucleusWarpSignData(String warpName, String permission, int warmupTime, int warpCost) {
        this.warpName = warpName;
        this.permission = permission;
        this.warmupTime = Math.max(0, warmupTime);
        this.warpCost = Math.max(0, warpCost);
    }

    /**
     * Gets the name of the warp.
     *
     * @return The name of the warp.
     */
    @Override
    public OptionalValue<String> warpName() {
        return Sponge.getRegistry().getValueFactory().createOptionalValue(NucleusKeys.WARP_NAME, warpName);
    }

    /**
     * The permission required to use the warp.
     *
     * @return The permission, if it exists.
     */
    @Override
    public OptionalValue<String> permission() {
        return Sponge.getRegistry().getValueFactory().createOptionalValue(NucleusKeys.WARP_PERMISSION, permission);
    }

    /**
     * The warmup time.
     *
     * @return The time, in seconds.
     */
    @Override
    public MutableBoundedValue<Integer> warmupTime() {
        return Sponge.getRegistry().getValueFactory()
                .createBoundedValueBuilder(NucleusKeys.WARP_WARMUP).minimum(0).maximum(Integer.MAX_VALUE)
                .actualValue(warmupTime).defaultValue(0).build();
    }

    /**
     * The warmup time.
     *
     * @return The time, in seconds.
     */
    @Override
    public MutableBoundedValue<Integer> cost() {
        return Sponge.getRegistry().getValueFactory()
                .createBoundedValueBuilder(NucleusKeys.WARP_WARMUP).minimum(0).maximum(Integer.MAX_VALUE)
                .actualValue(warpCost).defaultValue(0).build();
    }

    @Override
    protected void registerGettersAndSetters() {
        registerFieldGetter(NucleusKeys.WARP_NAME, this::getWarpName);
        registerFieldSetter(NucleusKeys.WARP_NAME, this::setWarpName);
        registerKeyValue(NucleusKeys.WARP_NAME, this::warpName);

        registerFieldGetter(NucleusKeys.WARP_PERMISSION, this::getPermission);
        registerFieldSetter(NucleusKeys.WARP_PERMISSION, this::setPermission);
        registerKeyValue(NucleusKeys.WARP_PERMISSION, this::permission);

        registerFieldGetter(NucleusKeys.WARP_WARMUP, this::getWarmupTime);
        registerFieldSetter(NucleusKeys.WARP_WARMUP, this::setWarmupTime);
        registerKeyValue(NucleusKeys.WARP_WARMUP, this::warmupTime);

        registerFieldGetter(NucleusKeys.WARP_COST, this::getWarpCost);
        registerFieldSetter(NucleusKeys.WARP_COST, this::setWarpCost);
        registerKeyValue(NucleusKeys.WARP_COST, this::cost);
    }

    @Override
    public Optional<WarpSignData> fill(DataHolder dataHolder, MergeFunction overlap) {
        return Optional.of(Preconditions.checkNotNull(overlap).merge(copy(), from(dataHolder.toContainer()).orElse(null)));
    }

    @Override
    public Optional<WarpSignData> from(DataContainer container) {
        if (!container.contains(NucleusKeys.WARP_NAME.getQuery())) {
            return Optional.empty();
        }

        return Optional.of(
            new NucleusWarpSignData(
                container.getString(NucleusKeys.WARP_NAME.getQuery()).orElse(null),
                container.getString(NucleusKeys.WARP_PERMISSION.getQuery()).orElse(null),
                container.getInt(NucleusKeys.WARP_WARMUP.getQuery()).orElse(0),
                container.getInt(NucleusKeys.WARP_COST.getQuery()).orElse(0)
            ));
    }

    @Override
    public WarpSignData copy() {
        return new NucleusWarpSignData(warpName, permission, warmupTime, warpCost);
    }

    @Override
    public ImmutableWarpSignData asImmutable() {
        return null;
    }

    @Override
    public int compareTo(WarpSignData o) {
        return ComparisonChain.start()
                .compare(this.warpName, o.warpName().get().orElse(null))
                .compare(this.warmupTime, o.warmupTime().get().intValue())
                .compare(this.permission, o.permission().get().orElse(null))
                .result();
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    // Getters and setters.
    public Optional<String> getWarpName() {
        return Optional.ofNullable(warpName);
    }

    public NucleusWarpSignData setWarpName(Optional<String> warpName) {
        String warp = warpName.orElse(null);
        Optional<NucleusWarpService> onws = Sponge.getServiceManager().provide(NucleusWarpService.class);
        Preconditions.checkState(onws.isPresent());

        if (warp != null && onws.isPresent()) {
            Preconditions.checkState(onws.get().warpExists(warp));
        }

        this.warpName = warp;
        return this;
    }

    public Optional<String> getPermission() {
        return Optional.ofNullable(permission);
    }

    @SuppressWarnings("all")
    public NucleusWarpSignData setPermission(Optional<String> permission) {
        this.permission = permission.orElse(null);
        return this;
    }

    public Integer getWarmupTime() {
        return warmupTime;
    }

    public NucleusWarpSignData setWarmupTime(Integer warmupTime) {
        this.warmupTime = warmupTime;
        return this;
    }

    public Integer getWarpCost() {
        return warpCost;
    }

    public NucleusWarpSignData setWarpCost(Integer warpCost) {
        this.warpCost = warpCost;
        return this;
    }
}
