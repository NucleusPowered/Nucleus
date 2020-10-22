/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.tests.sanity;

import com.google.common.collect.Lists;
import com.google.common.reflect.ClassPath;
import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.StandardModule;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import org.junit.Assert;
import org.junit.Test;
import org.spongepowered.api.util.Tuple;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.config.AbstractConfigAdapter;

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

import javax.inject.Inject;

public class SanityTests {

    @Test
    @SuppressWarnings("unchecked")
    public void testThatAnythingThatIsAConcreteModuleHasAModuleDataAnnotation() throws IOException {
        Set<ClassPath.ClassInfo> ci = ClassPath.from(this.getClass().getClassLoader())
                .getTopLevelClassesRecursive("io.github.nucleuspowered.nucleus.modules");
        Set<Class<? extends StandardModule>> sc = ci.stream().map(ClassPath.ClassInfo::load).filter(StandardModule.class::isAssignableFrom)
                .map(x -> (Class<? extends StandardModule>)x).collect(Collectors.toSet());

        List<Class<?>> moduleList = sc.stream().filter(x -> !x.isAnnotationPresent(ModuleData.class)).collect(Collectors.toList());
        if (!moduleList.isEmpty()) {
            StringBuilder sb = new StringBuilder("Some modules do not have the ModuleData annotation: ");
            moduleList.forEach(x -> sb.append(x.getName()).append(System.lineSeparator()));
            Assert.fail(sb.toString());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testThatIHaventMessedUpWithMultipleCommandsWithSameAlias() throws IOException {
        Set<ClassPath.ClassInfo> ci = ClassPath.from(this.getClass().getClassLoader())
                .getTopLevelClassesRecursive("io.github.nucleuspowered.nucleus.modules");
        Set<Class<? extends ICommandExecutor<?>>> sc = ci.stream().map(ClassPath.ClassInfo::load)
                .filter(ICommandExecutor.class::isAssignableFrom)
                .map(x -> (Class<? extends ICommandExecutor<?>>)x)
                .collect(Collectors.toSet());

        final Map<String, List<Class<? extends ICommandExecutor<?>>>> map = new HashMap<>();
        for (final Class<? extends ICommandExecutor<?>> c : sc) {
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
        for (final Map.Entry<String, List<Class<? extends ICommandExecutor<?>>>> entry : map.entrySet()) {
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
        Set<ClassPath.ClassInfo> ci = ClassPath.from(this.getClass().getClassLoader())
                .getTopLevelClassesRecursive("io.github.nucleuspowered.nucleus.modules");
        Set<Class<? extends ICommandExecutor<?>>> sc = ci.stream().map(ClassPath.ClassInfo::load).filter(ICommandExecutor.class::isAssignableFrom)
                .map(x -> (Class<? extends ICommandExecutor<?>>)x).collect(Collectors.toSet());

        // Get the resource
        String bundle = "assets.nucleus.messages";

        // Get the resource
        ResourceBundle rb = ResourceBundle.getBundle(bundle, Locale.getDefault());
        Enumeration<String> keys = rb.getKeys();
        Set<String> s = new HashSet<>();

        while (keys.hasMoreElements()) {
            s.add(keys.nextElement());
        }

        List<Class<? extends ICommandExecutor<?>>> keyRoots = sc.stream()
                .filter(x -> x.isAnnotationPresent(Command.class))
                .filter(x -> !s.contains(x.getAnnotation(Command.class).commandDescriptionKey() + ".desc"))
                .collect(Collectors.toList());

        if (!keyRoots.isEmpty()) {
            StringBuilder sb = new StringBuilder("Some command keys are not set: ");
            keyRoots.forEach(x -> sb.append("Key: ").append(x.getAnnotation(Command.class).commandDescriptionKey())
                    .append(".desc").append(" - ").append(x.getName())
                    .append(System.lineSeparator()));
            Assert.fail(sb.toString());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testThatAnyServicesHaveANoArgsOrInjectableCtor() throws IOException {
        Set<ClassPath.ClassInfo> ci = ClassPath.from(this.getClass().getClassLoader())
                .getTopLevelClassesRecursive("io.github.nucleuspowered.nucleus.modules");
        List<Class<?>> fails = Lists.newArrayList();
        ci.stream().map(ClassPath.ClassInfo::load)
                .filter(ServiceBase.class::isAssignableFrom)
                .map(x -> (Class<? extends ServiceBase>)x)
                .forEach(x -> {
                    Constructor<?>[] constructors = x.getDeclaredConstructors();
                    for (Constructor<?> constructor : constructors) {
                        if (constructor.getAnnotation(Inject.class) != null || constructor.getAnnotation(com.google.inject.Inject.class) != null) {
                            return;
                        }
                    }

                    // if not, then if we have a no-args, that's okay too.
                    try {
                        Constructor<?> ctor = x.getConstructor();
                    } catch (NoSuchMethodException e) {
                        // Nope
                        fails.add(x);
                    }
                });

        if (!fails.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder("Some services do not have no-args or injectable ctors:")
                    .append(System.lineSeparator());
            for (Class<?> fail : fails) {
                stringBuilder.append(fail.getName()).append(System.lineSeparator());
            }

            Assert.fail(stringBuilder.toString());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testThatAnythingThatIsAnAbstractConfigAdapterIsAlsoANucleusConfigAdapter() throws IOException {
        Set<ClassPath.ClassInfo> ci = ClassPath.from(this.getClass().getClassLoader())
                .getTopLevelClassesRecursive("io.github.nucleuspowered.nucleus.modules");
        Set<Class<? extends AbstractConfigAdapter<?>>> sc = ci.stream().map(ClassPath.ClassInfo::load)
                .filter(AbstractConfigAdapter.class::isAssignableFrom)
                .map(x -> (Class<? extends AbstractConfigAdapter<?>>)x).collect(Collectors.toSet());

        List<Class<?>> moduleList = sc.stream().filter(x -> !NucleusConfigAdapter.class.isAssignableFrom(x)).collect(Collectors.toList());
        if (!moduleList.isEmpty()) {
            StringBuilder sb = new StringBuilder("Some config adapters are not of the NucleusConfigAdapter type: ");
            moduleList.forEach(x -> sb.append(x.getName()).append(System.lineSeparator()));
            Assert.fail(sb.toString());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testThatAnyConstructorInCommandsThatIsNotTheDefaultConstructorIsInjected() throws Exception {
        Set<ClassPath.ClassInfo> ci = ClassPath.from(this.getClass().getClassLoader())
                .getTopLevelClassesRecursive("io.github.nucleuspowered.nucleus.modules");
        Set<Class<? extends ICommandExecutor<?>>> sc = ci.stream().map(ClassPath.ClassInfo::load)
                .filter(ICommandExecutor.class::isAssignableFrom)
                .filter(x -> !Modifier.isAbstract(x.getModifiers()))
                .map(x -> (Class<? extends ICommandExecutor<?>>)x)
                .filter(x -> {
                    boolean isDefault = true;
                    for (Constructor t : x.getDeclaredConstructors()) {
                        if (t.getParameterCount() > 0) {
                            isDefault = false;
                            if (t.isAnnotationPresent(Inject.class) || t.isAnnotationPresent(com.google.inject.Inject.class)) {
                                return false;
                            }
                        }
                    }

                    return !isDefault;
                })
                .collect(Collectors.toSet());


        if (!sc.isEmpty()) {
            StringBuilder sb = new StringBuilder("Some commands do not have injectable constructors: ");
            sb.append(System.lineSeparator());
            sc.forEach(x -> sb.append(x.getName()).append(System.lineSeparator()));
            Assert.fail(sb.toString());
        }
    }

    @Test
    public void testThatNoResourceKeyIsAParentOfAnother() throws Exception {
        // Get the resource
        String bundle = "assets.nucleus.messages";

        // Get the resource
        ResourceBundle rb = ResourceBundle.getBundle(bundle, Locale.getDefault());
        Enumeration<String> keys = rb.getKeys();
        Set<String> s = new HashSet<>();

        while (keys.hasMoreElements()) {
            s.add(keys.nextElement());
        }

        Map<String, List<String>> filter = s.parallelStream()
                .map(x -> Tuple.of(x.toLowerCase(),
                        s.stream().filter(y -> x.toLowerCase().startsWith(y.toLowerCase() + ".") && !x.equalsIgnoreCase(y)).collect(Collectors.toList())))
                .filter(x -> !x.getSecond().isEmpty())
                .collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
        if (!filter.isEmpty()) {
            StringBuilder sb = new StringBuilder("Some keys are parents of others!").append(System.lineSeparator());
            filter.forEach((x, y) -> sb.append(x).append("->").append(y).append(System.lineSeparator()));
            Assert.fail(sb.toString());
        }
    }


}
