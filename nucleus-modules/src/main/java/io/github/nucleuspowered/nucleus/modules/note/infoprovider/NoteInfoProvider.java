/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.infoprovider;

import io.github.nucleuspowered.nucleus.modules.note.NotePermissions;
import io.github.nucleuspowered.nucleus.modules.note.services.NoteService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.playerinformation.NucleusProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;

import java.util.Optional;

public class NoteInfoProvider implements NucleusProvider {

    @Override
    public String getCategory() {
        return "note";
    }

    @Override
    public Optional<Component> get(final User user, final CommandCause source, final INucleusServiceCollection serviceCollection) {
        if (serviceCollection.permissionService().hasPermission(source, NotePermissions.BASE_CHECKNOTES)) {
            final int active = serviceCollection.getServiceUnchecked(NoteService.class).getNotes(user.uniqueId()).join().size();

            final Component r = serviceCollection.messageProvider().getMessageFor(source.audience(), "seen.notes", active);
            if (active > 0) {
                return Optional.of(
                        r.clickEvent(ClickEvent.runCommand("/nucleus:checknotes " + user.name()))
                                .hoverEvent(HoverEvent.showText(
                                        serviceCollection.messageProvider().getMessageFor(source.audience(), "standard.clicktoseemore"))));
            }

            return Optional.of(r);
        }

        return Optional.empty();
    }
}
