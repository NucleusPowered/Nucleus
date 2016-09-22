/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata.warp;

import com.google.common.collect.ComparisonChain;
import io.github.nucleuspowered.nucleus.api.spongedata.NucleusKeys;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.ImmutableWarpSignData;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.WarpSignData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.data.value.immutable.ImmutableBoundedValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import java.util.Optional;

public class NucleusImmutableWarpSignData extends AbstractImmutableData<ImmutableWarpSignData, WarpSignData> implements ImmutableWarpSignData {
    private String warpName = null;
    private String permission = null;
    private int warmupTime = 0;

    public NucleusImmutableWarpSignData() {
        this(null, null, 0);
    }

    public NucleusImmutableWarpSignData(String warpName, String permission, int warmupTime) {
        this.warpName = warpName;
        this.permission = permission;
        this.warmupTime = Math.max(0, warmupTime);
    }

    @Override
    public ImmutableValue<Optional<String>> warpName() {
        return Sponge.getRegistry().getValueFactory().createOptionalValue(NucleusKeys.WARP_NAME, warpName).asImmutable();
    }

    @Override
    public ImmutableValue<Optional<String>> permission() {
        return Sponge.getRegistry().getValueFactory().createOptionalValue(NucleusKeys.WARP_PERMISSION, permission).asImmutable();
    }

    @Override
    public ImmutableBoundedValue<Integer> warmupTime() {
        return Sponge.getRegistry().getValueFactory()
                .createBoundedValueBuilder(NucleusKeys.WARP_WARMUP).minimum(0).maximum(Integer.MAX_VALUE)
                .actualValue(warmupTime).defaultValue(0).build().asImmutable();
    }

    @Override
    protected void registerGetters() {
        registerFieldGetter(NucleusKeys.WARP_NAME, this::getWarpName);
        registerKeyValue(NucleusKeys.WARP_NAME, this::warpName);

        registerFieldGetter(NucleusKeys.WARP_PERMISSION, this::getPermission);
        registerKeyValue(NucleusKeys.WARP_PERMISSION, this::permission);

        registerFieldGetter(NucleusKeys.WARP_WARMUP, this::getWarmupTime);
        registerKeyValue(NucleusKeys.WARP_WARMUP, this::warmupTime);
    }

    @Override
    public NucleusWarpSignData asMutable() {
        return new NucleusWarpSignData(warpName, permission, warmupTime);
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    public int compareTo(ImmutableWarpSignData o) {
        return ComparisonChain.start()
                .compare(this.warpName, o.warpName().get().orElse(null))
                .compare(this.warmupTime, o.warmupTime().get().intValue())
                .compare(this.permission, o.permission().get().orElse(null))
                .result();
    }

    public String getWarpName() {
        return warpName;
    }

    public String getPermission() {
        return permission;
    }

    public int getWarmupTime() {
        return warmupTime;
    }
}
