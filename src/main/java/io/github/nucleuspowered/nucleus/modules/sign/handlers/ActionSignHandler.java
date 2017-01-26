/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.sign.handlers;

import io.github.nucleuspowered.nucleus.internal.signs.SignDataListenerBase;
import io.github.nucleuspowered.nucleus.spongedata.manipulators.AbstractSignManipulator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ActionSignHandler {

    private final Map<Class<? extends AbstractSignManipulator<?, ?>>, SignDataListenerBase<?>> listeners = new HashMap<>();

    public void registerSignInteraction(SignDataListenerBase<? extends AbstractSignManipulator<?, ?>> onInteract) {

        if (listeners.containsKey(onInteract.getDataClass())) {
            throw new IllegalArgumentException();
        }

        listeners.put(onInteract.getDataClass(), onInteract);
    }

    public Collection<SignDataListenerBase<?>> getListeners() {
        return listeners.values();
    }

    public Collection<Class<? extends AbstractSignManipulator<?, ?>>> getRegisteredClasses() {
        return listeners.keySet();
    }
}
