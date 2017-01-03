/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.rtp.config.RTPConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "rtp", name = "rtp")
public class RTPModule extends ConfigurableModule<RTPConfigAdapter> {

    @Override
    public RTPConfigAdapter createAdapter() {
        return new RTPConfigAdapter();
    }
}
