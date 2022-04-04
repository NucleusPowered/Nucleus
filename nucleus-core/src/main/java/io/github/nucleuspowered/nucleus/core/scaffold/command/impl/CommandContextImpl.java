/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.scaffold.command.impl;

import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.control.CommandControl;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.ICommandModifier;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Nameable;
import org.spongepowered.api.util.locale.LocaleSource;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

public final class CommandContextImpl implements ICommandContext {

    private final INucleusServiceCollection serviceCollection;
    private final String commandkey;
    private final String stringName;
    private double cost;
    private int cooldown;
    private int warmup;
    private final boolean isBypass;
    private final CommandCause cause;
    private final CommandContext context;
    private final Map<CommandModifier, ICommandModifier> modifiers;
    private final ArrayList<Consumer<ICommandContext>> failActions = new ArrayList<>();

    private static String getFriendlyName(final CommandCause cause) {
        final Object root = cause.root();
        if (root instanceof Nameable) {
            return ((Nameable) root).name();
        }
        return "Server";
    }

    public CommandContextImpl(
            final CommandCause cause,
            final CommandContext context,
            final INucleusServiceCollection serviceCollection,
            final CommandControl control,
            final boolean isBypass,
            final Map<CommandModifier, ICommandModifier> modifiers) {
        this.cause = cause;
        this.commandkey = control.getCommandKey();
        this.context = context;
        this.serviceCollection = serviceCollection;
        this.cost = control.getCost(cause);
        this.cooldown = control.getCooldown(cause);
        this.warmup = control.getWarmup(cause);
        this.modifiers = new HashMap<>(modifiers);
        this.stringName = CommandContextImpl.getFriendlyName(cause);
        this.isBypass = isBypass;
    }

    @Override
    public ServerPlayer requirePlayer() throws CommandException {
        final Object root = this.cause.root();
        if (root instanceof ServerPlayer) {
            return (ServerPlayer) root;
        }
        throw new CommandException(Component.text("This command must be executed by a player!", NamedTextColor.RED));
    }

    @Override
    public CommandCause cause() {
        return this.cause;
    }

    @Override
    public String getCommandKey() {
        return this.commandkey;
    }

    @Override
    public boolean hasFlag(final String name) {
        return this.context.hasFlag(name);
    }

    @Override
    public boolean hasAny(final Parameter.Key<?> name) {
        return this.context.hasAny(name);
    }

    @Override
    public Object getCommandSourceRoot() {
        return this.cause.root();
    }

    @Override
    public <T> Optional<T> getOne(final Parameter.Key<T> name) {
        return this.context.one(name);
    }

    @Override
    public <T> Optional<T> getOne(final Parameter.Value<T> name) {
        return this.context.one(name);
    }

    @Override
    public <T> Collection<? extends T> getAll(final Parameter.Key<T> name) {
        return this.context.all(name);
    }

    @Override
    public <T> Collection<? extends T> getAll(final Parameter.Value<T> name) {
        return this.context.all(name);
    }

    @Override
    public <T> T requireOne(final Parameter.Key<T> key) {
        return this.context.requireOne(key);
    }

    @Override
    public <T> T requireOne(final Parameter.Value<T> key) {
        return this.context.requireOne(key);
    }

    @Override
    public ServerPlayer getPlayerFromArgs(final Parameter.Key<? extends ServerPlayer> key, final String errorKey) throws CommandException {
        final Optional<? extends ServerPlayer> player = this.getOne(key);
        if (player.isPresent()) {
            return player.get();
        } else {
            return this.getIfPlayer(errorKey);
        }
    }

    @Override
    public ServerPlayer getCommandSourceAsPlayerUnchecked() {
        try {
            return this.requirePlayer();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<User> getOptionalUserFromUUID(final Parameter.Key<? extends UUID> key) {
        return this.context.one(key).flatMap(x -> Sponge.server().userManager().load(x).join());
    }

    @Override
    public Optional<User> getOptionalUserFromUUID(final Parameter.Value<? extends UUID> key) {
        return this.context.one(key).flatMap(x -> Sponge.server().userManager().load(x).join());
    }

    @Override
    public User getUserFromArgs(final Parameter.Value<? extends UUID> key, final String errorKey) throws CommandException {
        return this.getUserFromArgs(key.key(), errorKey);
    }

    @Override
    public User getUserFromArgs(final Parameter.Key<? extends UUID> key, final String errorKey) throws CommandException {
        final Optional<? extends UUID> user = this.context.one(key);
        if (user.isPresent()) {
            return Sponge.server().userManager().load(user.get()).join().get();
        } else {
            return this.getIfPlayer(errorKey).user();
        }
    }

    @Override
    public int getCooldown() {
        return this.cooldown;
    }

    @Override
    public void setCooldown(final int cooldown) {
        this.cooldown = Math.max(cooldown, 0);
    }

    @Override
    public double getCost() {
        return this.cost;
    }

    @Override
    public void setCost(final double cost) {
        this.cost = Math.max(cost, 0);
    }

    @Override
    public ICommandResult successResult() {
        return CommandResultImpl.SUCCESS;
    }

    @Override
    public ICommandResult failResult() {
        return CommandResultImpl.FAILURE;
    }

    @Override public ICommandResult getResultFromBoolean(final boolean success) {
        if (success) {
            return this.successResult();
        }
        return this.failResult();
    }

    public ICommandResult errorResultLiteral(final Component message) {
        return new CommandResultImpl.Literal(message);
    }

    @Override
    public ICommandResult errorResult(final String key, final Object... args) {
        return new CommandResultImpl(key, args);
    }

    @Override
    public CommandException createException(final Throwable th, final String key, final Object... args) {
        final Audience source = this.cause.audience();
        return new CommandException(
                this.serviceCollection.messageProvider().getMessageFor(source, key, args),
                th
        );
    }

    @Override
    public CommandException createException(final String key, final Object... args) {
        final Audience source = this.cause.audience();
        return new CommandException(
                this.serviceCollection.messageProvider().getMessageFor(source, key, args)
        );
    }

    @Override
    public INucleusServiceCollection getServiceCollection() {
        return this.serviceCollection;
    }

    @Override public Map<CommandModifier, ICommandModifier> modifiers() {
        return Collections.unmodifiableMap(this.modifiers);
    }

    @Override public void removeModifier(final String modifierId) {
        this.modifiers.entrySet().removeIf(x -> x.getKey().value().equals(modifierId));
    }

    @Override
    public void removeModifier(final ICommandModifier modifier) {
        this.modifiers.entrySet().removeIf(x -> x.getValue() == modifier);
    }

    @Override public Collection<Consumer<ICommandContext>> failActions() {
        return Collections.unmodifiableList(this.failActions);
    }

    @Override public void addFailAction(final Consumer<ICommandContext> action) {
        this.failActions.add(action);
    }

    @Override public Audience audience() {
        return this.cause.audience();
    }

    @Override
    public Locale getLocale() {
        if (this.cause.audience() instanceof ServerPlayer) {
            return ((ServerPlayer) this.cause.audience()).locale();
        }
        return null;
    }

    @Override public int getWarmup() {
        return this.warmup;
    }

    @Override public void setWarmup(final int warmup) {
        this.warmup = warmup;
    }

    @Override public boolean testPermission(final String permission) {
        return this.testPermissionFor(this.cause.subject(), permission);
    }

    @Override public boolean testPermissionFor(final UUID subject, final String permission) {
        return this.serviceCollection.permissionService().hasPermission(subject, permission);
    }

    @Override public boolean testPermissionFor(final Subject subject, final String permission) {
        return this.serviceCollection.permissionService().hasPermission(subject, permission);
    }

    @Override public String getMessageString(final String key, final Object... replacements) {
        return this.getMessageStringFor(this.cause.audience(), key, replacements);
    }

    @Override public String getMessageStringFor(final Audience to, final String key, final Object... replacements) {
        final Audience audience = this.cause.audience();
        final Locale locale;
        if (audience instanceof LocaleSource) {
            locale = ((LocaleSource) audience).locale();
        } else {
            locale = Sponge.server().locale();
        }
        return this.serviceCollection.messageProvider().getMessageString(locale, key, replacements);
    }

    @Override
    public Component getMessageFor(final Audience to, final String key, final Object... replacements) {
        return this.serviceCollection.messageProvider().getMessageFor(to, key, replacements);
    }

    @Override
    public Component getMessage(final String key, final Object... replacements) {
        return this.getMessageFor(this.cause.audience(), key, replacements);
    }

    @Override
    public String getTimeString(final long seconds) {
        return this.serviceCollection.messageProvider().getTimeString(this.getLocaleFromAudience(this.cause.audience()), seconds);
    }

    @Override
    public void sendMessage(final String key, final Object... replacements) {
        this.sendMessageTo(this.cause.audience(), key, replacements);
    }

    @Override
    public void sendMessageText(final Component message) {
        this.context.sendMessage(Identity.nil(), message);
    }

    @Override public void sendMessageTo(final Audience source, final String key, final Object... replacements) {
        this.serviceCollection.messageProvider().sendMessageTo(source, key, replacements);
    }

    @Override public boolean is(final Object other) {
        return this.getCommandSourceRoot().equals(other);
    }

    @Override public boolean is(final Class<?> other) {
        return other.isInstance(this.getCommandSourceRoot());
    }

    @Override
    public boolean isConsoleAndBypass() {
        return this.isBypass && this.getCommandSourceRoot() instanceof SystemSubject;
    }

    @Override
    public ServerWorld getWorldPropertiesOrFromSelf(final Parameter.Key<ServerWorld> worldKey) throws CommandException {
        return this.getWorldPropertiesOrFromSelfOptional(worldKey).orElseThrow(() -> new CommandException(this.getMessage("command.specifyworld")));
    }

    @Override
    public Optional<ServerWorld> getWorldPropertiesOrFromSelfOptional(final Parameter.Key<ServerWorld> worldKey) {
        final Optional<ServerWorld> optionalWorldProperties = this.context.one(worldKey);
        if (!optionalWorldProperties.isPresent()) {
            return this.cause.location().map(Location::world);
        }

        return optionalWorldProperties;
    }

    @Override
    public Component getDisplayName() {
        return this.getServiceCollection().playerDisplayNameService().getDisplayName(this.cause.audience());
    }

    @Override
    public Component getDisplayName(final UUID uuid) {
        return this.getServiceCollection().playerDisplayNameService().getDisplayName(uuid);
    }

    @Override
    public String getName() {
        return this.stringName;
    }

    @Override
    public OptionalInt getLevelFor(final Subject subject, final String key) {
        return this.serviceCollection.permissionService().getIntOptionFromSubject(subject, key);
    }

    @Override
    public boolean isPermissionLevelOkay(final Subject actee, final String key, final String permissionIfNoLevel, final boolean isSameLevel) {
        return this.serviceCollection.permissionService().isPermissionLevelOkay(this.cause.subject(), actee, key, permissionIfNoLevel, isSameLevel);
    }

    @Override
    public boolean isPermissionLevelOkay(final UUID actee, final String key, final String permissionIfNoLevel, final boolean isSameLevel) {
        return this.serviceCollection.permissionService().isPermissionLevelOkay(this.cause.subject(), actee, key, permissionIfNoLevel, isSameLevel).join();
    }

    @Override
    public Optional<UUID> uniqueId() {
        return Optional.empty();
    }

    @Override
    public Optional<ServerPlayer> getAsPlayer() {
        if (this.getCommandSourceRoot() instanceof ServerPlayer) {
            return Optional.of((ServerPlayer) this.getCommandSourceRoot());
        }
        return Optional.empty();
    }

    @Override
    public @NonNull ServerPlayer getIfPlayer(final String errorKey) throws CommandException {
        if (this.getCommandSourceRoot() instanceof ServerPlayer) {
            return (ServerPlayer) this.getCommandSourceRoot();
        }

        throw new CommandException(
                this.getServiceCollection().messageProvider().getMessageFor(this.cause.audience(), errorKey)
        );
    }

    @Override
    public boolean is(final User x) {
        return this.is(x.uniqueId());
    }

    @Override
    public boolean is(final UUID x) {
        final Object source = this.getCommandSourceRoot();
        if (source instanceof ServerPlayer) {
            return ((ServerPlayer) source).uniqueId().equals(x);
        }

        return false;
    }

    @Override
    public boolean isUser() {
        return this.getCommandSourceRoot() instanceof User;
    }

    private Locale getLocaleFromAudience(final Audience audience) {
        final Locale locale;
        if (audience instanceof LocaleSource) {
            locale = ((LocaleSource) audience).locale();
        } else {
            locale = Sponge.server().locale();
        }
        return locale;
    }

}
