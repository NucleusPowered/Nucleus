/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.parameter;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class JailArgument extends CommandElement {

    private final JailHandler handler;
    private final IMessageProviderService messageProvider;

    public JailArgument(@Nullable Text key, INucleusServiceCollection serviceCollection) {
        super(key);
        this.handler = serviceCollection.getServiceUnchecked(JailHandler.class);
        this.messageProvider = serviceCollection.messageProvider();
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        String a = args.next().toLowerCase();
        Optional<NamedLocation> owl = this.handler.getJail(a);
        if (owl.isPresent()) {
            return owl.get();
        }

        throw args.createError(this.messageProvider.getMessageFor(source, "args.jail.nojail"));
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        try {
            String a = args.peek().toLowerCase();
            return this.handler.getJails().keySet().stream().filter(x -> x.startsWith(a)).collect(Collectors.toList());
        } catch (ArgumentParseException e) {
            return Lists.newArrayList(this.handler.getJails().keySet());
        }
    }
}
