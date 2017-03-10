/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.environment.config.EnvironmentConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "environment", name = "Environment")
public class EnvironmentModule extends ConfigurableModule<EnvironmentConfigAdapter> {

    @Override
    public EnvironmentConfigAdapter createAdapter() {
        return new EnvironmentConfigAdapter();
    }
}
