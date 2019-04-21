/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.catalogkeys.NucleusTeleportHelperFilters;
import io.github.nucleuspowered.nucleus.internal.CatalogTypeFinalStaticProcessor;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.core.teleport.filters.NoCheckFilter;
import io.github.nucleuspowered.nucleus.modules.core.teleport.filters.WallCheckFilter;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = CoreModule.ID, name = "Core", isRequired = true)
public class CoreModule extends ConfigurableModule<CoreConfigAdapter> {

    public static final String ID = "core";

    @Override
    public CoreConfigAdapter createAdapter() {
        return new CoreConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        CatalogTypeFinalStaticProcessor.setFinalStaticFields(
                NucleusTeleportHelperFilters.class,
                ImmutableMap.<String, TeleportHelperFilter>builder()
                    .put("NO_CHECK", new NoCheckFilter())
                    .put("WALL_CHECK", new WallCheckFilter())
                    .build()
        );

        Nucleus.getNucleus().reloadMessages();
    }

    @Override
    protected void performPostTasks() {
        super.performPostTasks();

        Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(UserPreferenceService.class).postInit();
    }
}
