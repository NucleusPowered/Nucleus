/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.sign;

import static io.github.nucleuspowered.nucleus.modules.sign.SignModule.ID;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.sign.config.SignConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.sign.handlers.ActionSignHandler;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = ID, name = "Sign")
public class SignModule extends ConfigurableModule<SignConfigAdapter> {

    public static final String ID = "sign";

    @Override public SignConfigAdapter createAdapter() {
        return new SignConfigAdapter();
    }

    @Override protected void performPreTasks() throws Exception {
        super.performPreTasks();

        ActionSignHandler ash = new ActionSignHandler();
        plugin.getInternalServiceManager().registerService(ActionSignHandler.class, ash);
    }
}
