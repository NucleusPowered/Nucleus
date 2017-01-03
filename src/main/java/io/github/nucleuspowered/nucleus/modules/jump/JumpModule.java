/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jump;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.jump.config.JumpConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "jump", name = "Jump")
public class JumpModule extends ConfigurableModule<JumpConfigAdapter> {

    @Override
    public JumpConfigAdapter createAdapter() {
        return new JumpConfigAdapter();
    }
}
