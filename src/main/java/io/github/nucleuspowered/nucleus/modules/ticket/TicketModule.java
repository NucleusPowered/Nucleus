/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.query.NucleusTicketQuery;
import io.github.nucleuspowered.nucleus.api.service.NucleusTicketService;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.ticket.config.TicketConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.ticket.data.TicketDataManager;
import io.github.nucleuspowered.nucleus.modules.ticket.data.TicketQueryBuilder;
import io.github.nucleuspowered.nucleus.modules.ticket.handlers.TicketHandler;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "ticketing", name = "Ticketing")
public class TicketModule extends ConfigurableModule<TicketConfigAdapter> {
    @Inject private Game game;
    @Inject private Logger logger;

    @Override
    public TicketConfigAdapter createAdapter() {
        return new TicketConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        try {
            TicketHandler ticketHandler = new TicketHandler(plugin);
            plugin.getInjector().injectMembers(ticketHandler);
            game.getServiceManager().setProvider(plugin, NucleusTicketService.class, ticketHandler);
            serviceManager.registerService(TicketHandler.class, ticketHandler);

            TicketDataManager ticketDataManager = plugin.getInjector().getInstance(TicketDataManager.class);
            ticketDataManager.createTables();

            Sponge.getGame().getRegistry().registerBuilderSupplier(NucleusTicketQuery.Builder.class, TicketQueryBuilder::new);
        } catch (Exception ex) {
            logger.warn("Could not load the ticketing module for the reason below.");
            ex.printStackTrace();
            throw ex;
        }
    }
}