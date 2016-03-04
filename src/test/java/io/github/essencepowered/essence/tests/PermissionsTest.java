/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.tests;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.PermissionRegistry;
import io.github.essencepowered.essence.internal.annotations.Permissions;
import io.github.essencepowered.essence.internal.annotations.RegisterCommand;
import io.github.essencepowered.essence.tests.util.TestModule;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.service.permission.Subject;

import java.util.Arrays;

/**
 * Tests permissions are assigned correctly.
 */
public class PermissionsTest {

    /**
     * Tests that the specified permissions are in the permission list.
     */
    @RunWith(Parameterized.class)
    public static class ValidTest {

        @Parameterized.Parameters(name = "{index}: Permission {0} on {1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    { PermissionRegistry.PERMISSIONS_PREFIX + "test.base", PermissionOne.class },
                    { PermissionRegistry.PERMISSIONS_PREFIX + "root.test.base", PermissionRoot.class },
                    { PermissionRegistry.PERMISSIONS_PREFIX + "test.sub.base", PermissionSub.class },
                    { PermissionRegistry.PERMISSIONS_PREFIX + "root.test.sub.base", PermissionRootSub.class },
                    { PermissionRegistry.PERMISSIONS_PREFIX + "test.base", PermissionCustom.class },
                    { PermissionRegistry.PERMISSIONS_PREFIX + "alias.base", PermissionAlias.class }
            });
        }

        @Parameterized.Parameter(0)
        public String permission;

        @Parameterized.Parameter(1)
        public Class<? extends CommandBase> clazz;

        @Test
        public void testPermissionIsValid() throws IllegalAccessException, InstantiationException {
            CommandBase c = clazz.newInstance();
            getInjector().injectMembers(c);
            c.postInit();
            Subject s = Mockito.mock(Subject.class);
            Mockito.when(s.hasPermission(Matchers.any())).thenReturn(false);
            Mockito.when(s.hasPermission(Matchers.eq(permission))).thenReturn(true);
            Mockito.validateMockitoUsage();
            Assert.assertTrue("The permission " + permission + " was not available for " + clazz.getName(), c.getPermissionHandler().testBase(s));
        }

    }

    /**
     * Tests that the specified permissions are not in the permission list.
     */
    @RunWith(Parameterized.class)
    public static class InvalidTest {

        @Parameterized.Parameters
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    { PermissionRegistry.PERMISSIONS_PREFIX + "test2.base", PermissionOne.class },
                    { PermissionRegistry.PERMISSIONS_PREFIX + "test.base", PermissionRoot.class },
                    { PermissionRegistry.PERMISSIONS_PREFIX + "test.base", PermissionSub.class },
                    { PermissionRegistry.PERMISSIONS_PREFIX + "test.base", PermissionRootSub.class },
                    { PermissionRegistry.PERMISSIONS_PREFIX + "root.test.base", PermissionRootSub.class },
                    { PermissionRegistry.PERMISSIONS_PREFIX + "test.sub.base", PermissionRootSub.class }
            });
        }

        @Parameterized.Parameter(0)
        public String permission;

        @Parameterized.Parameter(1)
        public Class<? extends CommandBase> clazz;

        @Test
        public void testPermissionIsNotValid() throws IllegalAccessException, InstantiationException {
            CommandBase c = clazz.newInstance();
            getInjector().injectMembers(c);
            c.postInit();
            Subject s = Mockito.mock(Subject.class);
            Mockito.when(s.hasPermission(Matchers.any())).thenReturn(false);
            Mockito.when(s.hasPermission(Matchers.eq(permission))).thenReturn(true);
            Mockito.validateMockitoUsage();
            Assert.assertFalse("The permission " + permission + " was not available for " + clazz.getName(), c.getPermissionHandler().testBase(s));
        }
    }

    @Permissions
    @RegisterCommand({"test", "test2"})
    public static class PermissionOne extends CommandBase {

        @Override
        public CommandSpec createSpec() {
            return CommandSpec.builder().executor(this).build();
        }

        @Override
        public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
            return null;
        }
    }

    @Permissions(root = "root")
    @RegisterCommand({"test", "test2"})
    public static class PermissionRoot extends PermissionOne { }

    @Permissions(sub = "sub")
    @RegisterCommand({"test", "test2"})
    public static class PermissionSub extends PermissionOne { }

    @Permissions(root = "root", sub = "sub")
    @RegisterCommand({"test", "test2"})
    public static class PermissionRootSub extends PermissionOne { }

    @Permissions(alias = "alias")
    @RegisterCommand({"test", "test2"})
    public static class PermissionAlias extends PermissionOne { }

    @Permissions({"test.test"})
    @RegisterCommand({"test", "test2"})
    public static class PermissionCustom extends PermissionOne { }

    private static Injector getInjector() {
        return Guice.createInjector(new TestModule());
    }
}
