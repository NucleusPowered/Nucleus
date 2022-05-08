/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.event;

import io.github.nucleuspowered.nucleus.core.module.IModuleProvider;
import org.spongepowered.api.event.Event;

public interface RegisterModuleEvent extends Event {

    void registerModuleProvider(final IModuleProvider provider);

}
