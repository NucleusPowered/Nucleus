/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.service.NucleusNoteService;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.note.commands.CheckNotesCommand;
import io.github.nucleuspowered.nucleus.modules.note.config.NoteConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.note.handlers.NoteHandler;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = NoteModule.ID, name = "Note")
public class NoteModule extends ConfigurableModule<NoteConfigAdapter> {

    public final static String ID = "note";

    @Inject private Game game;
    @Inject private Logger logger;

    @Override
    public NoteConfigAdapter createAdapter() {
        return new NoteConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        try {
            NoteHandler noteHandler = new NoteHandler(plugin);
            plugin.getInjector().injectMembers(noteHandler);
            game.getServiceManager().setProvider(plugin, NucleusNoteService.class, noteHandler);
            serviceManager.registerService(NoteHandler.class, noteHandler);
        } catch (Exception ex) {
            logger.warn("Could not load the note module for the reason below.");
            ex.printStackTrace();
            throw ex;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

        // Take base permission from /checkwarnings.
        createSeenModule(CheckNotesCommand.class, (c, u) -> {

            NoteHandler jh = plugin.getInternalServiceManager().getService(NoteHandler.class).get();
            int active = jh.getNotes(u).size();

            Text r = plugin.getMessageProvider().getTextMessageWithFormat("seen.notes", String.valueOf(active));
            if (active > 0) {
                return Lists.newArrayList(
                        r.toBuilder().onClick(TextActions.runCommand("/checknotes " + u.getName()))
                                .onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("standard.clicktoseemore"))).build());
            }

            return Lists.newArrayList(r);
        });
    }
}
