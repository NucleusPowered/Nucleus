/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note;

import io.github.nucleuspowered.nucleus.api.module.note.data.Note;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.playerinformation.NucleusProvider;
import io.github.nucleuspowered.nucleus.modules.note.commands.CheckNotesCommand;
import io.github.nucleuspowered.nucleus.modules.note.commands.ClearNotesCommand;
import io.github.nucleuspowered.nucleus.modules.note.commands.NoteCommand;
import io.github.nucleuspowered.nucleus.modules.note.commands.RemoveNoteCommand;
import io.github.nucleuspowered.nucleus.modules.note.config.NoteConfig;
import io.github.nucleuspowered.nucleus.modules.note.infoprovider.NoteInfoProvider;
import io.github.nucleuspowered.nucleus.modules.note.listeners.NoteListener;
import io.github.nucleuspowered.nucleus.modules.note.services.NoteService;
import io.github.nucleuspowered.nucleus.modules.note.services.NucleusNote;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public final class NoteModule implements IModule.Configurable<NoteConfig> {

    public final static String ID = "note";

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {
        serviceCollection.registerService(NoteService.class, new NoteService(serviceCollection), false);
        serviceCollection.game().dataManager().registerBuilder(Note.class, new NucleusNote.DataBuilder());
    }

    @Override
    public Collection<Class<? extends ICommandExecutor>> getCommands() {
        return Arrays.asList(
                CheckNotesCommand.class,
                ClearNotesCommand.class,
                NoteCommand.class,
                RemoveNoteCommand.class
        );
    }

    @Override
    public Optional<Class<?>> getPermissions() {
        return Optional.of(NotePermissions.class);
    }

    @Override public Optional<NucleusProvider> getInfoProvider() {
        return Optional.of(new NoteInfoProvider());
    }

    @Override
    public Collection<Class<? extends ListenerBase>> getListeners() {
        return Collections.singleton(NoteListener.class);
    }

    @Override
    public Class<NoteConfig> getConfigClass() {
        return NoteConfig.class;
    }
}
