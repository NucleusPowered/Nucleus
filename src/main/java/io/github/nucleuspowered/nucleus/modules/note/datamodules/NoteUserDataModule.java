/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.datamodules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Note;
import io.github.nucleuspowered.nucleus.dataservices.modular.DataKey;
import io.github.nucleuspowered.nucleus.modules.note.data.NoteData;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.IUserDataModule;

import java.util.List;

public class NoteUserDataModule implements IUserDataModule {

    @DataKey("notes")
    private List<NoteData> notes = Lists.newArrayList();

    public List<NoteData> getNotes() {
        return ImmutableList.copyOf(this.notes);
    }

    public void addNote(NoteData note) {
        if (this.notes == null) {
            this.notes = Lists.newArrayList();
        }

        this.notes.add(note);
    }

    public boolean removeNote(Note note) {
        return this.notes.removeIf(x -> x.getNoterInternal().equals(note.getNoter().orElse(Util.consoleFakeUUID))
                && x.getNote().equals(note.getNote()));
    }

    public boolean clearNotes() {
        if (!this.notes.isEmpty()) {
            this.notes.clear();
            return true;
        } else {
            return false;
        }
    }
}
