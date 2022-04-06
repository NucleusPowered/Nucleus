/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.parameter;

import io.github.nucleuspowered.nucleus.api.module.jail.data.Jail;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueParameter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JailParameter implements ValueParameter<Jail> {

    private final JailService handler;
    private final IMessageProviderService messageProvider;

    public JailParameter(final JailService service, final IMessageProviderService messageProviderService) {
        this.handler = service;
        this.messageProvider = messageProviderService;
    }

    @Override
    public List<CommandCompletion> complete(final CommandContext context, final String currentInput) {
        return this.handler.getJails().entrySet().stream().filter(x -> x.getKey().startsWith(currentInput))
                .map(x ->
                    x.getValue().getLocation().getLocation()
                            .map(loc -> CommandCompletion.of(x.getKey(), Component.text(loc.toString())))
                            .orElseGet(() -> CommandCompletion.of(x.getKey()))
                )
                .collect(Collectors.toList());
    }

    @Override
    public Optional<? extends Jail> parseValue(final Parameter.Key<? super Jail> parameterKey, final ArgumentReader.Mutable reader,
            final CommandContext.Builder context)
            throws ArgumentParseException {
        final String r = reader.parseString();
        final Optional<Jail> jail = this.handler.getJail(r);
        if (jail.isPresent()) {
            return jail;
        }
        throw reader.createException(this.messageProvider.getMessageFor(context.cause().audience(), "args.jail.nojail"));
    }
}
