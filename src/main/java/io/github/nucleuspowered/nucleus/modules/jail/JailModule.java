/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.JailData;
import io.github.nucleuspowered.nucleus.api.service.NucleusJailService;
import io.github.nucleuspowered.nucleus.internal.InternalServiceManager;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.jail.commands.CheckJailCommand;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.jail.handlers.JailHandler;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "jail", name = "Jail")
public class JailModule extends ConfigurableModule<JailConfigAdapter> {

    @Inject private NucleusPlugin nucleus;
    @Inject private Logger logger;
    @Inject private Game game;
    @Inject private InternalServiceManager serviceManager;

    @Override
    public JailConfigAdapter createAdapter() {
        return new JailConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        try {
            JailHandler jh = new JailHandler(nucleus);
            nucleus.getInjector().injectMembers(jh);
            game.getServiceManager().setProvider(nucleus, NucleusJailService.class, jh);
            serviceManager.registerService(JailHandler.class, jh);
        } catch (Exception ex) {
            logger.warn("Could not load the jail module for the reason below.");
            ex.printStackTrace();
            throw ex;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        createSeenModule(CheckJailCommand.class, (c, u) -> {

            // If we have a ban service, then check for a ban.
            JailHandler jh = nucleus.getInternalServiceManager().getService(JailHandler.class).get();
            if (jh.isPlayerJailed(u)) {
                JailData jd = jh.getPlayerJailData(u).get();
                Text.Builder m;
                if (jd.getEndTimestamp().isPresent()) {
                    m = NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("seen.isjailed.temp", Util.getTimeToNow(jd.getEndTimestamp().get())).toBuilder();
                } else {
                    m = NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("seen.isjailed.perm").toBuilder();
                }

                return Lists.newArrayList(
                        m.onClick(TextActions.runCommand("/checkjail " + u.getName()))
                                .onHover(TextActions.showText(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("standard.clicktoseemore"))).build(),
                        NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("standard.reason", jd.getReason()));
            }

            return Lists.newArrayList(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("seen.notjailed"));
        });
    }
}
