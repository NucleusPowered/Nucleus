/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.events;

import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.profile.GameProfile;

public class UserDataLoadedEvent extends AbstractEvent {

    private final Cause cause;
    private final IUserDataObject dataObject;
    private final GameProfile gameProfile;
    private boolean markedAsToSave = false;

    public UserDataLoadedEvent(Cause cause, IUserDataObject dataObject, GameProfile gameProfile) {
        this.cause = cause;
        this.dataObject = dataObject;
        this.gameProfile = gameProfile;
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }

    public IUserDataObject getDataObject() {
        return this.dataObject;
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public boolean shouldSave() {
        return this.markedAsToSave;
    }

    public void save() {
        this.markedAsToSave = true;
    }
}
