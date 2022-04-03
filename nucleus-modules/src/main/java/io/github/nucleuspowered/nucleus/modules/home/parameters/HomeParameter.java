/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.parameters;

import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.modules.home.services.HomeService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueParameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class HomeParameter implements ValueParameter<Home> {

    public static final Parameter.Key<UUID> OTHER_PLAYER_KEY = Parameter.key("other player", TypeToken.get(UUID.class));

    private final HomeService homeService;
    protected final IMessageProviderService messageProviderService;

    public HomeParameter(final HomeService homeService, final IMessageProviderService messageProviderService) {
        this.homeService = homeService;
        this.messageProviderService = messageProviderService;
    }

    @Override
    public List<CommandCompletion> complete(final CommandContext context, final String currentInput) {
        final Collection<String> s;
        try {
            s = this.getTarget(context)
                    .map(this.homeService::getHomeNames)
                    .orElseGet(Collections::emptyList);
        } catch (final Exception e) {
            return Collections.emptyList();
        }

        final String name = currentInput.toLowerCase();
        return s.stream().filter(x -> x.toLowerCase().startsWith(name)).limit(20)
                .map(CommandCompletion::of)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<? extends Home> parseValue(
            final Parameter.Key<? super Home> parameterKey,
            final ArgumentReader.Mutable reader,
            final CommandContext.Builder context) throws ArgumentParseException {

        final UUID target = this.getTarget(context).orElseThrow(() ->
                        reader.createException(this.messageProviderService.getMessageFor(context.cause().audience(), "command.playeronly")));

        final String home = reader.parseString();
        try {
            final Optional<Home> owl = this.homeService.getHome(target, home);
            if (owl.isPresent()) {
                return owl;
            }
        } catch (final Exception e) {
            e.printStackTrace();
            throw reader.createException(Component.text("An unspecified error occurred"));
        }

        throw reader.createException(this.messageProviderService.getMessageFor(context.cause().audience(), "args.home.nohome", home));
    }

    private Optional<UUID> getTarget(final CommandContext context) {
        if (context.hasAny(HomeParameter.OTHER_PLAYER_KEY)) {
            return Optional.of(context.requireOne(HomeParameter.OTHER_PLAYER_KEY));
        } else if (context.cause().root() instanceof ServerPlayer) {
            return Optional.of(((ServerPlayer) context.cause().root()).uniqueId());
        } else {
            return Optional.empty();
        }
    }

}
