/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fly;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.fly.config.FlyConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "fly", name = "Fly")
public class FlyModule extends ConfigurableModule<FlyConfigAdapter> {
    @Override
    public FlyConfigAdapter createAdapter() {
        return new FlyConfigAdapter();
    }
}
