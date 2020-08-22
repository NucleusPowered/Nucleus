package io.github.nucleuspowered.nucleus.sponge8.electrolysis;

import io.github.nucleuspowered.electrolysis.Capability;
import io.github.nucleuspowered.electrolysis.IPlatform;
import io.github.nucleuspowered.electrolysis.IServer;
import io.github.nucleuspowered.electrolysis.event.IEventManager;
import io.github.nucleuspowered.electrolysis.player.IPlayer;
import io.github.nucleuspowered.electrolysis.player.IUser;
import io.github.nucleuspowered.electrolysis.task.IScheduler;
import io.github.nucleuspowered.electrolysis.world.IWorldManager;
import io.github.nucleuspowered.nucleus.sponge8.electrolysis.player.SpongePlayer;
import io.github.nucleuspowered.nucleus.sponge8.electrolysis.player.SpongeUser;
import io.github.nucleuspowered.nucleus.sponge8.electrolysis.world.SpongeWorldManager;
import org.spongepowered.api.Sponge;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpongePlatform implements IPlatform {

    private final SpongeServer server = new SpongeServer();
    private final SpongeWorldManager worldManager = new SpongeWorldManager();

    @Override
    public boolean supportsCapability(Capability... capabilities) {
        return true;
    }

    @Override
    public IServer getServer() {
        return this.server;
    }

    @Override
    public Collection<IPlayer> getOnlinePlayers() {
        return Sponge.getServer().getOnlinePlayers()
                    .stream()
                    .map(SpongePlayer::new)
                    .collect(Collectors.toList());
    }

    @Override
    public Optional<IPlayer> getPlayer(UUID uuid) {
        return Sponge.getServer().getPlayer(uuid).map(SpongePlayer::new);
    }

    @Override
    public Optional<IUser> getUser(UUID uuid) {
        return Sponge.getServer().getUserManager().get(uuid).map(SpongeUser::new);
    }

    @Override
    public IWorldManager getWorldManager() {
        return this.worldManager;
    }

    @Override
    public IEventManager getEventManager() {
        return null;
    }

    @Override
    public IScheduler getScheduler() {
        return null;
    }

}
