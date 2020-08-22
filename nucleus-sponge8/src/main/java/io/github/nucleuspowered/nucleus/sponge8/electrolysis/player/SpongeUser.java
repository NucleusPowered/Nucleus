package io.github.nucleuspowered.nucleus.sponge8.electrolysis.player;

import io.github.nucleuspowered.electrolysis.player.IUser;
import org.spongepowered.api.entity.living.player.User;

public class SpongeUser implements IUser {

    private final User user;

    public SpongeUser(User user) {
        this.user = user;
    }

}
