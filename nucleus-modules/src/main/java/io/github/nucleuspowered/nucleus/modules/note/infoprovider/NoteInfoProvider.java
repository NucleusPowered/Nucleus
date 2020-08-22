/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.infoprovider;

import io.github.nucleuspowered.nucleus.modules.note.NotePermissions;
import io.github.nucleuspowered.nucleus.modules.note.services.NoteHandler;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.playerinformation.NucleusProvider;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import java.util.Optional;

public class NoteInfoProvider implements NucleusProvider {

    @Override
    public String getCategory() {
        return "note";
    }

    @Override
    public Optional<Text> get(User user, CommandSource source, INucleusServiceCollection serviceCollection) {
        if (serviceCollection.permissionService().hasPermission(source, NotePermissions.BASE_CHECKNOTES)) {
            int active = serviceCollection.getServiceUnchecked(NoteHandler.class).getNotesInternal(user).size();

            Text r = serviceCollection.messageProvider().getMessageFor(source, "seen.notes", active);
            if (active > 0) {
                return Optional.of(
                        r.toBuilder().onClick(TextActions.runCommand("/checknotes " + user.getName()))
                                .onHover(TextActions.showText(
                                        serviceCollection.messageProvider().getMessageFor(source, "standard.clicktoseemore")))
                                .build());
            }

            return Optional.of(r);
        }

        return Optional.empty();
    }
}
