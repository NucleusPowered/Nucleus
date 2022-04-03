/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.services.impl.commandelement.CommandElementSupplier;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.Locale;
import java.util.UUID;


@ImplementedBy(CommandElementSupplier.class)
public interface ICommandElementSupplier {

    Parameter.Value<Locale> createLocaleElement(String key);

    Parameter.Value<UUID> createOnlyOtherUserPermissionElement(String permission);

    Parameter.Value<ServerPlayer> createOnlyOtherPlayerPermissionElement(String permission);

    User getUserFromParametersElseSelf(ICommandContext context) throws CommandException;
}
