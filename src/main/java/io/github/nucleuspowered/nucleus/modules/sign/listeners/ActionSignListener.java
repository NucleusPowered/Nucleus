/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.sign.listeners;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.annotations.ConditionalListener;
import io.github.nucleuspowered.nucleus.internal.signs.SignDataListenerBase;
import io.github.nucleuspowered.nucleus.modules.sign.SignModule;
import io.github.nucleuspowered.nucleus.modules.sign.config.SignConfig;
import io.github.nucleuspowered.nucleus.modules.sign.config.SignConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.sign.handlers.ActionSignHandler;
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

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import javax.inject.Inject;

/**
 * This {@link ActionSignListener} is a specialised listener that is intended to unify some of the logic that sign
 * listening requires.
 */
@ConditionalListener(ActionSignListener.Condition.class)
public class ActionSignListener extends ListenerBase {

    @Inject private ActionSignHandler handler;

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
        ol.ifPresent(worldLocation -> event.setCancelled(!continueAction(worldLocation, (x, sign) -> x.onInteract(sign, player))));
    }

    private boolean continueAction(Location<World> location, BiFunction<SignDataListenerBase<?>, Sign, Boolean> methodToTest) {
        Optional<TileEntity> tileEntityOptional = location.getTileEntity();
        if (tileEntityOptional.isPresent()) {
            TileEntity te = tileEntityOptional.get();
            if (te instanceof Sign) {
                Sign sign = (Sign)te;

                // If one matches, cancel the transaction.
                return handler.getListeners().stream().anyMatch(x -> methodToTest.apply(x, sign));
            }
        }

        return true;
    }

    public static class Condition implements Predicate<Nucleus> {

        @Override public boolean test(Nucleus nucleus) {
            return nucleus.getConfigValue(SignModule.ID, SignConfigAdapter.class, SignConfig::isActionSigns).orElse(false);
        }
    }
}
