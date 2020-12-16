/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.scaffold.command.parameter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerDisplayNameService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerOnlineService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Identifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class DisplayNameArgument extends CommandElement {

    private static boolean init = false;
    private static int USER_LIMIT = 20;
    private static boolean PARTIAL_MATCH = true;
    private final IPlayerOnlineService playerOnlineService;

    public static void onReload(INucleusServiceCollection serviceCollection) {
        CoreConfig cc = serviceCollection.moduleDataProvider().getModuleConfig(CoreConfig.class);
        USER_LIMIT = Math.max(cc.getNicknameArgOfflineLimit(), 0);
        PARTIAL_MATCH = cc.isPartialMatch();
    }

    private final Target target;
    private final IPlayerDisplayNameService displayNameService;
    private final IMessageProviderService messageProviderService;
    // @Nullable private final NicknameService nicknameService;

    public DisplayNameArgument(@Nullable Text key, Target target, INucleusServiceCollection serviceCollection) {
        super(key);
        this.target = target;
        if (!init) {
            init = true;
            serviceCollection.reloadableService().registerReloadable(DisplayNameArgument::onReload);
        }

        this.displayNameService = serviceCollection.playerDisplayNameService();
        this.messageProviderService = serviceCollection.messageProvider();
        this.playerOnlineService = serviceCollection.playerOnlineService();
        // this.nicknameService = serviceCollection.getServiceUnchecked(NicknameService.class);
    }

    public enum Target {
        PLAYER,
        PLAYER_CONSOLE,
        USER
    }

    @Nullable
    @Override
    public Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        String toParse = args.next();
        return parseValue(source, toParse, (key, entry) -> args.createError(
                this.messageProviderService.getMessageFor(source.getLocale(), key, entry)));
    }

    public Set<?> parseValue(CommandSource source,
        String toParse, BiFunction<String, String, ArgumentParseException> exceptionSupplier) throws ArgumentParseException {

        if (target == Target.PLAYER_CONSOLE && toParse.equalsIgnoreCase("-")) {
            return ImmutableSet.of(Sponge.getServer().getConsole());
        }

        final Predicate<Player> shouldShow = determinePredicate(source);

        boolean playerOnly = toParse.startsWith("p:");
        if (playerOnly) {
            toParse = toParse.substring(2);
        }

        // Does the player exist?
        Optional<Player> player = Sponge.getServer().getPlayer(toParse);
        if (player.isPresent() && this.playerOnlineService.isOnline(source, player.get())) {
            return ImmutableSet.of(player.get()); // exact match.
        }

        if (playerOnly) {
            throw exceptionSupplier.apply("args.user.nouser", toParse);
        }

        // offline users take precedence over nicknames
        UserStorageService uss = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
        if (!toParse.isEmpty() && this.target == Target.USER) {
            Optional<User> user = uss.get(toParse);
            if (user.isPresent()) {
                return Collections.singleton(user.get());
            }
        }

        Optional<Player> op = this.displayNameService.getPlayer(toParse.toLowerCase());
        if (op.map(u -> this.playerOnlineService.isOnline(source, u)).isPresent()) {
            return Collections.singleton(op.get());
        }

        if (!PARTIAL_MATCH) {
            // no match
            throw exceptionSupplier.apply("args.user.nouser", toParse);
        }

        if (toParse.length() < 3) {
            throw exceptionSupplier.apply("args.user.nouserfuzzy", toParse);
        }

        Set<User> users = new HashSet<>();
        final String parse = toParse.toLowerCase();
        // fuzzy matching time.
        // players that match
        this.displayNameService.startsWith(parse)
                .keySet()
                .stream()
                .map(strings -> Sponge.getServer().getPlayer(strings).orElse(null))
                .filter(shouldShow)
                .filter(Objects::nonNull)
                .limit(USER_LIMIT)
                .forEach(users::add);

        List<UUID> uuids = users.stream().map(Identifiable::getUniqueId).collect(Collectors.toList());
        if (this.target == Target.USER) {
            if (PARTIAL_MATCH) {
                // This may add vanished players, but that's OK because we're showing all users anyway,
                // AND if they were hidden, it could give away that they were vanished.
                uss.match(parse).stream()
                        .map(x -> uss.get(x).orElse(null))
                        .filter(Objects::nonNull)
                        .filter(x -> !uuids.contains(x.getUniqueId()))
                        .limit(USER_LIMIT)
                        .forEach(users::add);
            } else {
                uss.get(parse).filter(x -> !uuids.contains(x.getUniqueId())).ifPresent(users::add);
            }
        }

        if (users.isEmpty()) {
            throw exceptionSupplier.apply("args.user.nouser", toParse);
        }

        return new HashSet<>(users);
    }

    @Override
    public List<String> complete(CommandSource source, CommandArgs args, CommandContext context) {
        List<String> names = new ArrayList<>();
        try {
            String toParse = args.peek();
            final boolean playerOnly = toParse.startsWith("p:");
            if (playerOnly) {
                toParse = toParse.substring(2);
            }

            String parse = toParse.toLowerCase();
            final Predicate<Player> shouldShow = determinePredicate(source);
            final Predicate<Player> partial = x -> x.getName().toLowerCase().startsWith(parse);

            UserStorageService uss = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
            Sponge.getServer().getOnlinePlayers().stream()
                    .filter(partial.and(shouldShow))
                    .forEach(player -> {
                        if (playerOnly) {
                            names.add("p:" + player.getName());
                        } else {
                            names.add(player.getName());
                        }
                    });

            if (!playerOnly) {
                this.displayNameService
                        .startsWith(parse)
                        .entrySet()
                        .stream()
                        .map(x ->
                                Sponge.getServer().getPlayer(x.getKey())
                                        .filter(shouldShow)
                                        .map(y -> x.getValue())
                                        .orElse(ImmutableList.of())
                        )
                        .filter(Objects::nonNull)
                        .forEach(names::addAll);

                if (USER_LIMIT > 0 && this.target == Target.USER) {
                    if (!parse.isEmpty()) {
                        uss.match(parse).stream()
                                .map(x -> uss.get(x).map(User::getName).orElse(null))
                                .filter(Objects::nonNull)
                                .limit(USER_LIMIT)
                                .forEach(names::add);
                    }
                }
            }

            return names;
        } catch (ArgumentParseException ex) {
            return names;
        }
    }

    private Predicate<Player> determinePredicate(CommandSource source) {
        return p -> this.playerOnlineService.isOnline(source, p);
    }


}
