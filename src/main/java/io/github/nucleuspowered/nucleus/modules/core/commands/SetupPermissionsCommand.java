/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.permissions.ServiceChangeListener;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@Permissions(prefix = "nucleus", suggestedLevel = SuggestedLevel.NONE)
@NoModifiers
@NonnullByDefault
@RunAsync
@RegisterCommand(value = {"setupperms", "setperms"}, subcommandOf = NucleusCommand.class)
public class SetupPermissionsCommand extends AbstractCommand<CommandSource> {

    private final PermissionRegistry permissionRegistry = Nucleus.getNucleus().getPermissionRegistry();

    private final String roleKey = "Nucleus Role";
    private final String groupKey = "Permission Group";
    private final String withGroupsKey = "-g";
    private final String acceptGroupKey = "-y";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.firstParsing(
                        GenericArguments.seq(
                                GenericArguments.literal(Text.of(this.withGroupsKey), withGroupsKey),
                                GenericArguments.optional(
                                        GenericArguments.literal(Text.of(this.acceptGroupKey), acceptGroupKey))),
                        GenericArguments.flags()
                                .flag("r", "-reset")
                                .flag("i", "-inherit")
                                .buildWith(GenericArguments.seq(
                            GenericArguments.onlyOne(GenericArguments.enumValue(Text.of(roleKey), SuggestedLevel.class)),
                            GenericArguments.onlyOne(new GroupArgument(Text.of(groupKey))))))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        if (args.hasAny(this.withGroupsKey)) {
            if (ServiceChangeListener.isOpOnly()) {
                // Fail
                throw ReturnMessageException.fromKey("args.permissiongroup.noservice");
            }

            if (args.hasAny(this.acceptGroupKey)) {
                setupGroups(src);
            } else {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.nucleus.permission.groups.info"));
                src.sendMessage(
                        plugin.getMessageProvider().getTextMessageWithFormat("command.nucleus.permission.groups.info2")
                            .toBuilder().onClick(TextActions.runCommand("/nucleus:nucleus setupperms -g -y"))
                            .onHover(TextActions.showText(Text.of("/nucleus:nucleus setupperms -g -y")))
                            .build()
                );
            }

            return CommandResult.success();
        }

        // The GroupArgument should have already checked for this.
        SuggestedLevel sl = args.<SuggestedLevel>getOne(roleKey).get();
        Subject group = args.<Subject>getOne(groupKey).get();
        boolean reset = args.hasAny("r");
        boolean inherit = args.hasAny("i");

        setupPerms(src, group, sl, reset, inherit);

        return CommandResult.success();
    }

    private void setupGroups(CommandSource src) throws Exception {
        String adminGroup = "admin";
        String modGroup = "mod";
        String defaultGroup = "default";
        MessageProvider messageProvider = Nucleus.getNucleus().getMessageProvider();

        // Create groups
        PermissionService permissionService = Sponge.getServiceManager().provide(PermissionService.class)
                .orElseThrow(() -> ReturnMessageException.fromKey("args.permissiongroup.noservice"));

        // check for admin
        Subject admin = permissionService.getGroupSubjects().getSubject(adminGroup).orElseGet(() -> {
            src.sendMessage(messageProvider.getTextMessageWithFormat("command.nucleus.permission.create", adminGroup));
            return permissionService.getGroupSubjects().loadSubject(adminGroup).join();
        });

        // create mod
        Subject mod = permissionService.getGroupSubjects().getSubject(modGroup).orElseGet(() -> {
            src.sendMessage(messageProvider.getTextMessageWithFormat("command.nucleus.permission.create", modGroup));
            return permissionService.getGroupSubjects().loadSubject(modGroup).join();
        });

        // right now, LuckPerms is the defacto permissions plugin. So we look for the "default" group
        Subject defaults = permissionService.getGroupSubjects().getSubject(defaultGroup).orElseGet(() -> {
            src.sendMessage(messageProvider.getTextMessageWithFormat("command.nucleus.permission.create", defaultGroup));
            return permissionService.getGroupSubjects().loadSubject(defaultGroup).join();
        });

        src.sendMessage(messageProvider.getTextMessageWithFormat("command.nucleus.permission.inherit", modGroup, adminGroup));
        admin.getSubjectData().addParent(ImmutableSet.of(), mod.asSubjectReference());

        src.sendMessage(messageProvider.getTextMessageWithFormat("command.nucleus.permission.inherit", defaults.getIdentifier(), modGroup));
        mod.getSubjectData().addParent(ImmutableSet.of(), defaults.asSubjectReference());

        src.sendMessage(messageProvider.getTextMessageWithFormat("command.nucleus.permission.perms"));
        setupPerms(src, admin, SuggestedLevel.ADMIN, false, false);
        setupPerms(src, mod, SuggestedLevel.MOD, false, false);
        setupPerms(src, defaults, SuggestedLevel.USER, false, false);
        src.sendMessage(messageProvider.getTextMessageWithFormat("command.nucleus.permission.completegroups"));
    }

    private void setupPerms(CommandSource src, Subject group, SuggestedLevel level, boolean reset, boolean inherit) throws Exception {
        if (inherit && level.getLowerLevel() != null) {
            setupPerms(src, group, level.getLowerLevel(), reset, inherit);
        }

        Set<Context> globalContext = Sets.newHashSet();
        SubjectData data = group.getSubjectData();
        Set<String> definedPermissions = data.getPermissions(ImmutableSet.of()).keySet();
        Logger logger = Nucleus.getNucleus().getLogger();
        MessageProvider messageProvider = Nucleus.getNucleus().getMessageProvider();

        // Register all the permissions, but only those that have yet to be assigned.
        permissionRegistry.getPermissions().entrySet().stream()
                .filter(x -> x.getValue().level == level)
                .filter(x -> reset || !definedPermissions.contains(x.getKey()))
                .forEach(x -> {
                    logger.info(messageProvider.getMessageWithFormat("command.nucleus.permission.added", x.getKey(), group.getIdentifier()));
                    data.setPermission(globalContext, x.getKey(), Tristate.TRUE);
                });

        src.sendMessage(plugin.getMessageProvider()
                .getTextMessageWithFormat("command.nucleus.permission.complete", level.toString().toLowerCase(), group.getIdentifier()));
    }

    private static class GroupArgument extends CommandElement {

        GroupArgument(@Nullable Text key) {
            super(key);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            String a = args.next();
            Optional<String> ls = getGroups(args).stream().filter(x -> x.equalsIgnoreCase(a)).findFirst();
            if (ls.isPresent()) {
                return Sponge.getServiceManager().provide(PermissionService.class).get()
                        .getGroupSubjects().getSubject(ls.get()).get();
            }

            throw args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.permissiongroup.nogroup", a));
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            try {
                String a = args.peek();
                return getGroups(args).stream().filter(x -> x.toLowerCase().contains(a)).collect(Collectors.toList());
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }

        private Set<String> getGroups(CommandArgs args) throws ArgumentParseException {
            Optional<PermissionService> ops = Sponge.getServiceManager().provide(PermissionService.class);
            if (!ops.isPresent()) {
                throw args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.permissiongroup.noservice"));
            }

            PermissionService ps = ops.get();
            try {
                return Sets.newHashSet(ps.getGroupSubjects().getAllIdentifiers().get());
            } catch (Exception e) {
                // TODO - Sort this out for API 7+
                if (Nucleus.getNucleus().isDebugMode()) {
                    e.printStackTrace();
                }

                throw args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.permissiongroup.failed"));
            }
        }
    }
}
