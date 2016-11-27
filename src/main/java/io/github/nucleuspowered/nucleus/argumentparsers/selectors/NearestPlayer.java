/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.argumentparsers.selectors;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.interfaces.SelectorParser;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Obtains the nearest player to the locatable object.
 */
public class NearestPlayer implements SelectorParser<Player> {

    public final static NearestPlayer INSTANCE = new NearestPlayer();

    private final Pattern selector = Pattern.compile("^p$", Pattern.CASE_INSENSITIVE);

    private NearestPlayer() {}

    @Override
    public Pattern selector() {
        return selector;
    }

    @Override
    public Player get(String selector, CommandSource source, CommandArgs args) throws ArgumentParseException {
        if (!(source instanceof Locatable)) {
            throw args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.selector.nolocation"));
        }

        Locatable locatedSource = (Locatable)source;

        // We don't want the executing player.
        List<Player> playerCollection = Lists.newArrayList(Sponge.getServer().getOnlinePlayers());
        if (locatedSource instanceof Player) {
            playerCollection.remove(locatedSource);
        }

        // Remove players "out of this world", then sort players by distance from current location.
        return getNearestPlayerFromLocation(playerCollection, locatedSource.getLocation(), args);
    }

    static Player getNearestPlayerFromLocation(Collection<Player> playerCollection, Location<World> locationInWorld, CommandArgs args) throws ArgumentParseException {
        Vector3d currentLocation = locationInWorld.getPosition();
        return playerCollection.parallelStream()
                .filter(x -> x.getWorld().getUniqueId().equals(locationInWorld.getExtent().getUniqueId()))
                .map(x -> new Tuple<>(x, x.getLocation().getPosition().distanceSquared(currentLocation)))
                .min((x, y) -> x.getSecond().compareTo(y.getSecond()))
                .orElseThrow(() -> args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.selector.notarget")))
                .getFirst();
    }
}
