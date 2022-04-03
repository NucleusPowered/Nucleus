/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.test.sanity;

import com.google.common.reflect.ClassPath;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.scaffold.task.TaskBase;
import org.junit.Assert;
import org.junit.Test;
import org.spongepowered.api.util.Tuple;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class SanityTests {

    @Test
    @SuppressWarnings("unchecked")
    public void testThatIHaventMessedUpWithMultipleCommandsWithSameAlias() throws IOException {
        final Set<ClassPath.ClassInfo> ci = ClassPath.from(this.getClass().getClassLoader())
                .getTopLevelClassesRecursive("io.github.nucleuspowered.nucleus.modules");
        final Set<Class<? extends ICommandExecutor>> sc = ci.stream().map(ClassPath.ClassInfo::load)
                .filter(ICommandExecutor.class::isAssignableFrom)
                .map(x -> (Class<? extends ICommandExecutor>) x)
                .collect(Collectors.toSet());

        final Map<String, List<Class<? extends ICommandExecutor>>> map = new HashMap<>();
        for (final Class<? extends ICommandExecutor> c : sc) {
            if (c.isAnnotationPresent(Command.class)) {
                final Command command = c.getAnnotation(Command.class);
                if (command.parentCommand() != ICommandExecutor.class) {
                    Arrays.stream(command.aliases()).filter(x -> x.startsWith("#")).map(x -> x.substring(1)).forEach(x ->
                            map.computeIfAbsent(x, y -> new ArrayList<>()).add(c));
                } else {
                    Arrays.stream(command.aliases())
                            .map(x -> {
                                if (x.startsWith("#") || x.startsWith("$")) {
                                    return x.substring(1);
                                }
                                return x;
                            })
                            .forEach(x -> map.computeIfAbsent(x, y -> new ArrayList<>()).add(c));
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, List<Class<? extends ICommandExecutor>>> entry : map.entrySet()) {
            if (entry.getValue().size() > 1) {
                // nope
                if (sb.length() == 0) {
                    sb.append("Some commands are defined more than once.").append(System.lineSeparator()).append(System.lineSeparator());
                }
                sb.append(entry.getKey()).append(" -> ").append(entry.getValue().stream().map(Class::getName).collect(Collectors.joining(", ")));
            }
        }

        final String s = sb.toString();
        if (!s.isEmpty()) {
            Assert.fail(sb.toString());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCommandKeysExistInMessagesFile() throws IOException {
        final Set<ClassPath.ClassInfo> ci = ClassPath.from(this.getClass().getClassLoader())
                .getTopLevelClassesRecursive("io.github.nucleuspowered.nucleus.modules");
        final Set<Class<? extends ICommandExecutor>> sc = ci.stream().map(ClassPath.ClassInfo::load).filter(ICommandExecutor.class::isAssignableFrom)
                .map(x -> (Class<? extends ICommandExecutor>)x).collect(Collectors.toSet());

        // Get the resource
        final String bundle = "data.plugin-nucleus.messages";

        // Get the resource
        final ResourceBundle rb = ResourceBundle.getBundle(bundle, Locale.getDefault());
        final Enumeration<String> keys = rb.getKeys();
        final Set<String> s = new HashSet<>();

        while (keys.hasMoreElements()) {
            s.add(keys.nextElement());
        }

        final List<Class<? extends ICommandExecutor>> keyRoots = sc.stream()
                .filter(x -> x.isAnnotationPresent(Command.class))
                .filter(x -> !s.contains(x.getAnnotation(Command.class).commandDescriptionKey() + ".desc"))
                .collect(Collectors.toList());

        if (!keyRoots.isEmpty()) {
            final StringBuilder sb = new StringBuilder("Some command keys are not set: ");
            keyRoots.forEach(x -> sb.append("Key: ").append(x.getAnnotation(Command.class).commandDescriptionKey())
                    .append(".desc").append(" - ").append(x.getName())
                    .append(System.lineSeparator()));
            Assert.fail(sb.toString());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testThatAnyServicesHaveANoArgsOrInjectableCtor() throws IOException {
        final Set<ClassPath.ClassInfo> ci = ClassPath.from(this.getClass().getClassLoader())
                .getTopLevelClassesRecursive("io.github.nucleuspowered.nucleus.modules");
        final List<Class<?>> fails = new ArrayList<>();
        ci.stream().map(ClassPath.ClassInfo::load)
                .filter(ServiceBase.class::isAssignableFrom)
                .map(x -> (Class<? extends ServiceBase>)x)
                .forEach(x -> {
                    final Constructor<?>[] constructors = x.getDeclaredConstructors();
                    for (final Constructor<?> constructor : constructors) {
                        if (constructor.getAnnotation(com.google.inject.Inject.class) != null) {
                            return;
                        }
                    }

                    // if not, then if we have a no-args, that's okay too.
                    try {
                        final Constructor<?> ctor = x.getConstructor();
                    } catch (final NoSuchMethodException e) {
                        // Nope
                        fails.add(x);
                    }
                });

        if (!fails.isEmpty()) {
            final StringBuilder stringBuilder = new StringBuilder("Some services do not have no-args or injectable ctors:")
                    .append(System.lineSeparator());
            for (final Class<?> fail : fails) {
                stringBuilder.append(fail.getName()).append(System.lineSeparator());
            }

            Assert.fail(stringBuilder.toString());
        }
    }

    @Test
    public void testThatNoResourceKeyIsAParentOfAnother() throws Exception {
        // Get the resource
        final String bundle = "data.plugin-nucleus.messages";

        // Get the resource
        final ResourceBundle rb = ResourceBundle.getBundle(bundle, Locale.getDefault());
        final Enumeration<String> keys = rb.getKeys();
        final Set<String> s = new HashSet<>();

        while (keys.hasMoreElements()) {
            s.add(keys.nextElement());
        }

        final Map<String, List<String>> filter = s.parallelStream()
                .map(x -> Tuple.of(x.toLowerCase(),
                        s.stream().filter(y -> x.toLowerCase().startsWith(y.toLowerCase() + ".") && !x.equalsIgnoreCase(y)).collect(Collectors.toList())))
                .filter(x -> !x.second().isEmpty())
                .collect(Collectors.toMap(Tuple::first, Tuple::second));
        if (!filter.isEmpty()) {
            final StringBuilder sb = new StringBuilder("Some keys are parents of others!").append(System.lineSeparator());
            filter.forEach((x, y) -> sb.append(x).append("->").append(y).append(System.lineSeparator()));
            Assert.fail(sb.toString());
        }
    }

    @Test
    public void testCommandsListenersTasksAreInjectable() throws IOException {
        final Set<ClassPath.ClassInfo> ci = ClassPath.from(this.getClass().getClassLoader())
                .getTopLevelClassesRecursive("io.github.nucleuspowered.nucleus.modules");
        final Set<Class<?>> sc = ci.stream().map(ClassPath.ClassInfo::load)
                .filter(x -> ICommandExecutor.class.isAssignableFrom(x)
                        || ListenerBase.class.isAssignableFrom(x)
                        || TaskBase.class.isAssignableFrom(x)
                        || IModule.class.isAssignableFrom(x))
                .filter(x -> !x.isInterface() && !Modifier.isAbstract(x.getModifiers()))
                .filter(x -> {
                    final Constructor<?>[] constructors = x.getDeclaredConstructors();
                    int validCtors = 0;
                    for (final Constructor<?> ctor : constructors) {
                        if (Modifier.isPublic(ctor.getModifiers()) && (ctor.getParameterCount() == 0 || ctor.getAnnotation(Inject.class) != null)) {
                            validCtors++;
                        }
                    }
                    return validCtors != 1;
                })
                .collect(Collectors.toSet());

        if (!sc.isEmpty()) {
            final StringBuilder sb = new StringBuilder("Some classes that are injected into are not @Injectable!").append(System.lineSeparator());
            sc.forEach(x -> sb.append(x).append(System.lineSeparator()));
            Assert.fail(sb.toString());
        }
    }

}
