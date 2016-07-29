/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.handlers;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.data.NoteData;
import io.github.nucleuspowered.nucleus.api.service.NucleusNoteService;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import org.spongepowered.api.entity.living.player.User;

import java.util.List;
import java.util.Optional;

public class NoteHandler implements NucleusNoteService {

    private final Nucleus nucleus;
    @Inject
    private UserDataManager userDataManager;

    public NoteHandler(Nucleus nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public List<NoteData> getNotes(User user) {
        Optional<UserService> userService = userDataManager.get(user);
        if (userService.isPresent()) {
            return userService.get().getNotes();
        }
        return Lists.newArrayList();
    }

    @Override
    public boolean addNote(User user, NoteData note) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(note);

        Optional<UserService> optUserService = userDataManager.get(user);
        if (!optUserService.isPresent()) {
            return false;
        }
        UserService userService = optUserService.get();

        userService.addNote(note);
        return true;
    }

    @Override
    public boolean removeNote(User user, NoteData note) {
        Optional<UserService> userService = userDataManager.get(user);
        if (userService.isPresent()) {
            userService.get().removeNote(note);
            return true;
        }

        return false;
    }

    @Override
    public boolean clearNotes(User user) {
        Optional<UserService> userService = userDataManager.get(user);
        if (userService.isPresent()) {
            userService.get().clearNotes();
            return true;
        }

        return false;
    }
}
