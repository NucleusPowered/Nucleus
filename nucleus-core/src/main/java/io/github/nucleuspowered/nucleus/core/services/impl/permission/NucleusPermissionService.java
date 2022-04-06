/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.permission;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.api.util.NoExceptionAutoClosable;
import io.github.nucleuspowered.nucleus.core.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.core.services.interfaces.data.SuggestedLevel;
import io.github.nucleuspowered.nucleus.core.util.PermissionMessageChannel;
import io.github.nucleuspowered.nucleus.core.util.PrettyPrinter;
import io.github.nucleuspowered.nucleus.core.util.functional.ThrownFunction;
import io.vavr.CheckedFunction1;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.block.entity.CommandBlock;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.util.Tristate;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Singleton
public class NucleusPermissionService implements IPermissionService, IReloadableService.Reloadable, ContextCalculator {

    private final IMessageProviderService messageProviderService;
    private final INucleusServiceCollection serviceCollection;
    private boolean init = false;
    private boolean useRole = false;
    private boolean consoleOverride = false;
    private final Set<String> failedChecks = new HashSet<>();
    private final Map<String, IPermissionService.Metadata> metadataMap = new HashMap<>();
    private final Map<String, IPermissionService.Metadata> prefixMetadataMap = new HashMap<>();

    private final Map<UUID, Map<String, Context>> standardContexts = new ConcurrentHashMap<>();

    @Inject
    public NucleusPermissionService(
            final INucleusServiceCollection serviceCollection,
            final IReloadableService service) {
        this.messageProviderService = serviceCollection.messageProvider();
        this.serviceCollection = serviceCollection;
        service.registerReloadable(this);
    }

    @Override
    public void assignUserRoleToDefault() {
        this.assignRoleToGroup(SuggestedLevel.USER, Sponge.server().serviceProvider().permissionService().defaults());
    }

    @Override
    public void assignRoleToGroup(final SuggestedLevel role, final Subject subject) {
        for (final Map.Entry<String, IPermissionService.Metadata> permission : this.metadataMap.entrySet()) {
            if (permission.getValue().getSuggestedLevel() == role) {
                subject.transientSubjectData().setPermission(Collections.emptySet(), permission.getValue().getPermission(), Tristate.TRUE);
            }
        }
        for (final Map.Entry<String, IPermissionService.Metadata> permission : this.prefixMetadataMap.entrySet()) {
            if (permission.getValue().getSuggestedLevel() == role) {
                subject.transientSubjectData().setPermission(Collections.emptySet(), permission.getValue().getPermission(), Tristate.TRUE);
            }
        }
    }

    @Override
    public void registerContextCalculator(final ContextCalculator calculator) {
        Sponge.server().serviceProvider().contextService().registerContextCalculator(calculator);
    }

    @Override
    public boolean hasPermission(final UUID playerUUID, final String permission) {
        return this.hasPermission(Sponge.server().userManager().load(playerUUID).join()
                .orElseThrow(() -> new IllegalArgumentException("The UUID " + playerUUID + " is not a valid player UUID")), permission);
    }

    @Override
    public boolean hasPermission(final Subject permissionSubject, final String permission) {
        return this.hasPermission(permissionSubject, permission, this.useRole);
    }

    @Override
    public Tristate hasPermissionTristate(final Subject subject, final String permission) {
        return this.hasPermissionTristate(subject, permission, this.useRole);
    }

    @Override
    public boolean hasPermissionWithConsoleOverride(final Subject subject, final String permission, final boolean permissionIfConsoleAndOverridden) {
        if (this.consoleOverride && subject instanceof SystemSubject) {
            return permissionIfConsoleAndOverridden;
        }

        return this.hasPermission(subject, permission);
    }

    @Override public boolean isConsoleOverride(final Subject subject) {
        return this.consoleOverride && subject instanceof SystemSubject;
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        final CoreConfig coreConfig = serviceCollection.configProvider().getModuleConfig(CoreConfig.class);
        this.useRole = coreConfig.isUseParentPerms();
        this.consoleOverride = coreConfig.isConsoleOverride();
    }

    @Override public void registerDescriptions() {
        if (this.init) {
            throw new IllegalStateException("Init has already started");
        }
        this.init = true;
        final PermissionService ps = Sponge.server().serviceProvider().permissionService();
        for (final Map.Entry<String, IPermissionService.Metadata> entry : this.metadataMap.entrySet()) {
            final SuggestedLevel level = entry.getValue().getSuggestedLevel();
            if (level.getRole() != null) {
                ps.newDescriptionBuilder(this.serviceCollection.pluginContainer())
                        .assign(level.getRole(), true)
                        .description(Component.text(entry.getValue().getDescription(this.messageProviderService)))
                        .id(entry.getKey()).register();
            }
        }
    }

    @Override public void register(final String permission, final PermissionMetadata metadata, final String moduleid) {
        final NucleusPermissionService.Metadata m = new NucleusPermissionService.Metadata(permission, metadata, moduleid);
        if (metadata.isPrefix()) {
            this.prefixMetadataMap.put(permission.toLowerCase(), m);
        } else {
            this.metadataMap.put(permission.toLowerCase(), m);
        }
    }

    @Override public OptionalDouble getDoubleOptionFromSubject(final Subject player, final String... options) {
        return this.getTypedObjectFromSubject(
                string -> OptionalDouble.of(Double.parseDouble(string)),
                OptionalDouble.empty(),
                player,
                options);
    }

    @Override public OptionalLong getPositiveLongOptionFromSubject(final Subject player, final String... options) {
        return this.getTypedObjectFromSubject(
                string -> OptionalLong.of(Long.parseLong(string)),
                OptionalLong.empty(),
                player,
                options);
    }

    @Override public OptionalInt getPositiveIntOptionFromSubject(final Subject player, final String... options) {
        return this.getTypedObjectFromSubject(
                string -> OptionalInt.of(Integer.parseUnsignedInt(string)),
                OptionalInt.empty(),
                player,
                options);
    }

    @Override public OptionalInt getIntOptionFromSubject(final Subject player, final String... options) {
        return this.getTypedObjectFromSubject(
                string -> OptionalInt.of(Integer.parseInt(string)),
                OptionalInt.empty(),
                player,
                options);
    }

    private <T> T getTypedObjectFromSubject(final CheckedFunction1<String, T> conversion, final T empty, final Subject player, final String... options) {
        try {
            final Optional<String> optional = this.getOptionFromSubject(player, options);
            if (optional.isPresent()) {
                return conversion.apply(optional.get());
            }
        } catch (final Throwable e) {
            // ignored
        }

        return empty;
    }

    @Override public Optional<String> getOptionFromSubject(final Subject player, final String... options) {
        for (final String option : options) {
            final String o = option.toLowerCase();

            // Option for context.
            Optional<String> os = player.option(o, player.contextCause());
            if (os.isPresent()) {
                return os.map(r -> r.isEmpty() ? null : r);
            }

            // General option
            os = player.option(o);
            if (os.isPresent()) {
                return os.map(r -> r.isEmpty() ? null : r);
            }
        }

        return Optional.empty();
    }

    @Override public PermissionMessageChannel permissionMessageChannel(final String permission) {
        return new PermissionMessageChannel(this, permission);
    }

    @Override public Collection<IPermissionService.Metadata> getAllMetadata() {
        return Collections.unmodifiableCollection(this.metadataMap.values());
    }

    private boolean hasPermission(final Subject subject, final String permission, final boolean checkRole) {
        final Tristate tristate = this.hasPermissionTristate(subject, permission, checkRole);
        if (tristate == Tristate.UNDEFINED) {
            return subject.hasPermission(permission); // guarantees the correct response.
        }

        return tristate.asBoolean();
    }

    private Tristate hasPermissionTristate(final Subject subject, final String permission, final boolean checkRole) {
        if (checkRole && permission.startsWith("nucleus.")) {
            final Tristate tristate = subject.permissionValue(permission, subject.contextCause());
            if (tristate == Tristate.UNDEFINED) {
                final IPermissionService.@Nullable Metadata result = this.metadataMap.get(permission);
                if (result != null) { // check the "parent" perm
                    final String perm = result.getSuggestedLevel().getPermission();
                    if (perm == null) {
                        return subject.permissionValue(permission, subject.contextCause());
                    } else {
                        return subject.permissionValue(perm, subject.contextCause());
                    }
                }

                for (final Map.Entry<String, IPermissionService.Metadata> entry : this.prefixMetadataMap.entrySet()) {
                    if (permission.startsWith(entry.getKey())) {
                        final String perm = entry.getValue().getSuggestedLevel().getPermission();
                        if (perm == null) {
                            return subject.permissionValue(permission, subject.contextCause());
                        } else {
                            return subject.permissionValue(perm, subject.contextCause());
                        }
                    }
                }

                // if we get here, no registered permissions were found
                // therefore, warn
                if (this.failedChecks.add(permission)) {
                    final PrettyPrinter printer = new PrettyPrinter(80);
                    printer.add("Nucleus Permission Not Registered").centre().hr();
                    printer.add("Nucleus has not registered a permission properly. This is an error in Nucleus - please report to the Nucleus "
                            + "github.");
                    printer.hr();
                    printer.add("Permission: %s", permission);
                    printer.log(this.serviceCollection.logger(), Level.WARN);
                }

                // guarantees that the subject default is selected.
                return Tristate.UNDEFINED; // subject.hasPermission(permission);
            }

            return tristate;
        }

        return Tristate.UNDEFINED;
    }

    @Override
    public Optional<IPermissionService.Metadata> getMetadataFor(final String permission) {
        return Optional.ofNullable(this.metadataMap.get(permission));
    }

    @Override
    public boolean isPermissionLevelOkay(final Subject actor, final Subject actee, final String key, final String permission, final boolean isSameOkay) {
        final int actorLevel =
                this.getDeclaredLevel(actor, key).orElseGet(() -> this.hasPermission(actor, permission) ? this.getDefaultLevel(actor) : 0);
        final int acteeLevel =
                this.getDeclaredLevel(actee, key).orElseGet(() -> this.hasPermission(actee, permission) ? this.getDefaultLevel(actee) : 0);
        if (isSameOkay) {
            return actorLevel >= acteeLevel;
        } else {
            return actorLevel > acteeLevel;
        }
    }

    @Override
    public CompletableFuture<Boolean> isPermissionLevelOkay(final Subject actor, final UUID actee, final String key, final String permission,
            final boolean isSameOkay) {
        return Sponge.server().serviceProvider().permissionService().userSubjects().loadSubject(actee.toString())
                .thenApply(subject -> this.isPermissionLevelOkay(actor, subject, key, permission, isSameOkay));
    }

    @Override
    public void setContext(final Subject subject, final Context context) {
        if (subject instanceof Identifiable) {
            this.setContext(((Identifiable) subject).uniqueId(), context);
        }
    }

    private void setContext(final UUID uuid, final Context context) {
        this.standardContexts.computeIfAbsent(uuid, k -> new HashMap<>()).put(context.getKey().toLowerCase(), context);
    }

    @Override
    public NoExceptionAutoClosable setContextTemporarily(final Subject subject, final Context context) {
        if (subject instanceof Identifiable) {
            final UUID uuid = ((Identifiable) subject).uniqueId();
            final Context old = this.standardContexts.computeIfAbsent(uuid, k -> new HashMap<>()).put(context.getKey().toLowerCase(), context);
            return () -> {
                this.removeContext(uuid, context.getKey().toLowerCase());
                if (old != null) {
                    this.setContext(uuid, context);
                }
            };
        }
        return NoExceptionAutoClosable.EMPTY;
    }

    @Override
    public void removeContext(final UUID subject, final String key) {
        final Map<String, Context> contexts = this.standardContexts.get(subject);
        if (contexts != null && !contexts.isEmpty()) {
            contexts.remove(key.toLowerCase());
        }
    }

    @Override
    public void removePlayerContexts(final UUID uuid) {
        this.standardContexts.remove(uuid);
    }

    @Override
    public void register(final String id, final Class<?> permissions) {
        for (final Field field : permissions.getDeclaredFields()) {
            final PermissionMetadata metadata = field.getAnnotation(PermissionMetadata.class);
            if (metadata != null && field.getType().equals(String.class)) {
                try {
                    field.setAccessible(true);
                    this.register((String) field.get(null), metadata, id);
                } catch (final IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int getDefaultLevel(final Subject subject) {
        if (subject instanceof SystemSubject || subject instanceof Server || subject instanceof CommandBlock) {
            return Integer.MAX_VALUE;
        }

        return 1;
    }

    @Override
    public void accumulateContexts(final Cause source, final Consumer<Context> accumulator) {
        if (source.root() instanceof Identifiable) {
            final Map<String, Context> ctxs = this.standardContexts.get(((Identifiable) source.root()).uniqueId());
            if (ctxs != null && !ctxs.isEmpty()) {
                ctxs.values().forEach(accumulator);
            }
        }
    }

    public static class Metadata implements IPermissionService.Metadata {

        private final String description;
        private final String permission;
        private final SuggestedLevel suggestedLevel;
        private final boolean isPrefix;
        private final String[] replacements;
        private final String moduleid;

        Metadata(final String permission, final PermissionMetadata metadata, final String moduleid) {
            this(
                    metadata.descriptionKey(),
                    metadata.replacements(),
                    permission,
                    metadata.level(),
                    metadata.isPrefix(),
                    moduleid
            );
        }

        Metadata(final String description,
                final String[] replacements,
                final String permission,
                final SuggestedLevel suggestedLevel,
                final boolean isPrefix,
                final String moduleid) {
            this.description = description;
            this.replacements = replacements;
            this.permission = permission.toLowerCase();
            this.suggestedLevel = suggestedLevel;
            this.isPrefix = isPrefix;
            this.moduleid = moduleid;
        }

        @Override public boolean isPrefix() {
            return this.isPrefix;
        }

        @Override public SuggestedLevel getSuggestedLevel() {
            return this.suggestedLevel;
        }

        @Override public String getDescription(final IMessageProviderService service) {
            return service.getMessageString(this.description, (Object[]) this.replacements);
        }

        @Override public String getPermission() {
            return this.permission;
        }

        @Override public String getModuleId() {
            return this.moduleid;
        }

    }

}
