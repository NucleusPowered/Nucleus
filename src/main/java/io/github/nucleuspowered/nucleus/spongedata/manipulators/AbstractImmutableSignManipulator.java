/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata.manipulators;

import io.github.nucleuspowered.nucleus.spongedata.NucleusSpongeDataKeys;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import java.util.Optional;

import javax.annotation.Nullable;

public abstract class AbstractImmutableSignManipulator<I extends AbstractImmutableData<I, T>, T extends AbstractData<T, I>>
    extends AbstractImmutableData<I, T> {

    @Nullable
    protected final String permission;
    protected final double cost;

    // Getters
    public final Optional<String> getPermission() {
        return Optional.ofNullable(permission);
    }

    public final Optional<Double> getCost() {
        if (cost <= 0) {
            return Optional.empty();
        }

        return Optional.of(cost);
    }

    // Value/key registration
    public final ImmutableValue<Optional<String>> getPermissionValue() {
        return Sponge.getRegistry().getValueFactory().createOptionalValue(NucleusSpongeDataKeys.NUCLEUS_PERMISSION, permission).asImmutable();
    }

    public final ImmutableValue<Optional<Double>> getCostValue() {
        if (cost > 0) {
            return Sponge.getRegistry().getValueFactory().createOptionalValue(NucleusSpongeDataKeys.NUCLEUS_COST, cost).asImmutable();
        }

        return Sponge.getRegistry().getValueFactory().createOptionalValue(NucleusSpongeDataKeys.NUCLEUS_COST, null).asImmutable();
    }

    public AbstractImmutableSignManipulator(@Nullable String permission, @Nullable Double cost) {
        this.permission = permission;
        this.cost = cost == null || cost <= 0 ? 0 : cost;
        registerGetters();
    }

    @Override protected final void registerGetters() {
        this.registerFieldGetter(NucleusSpongeDataKeys.NUCLEUS_PERMISSION, this::getPermission);
        this.registerFieldGetter(NucleusSpongeDataKeys.NUCLEUS_COST, this::getCost);

        this.registerKeyValue(NucleusSpongeDataKeys.NUCLEUS_PERMISSION, this::getPermissionValue);
        this.registerKeyValue(NucleusSpongeDataKeys.NUCLEUS_COST, this::getCostValue);

        registerSpecificGetters();
    }

    protected abstract void registerSpecificGetters();

    @Override public int getContentVersion() {
        return 1;
    }

    @Override public final DataContainer toContainer() {
        DataContainer dataContainer = super.toContainer();
        getPermission().ifPresent(x -> dataContainer.set(NucleusSpongeDataKeys.NUCLEUS_PERMISSION.getQuery(), x));
        getCost().ifPresent(x -> dataContainer.set(NucleusSpongeDataKeys.NUCLEUS_COST.getQuery(), x));
        return toContainerSpecific(dataContainer);
    }

    public abstract DataContainer toContainerSpecific(DataContainer dataContainer);

    public static abstract class Simple<I extends AbstractImmutableSignManipulator<I, T>, T extends AbstractSignManipulator<T, I>> extends AbstractImmutableSignManipulator<I, T> {

        public Simple(@Nullable String permission, @Nullable Double cost) {
            super(permission, cost);
        }

        @Override protected final void registerSpecificGetters() {}

        @Override public final DataContainer toContainerSpecific(DataContainer container) {
            return container;
        }
    }
}
