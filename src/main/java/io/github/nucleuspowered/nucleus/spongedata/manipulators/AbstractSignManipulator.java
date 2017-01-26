/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata.manipulators;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.spongedata.NucleusSpongeDataKeys;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.OptionalValue;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

import javax.annotation.Nullable;

@NonnullByDefault
public abstract class AbstractSignManipulator<T extends AbstractData<T, I>, I extends AbstractImmutableData<I, T>> extends AbstractData<T, I> {

    @Nullable
    protected String permission = null;
    protected double cost = 0;

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

    // Setters
    private void setPermission(@Nullable String permission) {
        this.permission = permission;
    }

    private void setCost(@Nullable Double cost) {
        this.cost = cost == null ? 0 : cost;
    }

    // Value/key registration
    public final OptionalValue<String> getPermissionValue() {
        return Sponge.getRegistry().getValueFactory().createOptionalValue(NucleusSpongeDataKeys.NUCLEUS_PERMISSION, permission);
    }

    public final OptionalValue<Double> getCostValue() {
        if (cost > 0) {
            return Sponge.getRegistry().getValueFactory().createOptionalValue(NucleusSpongeDataKeys.NUCLEUS_COST, cost);
        }

        return Sponge.getRegistry().getValueFactory().createOptionalValue(NucleusSpongeDataKeys.NUCLEUS_COST, null);
    }

    // Manipulator
    public AbstractSignManipulator() {
        this(null, 0);
    }

    public AbstractSignManipulator(@Nullable String permission, double cost) {
        this.permission = permission;
        this.cost = cost;
        registerGettersAndSetters();
    }

    @Override protected final void registerGettersAndSetters() {
        this.registerFieldGetter(NucleusSpongeDataKeys.NUCLEUS_PERMISSION, this::getPermission);
        this.registerFieldGetter(NucleusSpongeDataKeys.NUCLEUS_COST, this::getCost);

        this.registerFieldSetter(NucleusSpongeDataKeys.NUCLEUS_PERMISSION, s -> this.setPermission(s.orElse(null)));
        this.registerFieldSetter(NucleusSpongeDataKeys.NUCLEUS_COST, s -> this.setCost(s.orElse(null)));

        this.registerKeyValue(NucleusSpongeDataKeys.NUCLEUS_PERMISSION, this::getPermissionValue);
        this.registerKeyValue(NucleusSpongeDataKeys.NUCLEUS_COST, this::getCostValue);

        registerSpecificGettersAndSetters();
    }

    protected abstract void registerSpecificGettersAndSetters();

    protected void populateFromDataView(DataView view) {
        view.getString(NucleusSpongeDataKeys.NUCLEUS_PERMISSION.getQuery()).ifPresent(this::setPermission);
        view.getDouble(NucleusSpongeDataKeys.NUCLEUS_COST.getQuery()).ifPresent(this::setCost);
    }

    protected <C extends AbstractSignManipulator<?, ?>> Optional<C> fillInternal(Class<C> clazz, DataHolder dataHolder, MergeFunction overlap) {
        if (clazz.isAssignableFrom(this.getClass())) {
            return Optional.of(Preconditions.checkNotNull(overlap).merge(clazz.cast(this), dataHolder.get(clazz).orElse(null)));
        }

        // Just in case.
        return Optional.empty();
    }

    protected void fromInternal(DataContainer dataContainer) {
        this.permission = dataContainer.getString(NucleusSpongeDataKeys.NUCLEUS_PERMISSION.getQuery()).orElse(null);
        this.cost = dataContainer.getDouble(NucleusSpongeDataKeys.NUCLEUS_COST.getQuery()).orElse(null);
    }

    @Override public int getContentVersion() {
        return 1;
    }

    @Override public final DataContainer toContainer() {
        DataContainer dataContainer = super.toContainer();
        getPermission().ifPresent(x -> dataContainer.set(NucleusSpongeDataKeys.NUCLEUS_PERMISSION.getQuery(), x));
        getCost().ifPresent(x -> dataContainer.set(NucleusSpongeDataKeys.NUCLEUS_COST.getQuery(), x));
        return toContainerSpecific(dataContainer);
    }

    public abstract DataContainer toContainerSpecific(DataContainer container);

    public static abstract class Simple<T extends AbstractSignManipulator<T, I>, I extends AbstractImmutableSignManipulator<I, T>> extends AbstractSignManipulator<T, I> {

        public Simple() {
            super();
        }

        public Simple(@Nullable String permission, double cost) {
            super(permission, cost);
        }

        @Override protected final void registerSpecificGettersAndSetters() {}

        @Override public final DataContainer toContainerSpecific(DataContainer container) {
            return container;
        }
    }
}
