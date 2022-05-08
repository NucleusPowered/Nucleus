/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core;

import io.github.nucleuspowered.nucleus.core.module.IModuleProvider;
import io.github.nucleuspowered.nucleus.core.module.ModuleContainer;

import java.util.Collection;
import java.util.Collections;

public final class CoreModuleProvider implements IModuleProvider {

    @Override
    public Collection<ModuleContainer> getModules() {
        return Collections.singleton(new ModuleContainer(CoreModule.ID, "Core", true, CoreModule.class));
    }
}
