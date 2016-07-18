/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.service;

import io.github.nucleuspowered.nucleus.api.data.NoteData;
import org.spongepowered.api.entity.living.player.User;

import java.util.List;

/**
 * A service that determines whether a player has notes.
 */
public interface NucleusNoteService {

    /**
     * Gets all notes for a specific user
     *
     * @param user The {@link User} to check.
     * @return A list of {@link NoteData}.
     */
    List<NoteData> getNotes(User user);

    /**
     * Adds a note to a player.
     *
     * @param user The {@link User} to add a note to.
     * @param note The {@link NoteData} to add.
     * @return <code>true</code> if the note was added.
     */
    boolean addNote(User user, NoteData note);

    /**
     * Removes a note from a player.
     *
     * @param user The {@link User} to remove a note from.
     * @param note The {@link NoteData} to remove.
     * @return <code>true</code> if the note was removed.
     */
    boolean removeNote(User user, NoteData note);

    /**
     * Clears all notes from a player.
     *
     * @param user The {@link User} to remove all notes from.
     * @return <code>true</code> if all notes were removed.
     */
    boolean clearNotes(User user);
}
