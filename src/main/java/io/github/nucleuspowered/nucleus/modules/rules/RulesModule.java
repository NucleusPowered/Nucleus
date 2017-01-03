/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rules;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.rules.config.RulesConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "rules", name = "Rules")
public class RulesModule extends ConfigurableModule<RulesConfigAdapter> {

    @Override
    public RulesConfigAdapter createAdapter() {
        return new RulesConfigAdapter();
    }
}
