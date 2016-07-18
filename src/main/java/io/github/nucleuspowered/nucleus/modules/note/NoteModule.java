/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.service.NucleusNoteService;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.note.config.NoteConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.note.handlers.NoteHandler;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import uk.co.drnaylor.quickstart.annotations.ModuleData;


@ModuleData(id = "note", name = "Note")
public class NoteModule extends ConfigurableModule<NoteConfigAdapter> {

    @Inject
    private Game game;
    @Inject private Logger logger;

    @Override
    public NoteConfigAdapter getAdapter() {
        return new NoteConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        try {
            NoteHandler noteHandler = new NoteHandler(nucleus);
            nucleus.getInjector().injectMembers(noteHandler);
            game.getServiceManager().setProvider(nucleus, NucleusNoteService.class, noteHandler);
            serviceManager.registerService(NoteHandler.class, noteHandler);
        } catch (Exception ex) {
            logger.warn("Could not load the note module for the reason below.");
            ex.printStackTrace();
            throw ex;
        }
    }
}
