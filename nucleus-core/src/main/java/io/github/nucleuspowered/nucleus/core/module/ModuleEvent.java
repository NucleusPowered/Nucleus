/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.module;

import io.github.nucleuspowered.nucleus.api.core.event.NucleusModuleSelectionEvent;
import io.github.nucleuspowered.nucleus.core.NucleusCore;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.plugin.PluginContainer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModuleEvent extends AbstractEvent implements NucleusModuleSelectionEvent {

    private final Cause cause;
    private final Map<String, NucleusCore.ModuleState> moduleStatus;
    private final Set<String> allModules;

    public ModuleEvent(final Cause cause, final Set<String> allModules, final Map<String, NucleusCore.ModuleState> moduleStatus) {
        this.cause = cause;
        this.allModules = Collections.unmodifiableSet(allModules);
        this.moduleStatus = new HashMap<>(moduleStatus);
    }

    @Override
    public Set<String> availableModules() {
        return this.allModules;
    }

    @Override
    public boolean disableModule(final String module, final PluginContainer plugin) throws IllegalArgumentException {
        if (!this.allModules.contains(module)) {
            throw new IllegalArgumentException("Module " + module + " does not exist");
        }
        if (this.moduleStatus.get(module) == NucleusCore.ModuleState.TRUE) {
            this.moduleStatus.put(module, NucleusCore.ModuleState.FALSE);
            return true;
        }

        return false;
    }

    @Override
    public Cause cause() {
        return this.cause;
    }

    public boolean shouldLoad(final String id) {
        return this.moduleStatus.getOrDefault(id, NucleusCore.ModuleState.TRUE).isShouldLoad();
    }

}
