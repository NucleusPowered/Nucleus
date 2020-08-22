/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname.events;

import io.github.nucleuspowered.nucleus.api.module.nickname.event.NucleusChangeNicknameEvent;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.text.Text;

import java.util.Optional;

import javax.annotation.Nullable;

public class ChangeNicknameEventPre extends AbstractEvent implements NucleusChangeNicknameEvent.Pre {

    private final Cause cause;
    private final User target;
    @Nullable private final Text previousNickname;
    @Nullable private final Text newNickname;
    private boolean cancel = false;

    public ChangeNicknameEventPre(Cause cause, @Nullable Text previousNickname, @Nullable Text newNickname, User target) {
        this.cause = cause;
        this.previousNickname = previousNickname;
        this.newNickname = newNickname;
        this.target = target;
    }

    @Override public User getUser() {
        return this.target;
    }

    @Override
    public Optional<Text> getPreviousNickname() {
        return Optional.ofNullable(this.previousNickname);
    }

    @Override public Optional<Text> getNickname() {
        return Optional.ofNullable(this.newNickname);
    }

    @Override
    public boolean isCancelled() {
        return this.cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }

}
