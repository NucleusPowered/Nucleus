/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.core.core.CoreKeys;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlayerInformationService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlayerOnlineService;
import io.github.nucleuspowered.nucleus.modules.misc.commands.SpeedCommand;
import io.github.nucleuspowered.nucleus.modules.playerinfo.PlayerInfoPermissions;
import io.github.nucleuspowered.nucleus.modules.playerinfo.services.SeenHandler;
import io.vavr.Function3;
import io.vavr.control.Either;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@EssentialsEquivalent("seen")
@Command(
        aliases = {"seen", "seenplayer", "lookup"},
        basePermission = PlayerInfoPermissions.BASE_SEEN,
        commandDescriptionKey = "seen",
        associatedPermissions = {
            PlayerInfoPermissions.SEEN_EXTENDEDPERMS_UUID,
            PlayerInfoPermissions.SEEN_EXTENDEDPERMS_IP,
            PlayerInfoPermissions.SEEN_EXTENDEDPERMS_FIRSTPLAYED,
            PlayerInfoPermissions.SEEN_EXTENDEDPERMS_LASTPLAYED,
            PlayerInfoPermissions.SEEN_EXTENDEDPERMS_SPEED_WALKING,
            PlayerInfoPermissions.SEEN_EXTENDEDPERMS_SPEED_FLYING,
            PlayerInfoPermissions.SEEN_EXTENDEDPERMS_LOCATION,
            PlayerInfoPermissions.SEEN_EXTENDEDPERMS_CAN_FLY,
            PlayerInfoPermissions.SEEN_EXTENDEDPERMS_IS_FLYING,
            PlayerInfoPermissions.SEEN_EXTENDEDPERMS_GAMEMODE
        }
)
public class SeenCommand implements ICommandExecutor {

    private static final NumberFormat NUMBER_FORMATTER = new DecimalFormat("0.00");

    // keeps order!
    private final Map<String, Function3<ICommandContext, User, IUserDataObject, Component>> entries;

    @Inject
    public SeenCommand() {
        final Map<String, Function3<ICommandContext, User, IUserDataObject, Component>> m = new LinkedHashMap<>();
        m.put(PlayerInfoPermissions.SEEN_EXTENDEDPERMS_UUID, this::getUUID);
        m.put(PlayerInfoPermissions.SEEN_EXTENDEDPERMS_IP, this::getIP);
        m.put(PlayerInfoPermissions.SEEN_EXTENDEDPERMS_FIRSTPLAYED, this::getFirstPlayed);
        m.put(PlayerInfoPermissions.SEEN_EXTENDEDPERMS_LASTPLAYED, this::getLastPlayed);
        m.put(PlayerInfoPermissions.SEEN_EXTENDEDPERMS_SPEED_WALKING, this::getWalkingSpeed);
        m.put(PlayerInfoPermissions.SEEN_EXTENDEDPERMS_SPEED_FLYING, this::getFlyingSpeed);
        m.put(PlayerInfoPermissions.SEEN_EXTENDEDPERMS_LOCATION, this::getLocation);
        m.put(PlayerInfoPermissions.SEEN_EXTENDEDPERMS_CAN_FLY, this::getCanFly);
        m.put(PlayerInfoPermissions.SEEN_EXTENDEDPERMS_IS_FLYING, this::getIsFlying);
        m.put(PlayerInfoPermissions.SEEN_EXTENDEDPERMS_GAMEMODE, this::getGameMode);
        this.entries = Collections.unmodifiableMap(m);
    }

    @Nullable
    private Component getUUID(final ICommandContext context, final User user, final IUserDataObject userDataModule) {
        return context.getMessage("command.seen.uuid", user.uniqueId());
    }

    @Nullable
    private Component getIP(final ICommandContext context, final User user, final IUserDataObject userDataModule) {
        @Nullable final Tuple<Component, String> res = user.player()
                    .map(pl -> Tuple.of(
                            context.getMessage("command.seen.ipaddress",
                                    pl.connection().address().getAddress().toString()),
                                    pl.connection().address().getAddress().toString()))
                    .orElseGet(() -> userDataModule.get(CoreKeys.IP_ADDRESS).map(x ->
                            Tuple.of(
                                    context.getMessage("command.seen.lastipaddress", x),
                                    x
                            )).orElse(null));
        if (res != null) {
            if (Sponge.server().commandManager().commandMapping("nucleus:getfromip").isPresent()) {
                return res.first()
                        .hoverEvent(HoverEvent.showText(context.getMessage("command.seen.ipclick")))
                        .clickEvent(ClickEvent.runCommand("/nucleus:getfromip " + res.second().replaceAll("^/", "")));
            }
            return res.first();
        }

        return null;
    }

    @Nullable
    private Component getFirstPlayed(final ICommandContext context, final User user, final IUserDataObject userDataModule) {
        Optional<Instant> i = user.get(Keys.FIRST_DATE_JOINED);
        return i.map(x -> context.getMessage("command.seen.firstplayed",
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(context.getLocale())
                        .withZone(ZoneId.systemDefault()).format(x))).orElse(null);
    }

    @Nullable
    private Component getLastPlayed(final ICommandContext context, final User user, final IUserDataObject userDataModule) {
        if (user.isOnline()) {
            return null;
        }

        return user.get(Keys.LAST_DATE_PLAYED).map(x -> context.getMessage("command.seen.lastplayed",
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(context.getLocale())
                        .withZone(ZoneId.systemDefault()).format(x))).orElse(null);
    }

    @Nullable
    private Component getLocation(final ICommandContext context, final User user, final IUserDataObject userDataModule) {
        if (user.isOnline()) {
            return this.getLocationString("command.seen.currentlocation", user.player().get().serverLocation(), context);
        }

        return this.getLocationString("command.seen.lastlocation", user.worldKey(), user.position(), context);
    }

    @Nullable
    private Component getWalkingSpeed(final ICommandContext context, final User user, final IUserDataObject userDataModule) {
        return user.get(Keys.WALKING_SPEED)
                .map(x -> context.getMessage("command.seen.speed.walk", NUMBER_FORMATTER.format(x * SpeedCommand.multiplier)))
                .orElse(null);
    }

    @Nullable
    private Component getFlyingSpeed(final ICommandContext context, final User user, final IUserDataObject userDataModule) {
        return user.get(Keys.FLYING_SPEED)
                .map(x -> context.getMessage("command.seen.speed.fly", NUMBER_FORMATTER.format(x * SpeedCommand.multiplier)))
                .orElse(null);
    }

    @Nullable
    private Component getCanFly(final ICommandContext context, final User user, final IUserDataObject userDataModule) {
        return context.getMessage("command.seen.canfly", this.getYesNo(user.get(Keys.CAN_FLY).orElse(false), context));
    }

    @Nullable
    private Component getIsFlying(final ICommandContext context, final User user, final IUserDataObject userDataModule) {
        return context.getMessage("command.seen.isflying", this.getYesNo(user.get(Keys.IS_FLYING).orElse(false), context));
    }

    @Nullable
    private Component getGameMode(final ICommandContext context, final User user, final IUserDataObject userDataModule) {
        return user.get(Keys.GAME_MODE).map(x -> context.getMessage("command.seen.gamemode", x.asComponent())).orElse(null);
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
            NucleusParameters.Composite.USER_OR_GAME_PROFILE
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final Either<UUID, GameProfile> target = NucleusParameters.Composite.parseUserOrGameProfile(context);
        final User user = target.fold(x -> Sponge.server().userManager().loadOrCreate(x).join(),
                x -> Sponge.server().userManager().loadOrCreate(x.uuid()).join());
        final IUserDataObject userDataObject = context.getServiceCollection()
                .storageManager()
                .getUserService()
                .getOrNewOnThread(user.uniqueId());

        final List<Component> messages = new ArrayList<>();

        // Everyone gets the last online time.
        final IPlayerOnlineService playerOnlineService = context.getServiceCollection().playerOnlineService();
        if (context.getAsPlayer().map(x -> playerOnlineService.isOnline(x, user)).orElse(true)) {
            messages.add(context.getMessage("command.seen.iscurrently.online", user.name()));
            userDataObject.get(CoreKeys.LAST_LOGIN).ifPresent(x -> messages.add(
                    context.getMessage("command.seen.loggedon", context.getTimeString(Duration.between(x, Instant.now())))));
        } else {
            messages.add(context.getMessage("command.seen.iscurrently.offline", user.name()));
            playerOnlineService.lastSeen(context.getAsPlayer().orElse(null), user).ifPresent(x -> messages.add(
                    context.getMessage("command.seen.loggedoff", context.getTimeString(Duration.between(Instant.now(), x)))));
        }

        messages.add(context.getMessage("command.seen.displayname", context.getDisplayName(user.uniqueId())));

        messages.add(Component.empty());
        for (final Map.Entry<String, Function3<ICommandContext, User, IUserDataObject, Component>> entry : this.entries.entrySet()) {
            if (context.testPermission(entry.getKey())) {
                @Nullable final Component m = entry.getValue().apply(context, user, userDataObject);
                if (m != null) {
                    messages.add(m);
                }
            }
        }

        // Add the extra module information.
        // TODO: Ordering
        final IPlayerInformationService playerInformationService = context.getServiceCollection().playerInformationService();
        for (final IPlayerInformationService.Provider provider : playerInformationService.getProviders()) {
            provider.get(user, context.cause(), context.getServiceCollection()).ifPresent(messages::add);
        }

        messages.addAll(context.getServiceCollection().getServiceUnchecked(SeenHandler.class)
                .getText(context.cause(), user));

        Util.getPaginationBuilder(context.audience())
                .contents(messages)
                .padding(Component.text("-", NamedTextColor.GREEN))
                .title(context.getMessage("command.seen.title", user.name())).sendTo(context.audience());
        return context.successResult();
    }

    private Component getLocationString(final String key, final ServerLocation lw, final ICommandContext source) {
        return this.getLocationString(key, lw.worldKey(), lw.position(), source);
    }

    private Component getLocationString(final String key, final ResourceKey worldKey, final Vector3d position, final ICommandContext context) {
        final Component text = context.getMessage(key, context.getMessage("command.seen.locationtemplate", worldKey.asString(),
                position.toInt().toString()));
        if (Sponge.server().commandManager().commandMapping("nucleus:tppos")
                .map(x -> x.registrar().canExecute(context.cause(), x))
                .orElse(false)) {

            final TextComponent.Builder building = Component.text().append(text)
                    .hoverEvent(HoverEvent.showText(context.getMessage("command.seen.teleportposition")
            ));

            Sponge.server().worldManager().world(worldKey).ifPresent(
                    x -> building.clickEvent(SpongeComponents.executeCallback(cs -> {
                        if (cs.root() instanceof ServerPlayer) {
                            ((ServerPlayer) cs.root()).setLocation(ServerLocation.of(x, position));
                        }
                    })
            ));

            return building.build();
        }

        return text;
    }

    private String getYesNo(@Nullable Boolean bool, final ICommandContext context) {
        if (bool == null) {
            bool = false;
        }

        return context.getMessageString("standard.yesno." + bool.toString().toLowerCase());
    }
}
