/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.internal.listeners.SignDataListenerBase;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * This {@link SignListener} is a specialised listener that is intended to unify some of the logic that sign
 * listening requires.
 */
public final class SignListener {

    private final List<SignDataListenerBase<?>> listenerBases = Lists.newArrayList();
    private final NucleusPlugin plugin;

    public SignListener(NucleusPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers sign listeners with their various data classes.
     *
     * @param dataListenerBase The {@link SignDataListenerBase} that will handle some sort of data infused sign.
     */
    public void registerSignListener(SignDataListenerBase<?> dataListenerBase) {
        if (!plugin.isModulesLoaded()) {
            listenerBases.add(dataListenerBase);
        }
    }

    /**
     * Gets whether listeners have been added.
     *
     * @return <code>true</code> if there is at least one.
     */
    public boolean hasListeners() {
        return !listenerBases.isEmpty();
    }

    /**
     * Fired when the sign is broken. We use {@link First} over {@link Root} as we want to catch various contraptions
     * that might break it too!
     *
     * @param event The {@link org.spongepowered.api.event.block.ChangeBlockEvent.Break}.
     * @param player The responsible player.
     */
    @Listener(order = Order.EARLY)
    @Include({ChangeBlockEvent.Break.class, ChangeBlockEvent.Modify.class})
    public void onBreak(ChangeBlockEvent event, @First Player player) {
        event.filter(x -> continueAction(x, (listenerBase, sign) -> listenerBase.onBreak(sign, player)));
    }

    /**
     * Fired when the sign is interacted with. {@link Root} is fine here!
     *
     * @param event The {@link org.spongepowered.api.event.block.InteractBlockEvent}.
     * @param player The responsible player.
     */
    @Listener
    public void onInteract(InteractBlockEvent event, @Root Player player) {
        Optional<Location<World>> ol = event.getTargetBlock().getLocation();
        if (ol.isPresent()) {
            event.setCancelled(!continueAction(ol.get(), (x, sign) -> x.onInteract(sign, player)));
        }
    }

    private boolean continueAction(Location<World> location, BiFunction<SignDataListenerBase<?>, Sign, Boolean> methodToTest) {
        Optional<TileEntity> tileEntityOptional = location.getTileEntity();
        if (tileEntityOptional.isPresent()) {
            TileEntity te = tileEntityOptional.get();
            if (te instanceof Sign) {
                Sign sign = (Sign)te;

                // If one doesn't match, then it fails, and we cancel the transaction.
                return listenerBases.stream().allMatch(x -> methodToTest.apply(x, sign));
            }
        }

        return true;
    }
}
