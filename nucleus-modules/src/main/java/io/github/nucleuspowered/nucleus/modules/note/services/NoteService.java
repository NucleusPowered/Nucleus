/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.services;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.note.NucleusNoteService;
import io.github.nucleuspowered.nucleus.api.module.note.data.Note;
import io.github.nucleuspowered.nucleus.modules.note.NoteKeys;
import io.github.nucleuspowered.nucleus.modules.note.event.CreateNoteEvent;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.IKeyedDataObject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@APIService(NucleusNoteService.class)
public class NoteService implements NucleusNoteService, ServiceBase {

    private final INucleusServiceCollection serviceCollection;

    @Inject
    public NoteService(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Override
    public CompletableFuture<Collection<Note>> getNotes(final UUID uuid) {
        return this.serviceCollection.storageManager()
                .getUserService().get(uuid)
                .thenApply(result -> result
                        .<Collection<Note>>flatMap(udo -> udo.get(NoteKeys.NOTE_DATA).map(ArrayList::new))
                        .orElseGet(Collections::emptyList));
    }

    @Override public CompletableFuture<Boolean> addNote(@Nullable final UUID uuid, final String note) {
        return this.addNote(uuid, new NucleusNote(uuid, note, Instant.now()));
    }

    public CompletableFuture<Boolean> addNote(final UUID user, final NucleusNote note) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(note);

        // Create the note event.
        final CreateNoteEvent event = new CreateNoteEvent(
                note.getNoter().orElse(null),
                note.getNote(),
                note.getDate(),
                user,
                Sponge.server().causeStackManager().currentCause()
        );
        Sponge.eventManager().post(event);

        return this.serviceCollection.storageManager().getUserService().getOrNew(user).thenApply(x -> {
            try (final IKeyedDataObject.Value<List<Note>> v = x.getAndSet(NoteKeys.NOTE_DATA)) {
                final List<Note> data = v.getValue().orElseGet(ArrayList::new);
                data.add(note);
                v.setValue(data);
            }
            this.serviceCollection.storageManager().getUserService().save(user, x);
            return true;
        });
    }

    @Override
    public CompletableFuture<Boolean> removeNote(final UUID uuid, final Note note) {
        return this.serviceCollection.storageManager().getUserService().get(uuid).thenApply(udo -> {
            if (udo.isPresent()) {
                try (final IKeyedDataObject.Value<List<Note>> v = udo.get().getAndSet(NoteKeys.NOTE_DATA)) {
                    if (v.getValue().isPresent()) {
                        final List<Note> data = new ArrayList<>(v.getValue().get());
                        if (data.removeIf(x -> note.equals(x))) {
                            v.setValue(data);
                            this.serviceCollection.storageManager().getUserService().save(uuid, udo.get());
                            return true;
                        }
                    }
                }
            }

            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> clearNotes(final UUID uuid) {
        return this.serviceCollection.storageManager().getUserService().get(uuid).thenApply(udo -> {
            if (udo.isPresent()) {
                udo.get().remove(NoteKeys.NOTE_DATA);
                this.serviceCollection.storageManager().getUserService().save(uuid, udo.get());
                return true;
            }

            return false;
        });
    }
}
