/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.services;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.note.NucleusNoteService;
import io.github.nucleuspowered.nucleus.api.module.note.data.Note;
import io.github.nucleuspowered.nucleus.modules.note.NoteKeys;
import io.github.nucleuspowered.nucleus.modules.note.data.NoteData;
import io.github.nucleuspowered.nucleus.modules.note.event.CreateNoteEvent;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.storage.dataobjects.keyed.IKeyedDataObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

@APIService(NucleusNoteService.class)
public class NoteHandler implements NucleusNoteService, ServiceBase {

    private final INucleusServiceCollection serviceCollection;

    @Inject
    public NoteHandler(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    public List<NoteData> getNotesInternal(User user) {
        Optional<IUserDataObject> userDataObject = this.serviceCollection.storageManager()
                .getUserService().getOnThread(user.getUniqueId());
        return userDataObject.flatMap(udo -> udo.get(NoteKeys.NOTE_DATA)).orElseGet(ImmutableList::of);
    }

    @Override public ImmutableList<Note> getNotes(User user) {
        return ImmutableList.copyOf(getNotesInternal(user));
    }

    @Override public boolean addNote(User user, CommandSource source, String note) {
        return addNote(user, new NoteData(Instant.now(), Util.getUUID(source), note));
    }

    public boolean addNote(User user, NoteData note) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(note);

        IUserDataObject udo = this.serviceCollection.storageManager().getUserService().getOrNewOnThread(user.getUniqueId());
        try (IKeyedDataObject.Value<List<NoteData>> v = udo.getAndSet(NoteKeys.NOTE_DATA)) {
            List<NoteData> data = v.getValue().orElseGet(Lists::newArrayList);
            data.add(note);
            v.setValue(data);
        }

        // Create the note event.
        CreateNoteEvent event = new CreateNoteEvent(
                note.getNoterInternal(),
                note.getNote(),
                note.getDate(),
                user,
                Sponge.getCauseStackManager().getCurrentCause()
        );
        Sponge.getEventManager().post(event);
        this.serviceCollection.storageManager().getUserService().save(user.getUniqueId(), udo);
        return true;
    }

    @Override
    public boolean removeNote(User user, Note note) {
        Optional<IUserDataObject> udo = this.serviceCollection.storageManager().getUserService()
                .getOnThread(user.getUniqueId());
        if (udo.isPresent()) {
            boolean res;
            try (IKeyedDataObject.Value<List<NoteData>> v = udo.get().getAndSet(NoteKeys.NOTE_DATA)) {
                List<NoteData> data = v.getValue().orElseGet(Lists::newArrayList);
                res = data.removeIf(x -> x.equals(note));
                v.setValue(data);
            }

            this.serviceCollection.storageManager().getUserService().save(user.getUniqueId(), udo.get());
            return res;
        }

        return false;
    }

    @Override
    public boolean clearNotes(User user) {
        Optional<IUserDataObject> udo = this.serviceCollection.storageManager().getUserService()
                .getOnThread(user.getUniqueId());
        if (udo.isPresent()) {
            udo.get().remove(NoteKeys.NOTE_DATA);
            this.serviceCollection.storageManager().getUserService().save(user.getUniqueId(), udo.get());
            return true;
        }

        return false;
    }
}
