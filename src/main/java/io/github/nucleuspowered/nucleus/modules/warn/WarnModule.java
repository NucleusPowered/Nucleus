/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarnService;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.warn.commands.CheckWarningsCommand;
import io.github.nucleuspowered.nucleus.modules.warn.config.WarnConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warn.handlers.WarnHandler;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "warn", name = "Warn")
public class WarnModule extends ConfigurableModule<WarnConfigAdapter> {

    @Inject private Game game;
    @Inject private Logger logger;

    @Override
    public WarnConfigAdapter createAdapter() {
        return new WarnConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        try {
            WarnHandler warnHandler = new WarnHandler(plugin);
            plugin.getInjector().injectMembers(warnHandler);
            game.getServiceManager().setProvider(plugin, NucleusWarnService.class, warnHandler);
            serviceManager.registerService(WarnHandler.class, warnHandler);
        } catch (Exception ex) {
            logger.warn("Could not load the warn module for the reason below.");
            ex.printStackTrace();
            throw ex;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

        // Take base permission from /checkwarnings.
        createSeenModule(CheckWarningsCommand.class, (c, u) -> {

            WarnHandler jh = plugin.getInternalServiceManager().getService(WarnHandler.class).get();
            int active = jh.getWarnings(u, true, false).size();
            int expired = jh.getWarnings(u, false, true).size();

            Text r = plugin.getMessageProvider().getTextMessageWithFormat("seen.warnings", String.valueOf(active), String.valueOf(expired));
            if (active > 0) {
                return Lists.newArrayList(
                        r.toBuilder().onClick(TextActions.runCommand("/checkwarnings " + u.getName()))
                                .onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("standard.clicktoseemore"))).build());
            }

            return Lists.newArrayList(r);
        });
    }
}
