/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.permission;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.nucleuspowered.nucleus.api.util.NoExceptionAutoClosable;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.NucleusRequirePermissionArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.util.PermissionMessageChannel;
import io.github.nucleuspowered.nucleus.util.PrettyPrinter;
import io.github.nucleuspowered.nucleus.util.ThrownFunction;
import org.slf4j.event.Level;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.source.CommandBlockSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.service.ProviderRegistration;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.util.Tristate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NucleusPermissionService implements IPermissionService, IReloadableService.Reloadable, ContextCalculator<Subject> {

    private final IMessageProviderService messageProviderService;
    private final INucleusServiceCollection serviceCollection;
    private boolean init = false;
    private boolean useRole = false;
    private boolean isOpOnly = true;
    private boolean consoleOverride = false;
    private final Set<ContextCalculator<Subject>> contextCalculators = new HashSet<>();
    private final Set<String> failedChecks = new HashSet<>();
    private final Map<String, IPermissionService.Metadata> metadataMap = new HashMap<>();
    private final Map<String, IPermissionService.Metadata> prefixMetadataMap = new HashMap<>();

    private final Map<UUID, Map<String, Context>> standardContexts = new ConcurrentHashMap<>();
    private final Map<SuggestedLevel, Set<SubjectReference>> appliedRoles = new HashMap<>();

    @Inject
    public NucleusPermissionService(
            INucleusServiceCollection serviceCollection,
            IReloadableService service) {
        this.messageProviderService = serviceCollection.messageProvider();
        this.serviceCollection = serviceCollection;

        // Register the context calculators.
        Sponge.getServiceManager().provide(PermissionService.class).ifPresent(x -> x.registerContextCalculator(this));
        service.registerReloadable(this);
    }

    @Override
    public void assignUserRoleToDefault() {
        assignRoleToGroup(SuggestedLevel.USER, Sponge.getServiceManager().provideUnchecked(PermissionService.class).getDefaults());
    }

    @Override
    public void assignRoleToGroup(SuggestedLevel role, Subject subject) {
        for (Map.Entry<String, IPermissionService.Metadata> permission : this.metadataMap.entrySet()) {
            if (permission.getValue().getSuggestedLevel() == role) {
                subject.getTransientSubjectData().setPermission(ImmutableSet.of(), permission.getValue().getPermission(), Tristate.TRUE);
            }
        }
        for (Map.Entry<String, IPermissionService.Metadata> permission : this.prefixMetadataMap.entrySet()) {
            if (permission.getValue().getSuggestedLevel() == role) {
                subject.getTransientSubjectData().setPermission(ImmutableSet.of(), permission.getValue().getPermission(), Tristate.TRUE);
            }
        }
    }

    @Override public boolean isOpOnly() {
        return this.isOpOnly;
    }

    @Override public void registerContextCalculator(ContextCalculator<Subject> calculator) {
        this.contextCalculators.add(calculator);
        Sponge.getServiceManager().provide(PermissionService.class).ifPresent(x -> x.registerContextCalculator(calculator));
    }

    @Override public void checkServiceChange(ProviderRegistration<PermissionService> service) {
        this.contextCalculators.forEach(x -> service.getProvider().registerContextCalculator(x));
        service.getProvider().registerContextCalculator(this);

        // don't know if there is a better way to do this.
        this.isOpOnly = service.getPlugin().getId().equals("sponge");
    }

    @Override public boolean hasPermission(Subject permissionSubject, String permission) {
        return hasPermission(permissionSubject, permission, this.useRole);
    }

    @Override public Tristate hasPermissionTristate(Subject subject, String permission) {
        return hasPermissionTristate(subject, permission, this.useRole);
    }

    @Override public boolean hasPermissionWithConsoleOverride(Subject subject, String permission, boolean permissionIfConsoleAndOverridden) {
        if (this.consoleOverride && subject instanceof ConsoleSource) {
            return permissionIfConsoleAndOverridden;
        }

        return hasPermission(subject, permission);
    }

    @Override public boolean isConsoleOverride(Subject subject) {
        return this.consoleOverride && subject instanceof ConsoleSource;
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        CoreConfig coreConfig = serviceCollection.moduleDataProvider().getModuleConfig(CoreConfig.class);
        this.useRole = coreConfig.isUseParentPerms();
        this.consoleOverride = coreConfig.isConsoleOverride();
    }

    @Override public void registerDescriptions() {
        Preconditions.checkState(!this.init);
        this.init = true;
        PermissionService ps = Sponge.getServiceManager().provide(PermissionService.class).orElse(null);
        boolean isPresent = ps != null;

        for (Map.Entry<String, IPermissionService.Metadata> entry : this.metadataMap.entrySet()) {
            SuggestedLevel level = entry.getValue().getSuggestedLevel();
            if (isPresent && level.getRole() != null) {
                ps.newDescriptionBuilder(this.serviceCollection.pluginContainer())
                        .assign(level.getRole(), true)
                        .description(Text.of(entry.getValue().getDescription(this.messageProviderService)))
                        .id(entry.getKey()).register();
            }
        }
    }

    @Override public void register(String permission, PermissionMetadata metadata, String moduleid) {
        NucleusPermissionService.Metadata m = new NucleusPermissionService.Metadata(permission, metadata, moduleid);
        if (metadata.isPrefix()) {
            this.prefixMetadataMap.put(permission.toLowerCase(), m);
        } else {
            this.metadataMap.put(permission.toLowerCase(), m);
        }
    }

    @Override public CommandElement createOtherUserPermissionElement(String permission) {
        return GenericArguments.optionalWeak(
                new NucleusRequirePermissionArgument(NucleusParameters.ONE_USER.get(this.serviceCollection),
                this,
                permission,
                false));
    }

    @Override public OptionalDouble getDoubleOptionFromSubject(Subject player, String... options) {
        return getTypedObjectFromSubject(
                string -> OptionalDouble.of(Double.parseDouble(string)),
                OptionalDouble.empty(),
                player,
                options);
    }

    @Override public OptionalLong getPositiveLongOptionFromSubject(Subject player, String... options) {
        return getTypedObjectFromSubject(
                string -> OptionalLong.of(Long.parseLong(string)),
                OptionalLong.empty(),
                player,
                options);
    }

    @Override public OptionalInt getPositiveIntOptionFromSubject(Subject player, String... options) {
        return getTypedObjectFromSubject(
                string -> OptionalInt.of(Integer.parseUnsignedInt(string)),
                OptionalInt.empty(),
                player,
                options);
    }

    @Override public OptionalInt getIntOptionFromSubject(Subject player, String... options) {
        return getTypedObjectFromSubject(
                string -> OptionalInt.of(Integer.parseInt(string)),
                OptionalInt.empty(),
                player,
                options);
    }

    private <T> T getTypedObjectFromSubject(ThrownFunction<String, T, Exception> conversion, T empty, Subject player, String... options) {
        try {
            Optional<String> optional = getOptionFromSubject(player, options);
            if (optional.isPresent()) {
                return conversion.apply(optional.get());
            }
        } catch (Exception e) {
            // ignored
        }

        return empty;
    }

    @Override public Optional<String> getOptionFromSubject(Subject player, String... options) {
        for (String option : options) {
            String o = option.toLowerCase();

            // Option for context.
            Optional<String> os = player.getOption(player.getActiveContexts(), o);
            if (os.isPresent()) {
                return os.map(r -> r.isEmpty() ? null : r);
            }

            // General option
            os = player.getOption(o);
            if (os.isPresent()) {
                return os.map(r -> r.isEmpty() ? null : r);
            }
        }

        return Optional.empty();
    }

    @Override public PermissionMessageChannel permissionMessageChannel(String permission) {
        return new PermissionMessageChannel(this, permission);
    }

    @Override public List<IPermissionService.Metadata> getAllMetadata() {
        return ImmutableList.copyOf(this.metadataMap.values());
    }

    private boolean hasPermission(Subject subject, String permission, boolean checkRole) {
        Tristate tristate = hasPermissionTristate(subject, permission, checkRole);
        if (tristate == Tristate.UNDEFINED) {
            return subject.hasPermission(permission); // guarantees the correct response.
        }

        return tristate.asBoolean();
    }

    private Tristate hasPermissionTristate(Subject subject, String permission, boolean checkRole) {
        if (checkRole && permission.startsWith("nucleus.")) {
            Tristate tristate = subject.getPermissionValue(subject.getActiveContexts(), permission);
            if (tristate == Tristate.UNDEFINED) {
                @Nullable IPermissionService.Metadata result = this.metadataMap.get(permission);
                if (result != null) { // check the "parent" perm
                    String perm = result.getSuggestedLevel().getPermission();
                    if (perm == null) {
                        return subject.getPermissionValue(subject.getActiveContexts(), permission);
                    } else {
                        return subject.getPermissionValue(subject.getActiveContexts(), perm);
                    }
                }

                for (Map.Entry<String, IPermissionService.Metadata> entry : this.prefixMetadataMap.entrySet()) {
                    if (permission.startsWith(entry.getKey())) {
                        String perm = entry.getValue().getSuggestedLevel().getPermission();
                        if (perm == null) {
                            return subject.getPermissionValue(subject.getActiveContexts(), permission);
                        } else {
                            return subject.getPermissionValue(subject.getActiveContexts(), perm);
                        }
                    }
                }

                // if we get here, no registered permissions were found
                // therefore, warn
                if (this.failedChecks.add(permission)) {
                    PrettyPrinter printer = new PrettyPrinter(80);
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
    public Optional<IPermissionService.Metadata> getMetadataFor(String permission) {
        this.metadataMap.get(permission);
        return Optional.empty();
    }

    @Override
    public boolean isPermissionLevelOkay(Subject actor, Subject actee, String key, String permission, boolean isSameOkay) {
        int actorLevel = getDeclaredLevel(actor, key).orElseGet(() -> hasPermission(actor, permission) ? getDefaultLevel(actor) : 0);
        int acteeLevel = getDeclaredLevel(actee, key).orElseGet(() -> hasPermission(actee, permission) ? getDefaultLevel(actee) : 0);
        if (isSameOkay) {
            return actorLevel >= acteeLevel;
        } else {
            return actorLevel > acteeLevel;
        }
    }

    @Override
    public void setContext(Subject subject, Context context) {
        if (subject instanceof Identifiable) {
            setContext(((Identifiable) subject).getUniqueId(), context);
        }
    }

    private void setContext(UUID uuid, Context context) {
        this.standardContexts.computeIfAbsent(uuid, k -> new HashMap<>()).put(context.getKey().toLowerCase(), context);
    }

    @Override
    public NoExceptionAutoClosable setContextTemporarily(Subject subject, Context context) {
        if (subject instanceof Identifiable) {
            UUID uuid = ((Identifiable) subject).getUniqueId();
            Context old = this.standardContexts.computeIfAbsent(uuid, k -> new HashMap<>()).put(context.getKey().toLowerCase(), context);
            return () -> {
                removeContext(uuid, context.getKey().toLowerCase());
                if (old != null) {
                    setContext(uuid, context);
                }
            };
        }
        return NoExceptionAutoClosable.EMPTY;
    }

    @Override
    public void removeContext(UUID subject, String key) {
        Map<String, Context> contexts = this.standardContexts.get(subject);
        if (contexts != null && !contexts.isEmpty()) {
            contexts.remove(key.toLowerCase());
        }
    }

    @Override
    public void removePlayerContexts(UUID uuid) {
        this.standardContexts.remove(uuid);
    }

    @Override
    public void accumulateContexts(Subject target, Set<Context> accumulator) {
        if (target instanceof Identifiable) {
            Map<String, Context> ctxs = this.standardContexts.get(((Identifiable) target).getUniqueId());
            if (ctxs != null && !ctxs.isEmpty()) {
                accumulator.addAll(ctxs.values());
            }
        }
    }

    @Override
    public boolean matches(final Context context, final Subject target) {
        if (target instanceof Identifiable) {
            Map<String, Context> ctxs = this.standardContexts.get(((Identifiable) target).getUniqueId());
            if (ctxs != null && !ctxs.isEmpty()) {
                Context ctx = ctxs.get(context.getKey());
                return ctx.equals(context);
            }
        }
        return false;
    }

    private int getDefaultLevel(Subject subject) {
        if (subject instanceof ConsoleSource || subject instanceof CommandBlockSource) {
            return Integer.MAX_VALUE;
        }

        return 1;
    }

    public static class Metadata implements IPermissionService.Metadata {

        private final String description;
        private final String permission;
        private final SuggestedLevel suggestedLevel;
        private final boolean isPrefix;
        private final String[] replacements;
        private final String moduleid;

        Metadata(String permission, PermissionMetadata metadata, String moduleid) {
            this(
                    metadata.descriptionKey(),
                    metadata.replacements(),
                    permission,
                    metadata.level(),
                    metadata.isPrefix(),
                    moduleid
            );
        }

        Metadata(String description,
                String[] replacements,
                String permission,
                SuggestedLevel suggestedLevel,
                boolean isPrefix,
                String moduleid) {
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

        @Override public String getDescription(IMessageProviderService service) {
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
