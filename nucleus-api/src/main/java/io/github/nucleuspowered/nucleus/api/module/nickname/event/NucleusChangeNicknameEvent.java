/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.nickname.event;

import io.github.nucleuspowered.nucleus.api.util.MightOccurAsync;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;

import java.util.Optional;

/**
 * Fired when a player requests or deletes a nickname.
 *
 * <p>Ensure that you listen for {@link Pre} or {@link Post}, rather than this
 * base event.</p>
 */
@MightOccurAsync
public interface NucleusChangeNicknameEvent extends Event {

    /**
     * The user whose nickname was changed.
     *
     * @return The {@link User}
     */
    User getUser();

    /**
     * The previous nickname for the {@link #getUser()}
     *
     * @return The previous nickname.
     */
    Optional<Text> getPreviousNickname();

    /**
     * The new nickname, if any, for the {@link #getUser()}
     *
     * @return The nickname, if any is given
     */
    Optional<Text> getNickname();

    @MightOccurAsync
    interface Pre extends NucleusChangeNicknameEvent, Cancellable { }

    @MightOccurAsync
    interface Post extends NucleusChangeNicknameEvent { }

}
