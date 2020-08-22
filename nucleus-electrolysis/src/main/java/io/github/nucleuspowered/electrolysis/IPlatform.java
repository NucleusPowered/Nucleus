package io.github.nucleuspowered.electrolysis;

import io.github.nucleuspowered.electrolysis.event.IEventManager;
import io.github.nucleuspowered.electrolysis.player.IPlayer;
import io.github.nucleuspowered.electrolysis.player.IUser;
import io.github.nucleuspowered.electrolysis.task.IScheduler;
import io.github.nucleuspowered.electrolysis.world.IWorldManager;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IPlatform {

    boolean supportsCapability(Capability... capabilities);

    IServer getServer();

    Collection<IPlayer> getOnlinePlayers();

    Optional<IPlayer> getPlayer(UUID uuid);

    Optional<IUser> getUser(UUID uuid);

    IWorldManager getWorldManager();

    IEventManager getEventManager();

    IScheduler getScheduler();

}
