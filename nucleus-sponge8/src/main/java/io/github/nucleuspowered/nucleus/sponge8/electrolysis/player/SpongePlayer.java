package io.github.nucleuspowered.nucleus.sponge8.electrolysis.player;

import io.github.nucleuspowered.electrolysis.player.IPlayer;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.UUID;

public class SpongePlayer implements IPlayer {

    private final ServerPlayer player;

    public SpongePlayer(final ServerPlayer player) {
        this.player = player;
    }

    @Override
    public UUID getUniqueId() {
        return this.player.getUniqueId();
    }

    @Override
    public String getName() {
        return this.player.getName();
    }

}
