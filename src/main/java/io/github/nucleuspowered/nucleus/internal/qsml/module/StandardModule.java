/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.qsml.module;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.api.data.seen.BasicSeenInformationProvider;
import io.github.nucleuspowered.nucleus.config.CommandsConfig;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.InternalServiceManager;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.MixinConfigProxy;
import io.github.nucleuspowered.nucleus.internal.TaskBase;
import io.github.nucleuspowered.nucleus.internal.annotations.ConditionalListener;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RequireMixinPlugin;
import io.github.nucleuspowered.nucleus.internal.annotations.SkipOnError;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.CommandBuilder;
import io.github.nucleuspowered.nucleus.internal.docgen.DocGenCache;
import io.github.nucleuspowered.nucleus.modules.playerinfo.handlers.SeenHandler;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import uk.co.drnaylor.quickstart.Module;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.config.AbstractConfigAdapter;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

public abstract class StandardModule implements Module {

    private final String moduleId;
    private final String moduleName;
    private String packageName;
    @Inject protected NucleusPlugin plugin;
    @Inject protected InternalServiceManager serviceManager;

    @Inject
    private CommandsConfig commandsConfig;

    public StandardModule() {
        ModuleData md = this.getClass().getAnnotation(ModuleData.class);
        this.moduleId = md.id();
        this.moduleName = md.name();
    }

    /**
     * Non-configurable module, no configuration to register.
     *
     * @return {@link Optional#empty()}
     */
    @Override
    public Optional<AbstractConfigAdapter<?>> getConfigAdapter() {
        return Optional.empty();
    }

    @Override
    public final void preEnable() {
        try {
            performPreTasks();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot enable module!", e);
        }
    }

    @Override
    public void onEnable() {
        packageName = this.getClass().getPackage().getName() + ".";

        // Construct commands
        loadCommands();
        loadEvents();
        loadRunnables();
    }

    @SuppressWarnings("unchecked")
    private void loadCommands() {
        Set<Class<? extends AbstractCommand<?>>> cmds = getStreamForModule(AbstractCommand.class)
                .filter(x -> x.isAnnotationPresent(RegisterCommand.class))
                .filter(checkMixin("command", t -> t.getName() + ": (" + t.getAnnotation(RegisterCommand.class).value()[0] + ")"))
                .map(x -> (Class<? extends AbstractCommand<?>>)x) // Keeping the compiler happy...
                .collect(Collectors.toSet());

        // We all love the special injector. We just want to provide the module with more commands, in case it needs a child.
        Injector injector = plugin.getInjector();

        Set<Class<? extends AbstractCommand>> commandBases =  cmds.stream().filter(x -> {
            RegisterCommand rc = x.getAnnotation(RegisterCommand.class);
            return (rc != null && rc.subcommandOf().equals(AbstractCommand.class));
        }).collect(Collectors.toSet());

        CommandBuilder builder = new CommandBuilder(plugin, injector, cmds, moduleId, moduleName);
        commandBases.forEach(builder::buildCommand);

        try {
            commandsConfig.mergeDefaults(builder.getNodeToMerge());
            commandsConfig.save();
        } catch (IOException | ObjectMappingException e) {
            plugin.getLogger().error("Could not save defaults.");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadEvents() {
        Set<Class<? extends ListenerBase>> listenersToLoad = getStreamForModule(ListenerBase.class)
            .filter(checkMixin("listener"))
            .collect(Collectors.toSet());

        ModuleData md = this.getClass().getAnnotation(ModuleData.class);
        Optional<DocGenCache> docGenCache = plugin.getDocGenCache();
        Injector injector = plugin.getInjector();
        listenersToLoad.stream().map(x -> this.getInstance(injector, x)).filter(Objects::nonNull).forEach(c -> {
            // Register suggested permissions
            c.getPermissions().forEach((k, v) -> plugin.getPermissionRegistry().registerOtherPermission(k, v));
            docGenCache.ifPresent(x -> x.addPermissionDocs(moduleId, c.getPermissions()));

            if (c instanceof ListenerBase.Reloadable) {
                plugin.registerReloadable((ListenerBase.Reloadable)c);
            }

            final ConditionalListener conditionalListener = c.getClass().getAnnotation(ConditionalListener.class);
            if (conditionalListener != null) {
                // Add reloadable to load in the listener dynamically if required.
                plugin.registerReloadable(() -> {
                    Sponge.getEventManager().unregisterListeners(c);
                    if (conditionalListener.value().newInstance().test(plugin)) {
                        Sponge.getEventManager().registerListeners(plugin, c);
                    }
                });

                // If the condition is NOT met, return out of here early.
                try {
                    if (!conditionalListener.value().newInstance().test(plugin)) {
                        return;
                    }
                } catch (InstantiationException | IllegalAccessException e) {
                    if (plugin.isDebugMode()) {
                        e.printStackTrace();
                    }

                    return;
                }
            }

            Sponge.getEventManager().registerListeners(plugin, c);
        });
    }

    @SuppressWarnings("unchecked")
    private void loadRunnables() {
        Set<Class<? extends TaskBase>> commandsToLoad = getStreamForModule(TaskBase.class)
                .filter(checkMixin("runnable"))
                .collect(Collectors.toSet());

        Optional<DocGenCache> docGenCache = plugin.getDocGenCache();
        Injector injector = plugin.getInjector();
        commandsToLoad.stream().map(x -> this.getInstance(injector, x)).filter(lb -> lb != null).forEach(c -> {
            c.getPermissions().forEach((k, v) -> plugin.getPermissionRegistry().registerOtherPermission(k, v));
            docGenCache.ifPresent(x -> x.addPermissionDocs(moduleId, c.getPermissions()));
            TaskBase.TimePerRun tpr = c.interval();
            Task.Builder tb = Sponge.getScheduler().createTaskBuilder().execute(c).interval(tpr.getTime(), tpr.getUnit());
            if (c.isAsync()) {
                tb.async();
            }

            tb.submit(plugin);
        });
    }

    @SuppressWarnings("unchecked")
    private <T> Stream<Class<? extends T>> getStreamForModule(Class<T> assignableClass) {
        return plugin.getModuleContainer().getLoadedClasses().stream().filter(assignableClass::isAssignableFrom)
                .filter(x -> x.getPackage().getName().startsWith(packageName))
                .map(x -> (Class<? extends T>)x);
    }

    protected void performPreTasks() throws Exception { }

    private <T> T getInstance(Injector injector, Class<T> clazz) {
        try {
            return injector.getInstance(clazz);

        // I can't believe I have to do this...
        } catch (RuntimeException | NoClassDefFoundError e) {
            if (clazz.isAnnotationPresent(SkipOnError.class)) {
                plugin.getLogger().warn(NucleusPlugin.getNucleus().getMessageProvider().getMessageWithFormat("startup.injectablenotloaded", clazz.getName()));
                return null;
            }

            throw e;
        }
    }

    private <T extends Class<?>> Predicate<T> checkMixin(String x) {
        return checkMixin(x, t -> t.getName());
    }

    private <T extends Class<?>> Predicate<T> checkMixin(String x, Function<T, String> nameSupplier) {
        return t -> {
            RequireMixinPlugin requireMixinPlugin = t.getAnnotation(RequireMixinPlugin.class);
            if (requireMixinPlugin == null) {
                return true;
            }

            Optional<MixinConfigProxy> mixinConfigProxyOptional = plugin.getMixinConfigIfAvailable();
            if (!mixinConfigProxyOptional.isPresent() && requireMixinPlugin.value() == RequireMixinPlugin.MixinLoad.MIXIN_ONLY) {
                if (requireMixinPlugin.notifyOnLoad()) {
                    plugin.getLogger().warn(plugin.getMessageProvider().getMessageWithFormat("loader.mixinrequired." + x, nameSupplier.apply(t)));
                }

                return false;
            } else if (mixinConfigProxyOptional.isPresent() && requireMixinPlugin.value() == RequireMixinPlugin.MixinLoad.MIXIN_ONLY) {
                try {
                    if (requireMixinPlugin.loadWhen().newInstance().test(mixinConfigProxyOptional.get())) {
                        return true;
                    }

                    if (requireMixinPlugin.notifyOnLoad()) {
                        plugin.getLogger().warn(plugin.getMessageProvider().getMessageWithFormat("loader.mixinrequired." + x, nameSupplier.apply(t)));
                    }

                    return false;
                } catch (Exception e) {
                    if (plugin.isDebugMode()) {
                        e.printStackTrace();
                    }

                    return false;
                }
            } else if (mixinConfigProxyOptional.isPresent() && requireMixinPlugin.value() == RequireMixinPlugin.MixinLoad.NO_MIXIN) {
                if (requireMixinPlugin.notifyOnLoad()) {
                    plugin.getLogger().warn(plugin.getMessageProvider().getMessageWithFormat("loader.nomixinrequired." + x, nameSupplier.apply(t)));
                }

                return false;
            }

            return true;
        };
    }

    protected final void createSeenModule(BiFunction<CommandSource, User, Collection<Text>> function) {
        createSeenModule((String)null, function);
    }

    protected final void createSeenModule(@Nullable Class<? extends AbstractCommand> permissionClass, BiFunction<CommandSource, User, Collection<Text>> function) {
        // Register seen information.
        CommandPermissionHandler permissionHandler = plugin.getPermissionRegistry().getService(permissionClass);
        createSeenModule(permissionHandler == null ? null : permissionHandler.getBase(), function);
    }

    protected final void createSeenModule(@Nullable Class<? extends AbstractCommand> permissionClass, String suffix, BiFunction<CommandSource, User, Collection<Text>> function) {
        // Register seen information.
        CommandPermissionHandler permissionHandler = plugin.getPermissionRegistry().getService(permissionClass);
        createSeenModule(permissionHandler == null ? null : permissionHandler.getPermissionWithSuffix(suffix), function);
    }

    protected final void createSeenModule(@Nullable String permission, BiFunction<CommandSource, User, Collection<Text>> function) {
        plugin.getInternalServiceManager().getService(SeenHandler.class).ifPresent(x -> x.register(plugin, this.getClass().getAnnotation(ModuleData.class).name(),
            new BasicSeenInformationProvider(permission == null ? null : permission, function)));
    }
}
