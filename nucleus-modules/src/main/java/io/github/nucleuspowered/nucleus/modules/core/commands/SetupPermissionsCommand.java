/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@NonnullByDefault
@Command(
        aliases = {"setupperms", "setperms"},
        basePermission = CorePermissions.BASE_NUCLEUS_SETUPPERMS,
        commandDescriptionKey = "nucleus.setupperms",
        parentCommand = NucleusCommand.class
)
public class SetupPermissionsCommand implements ICommandExecutor<CommandSource> {

    private final String roleKey = "Nucleus Role";
    private final String groupKey = "Permission Group";
    private final String withGroupsKey = "-g";
    private final String acceptGroupKey = "-y";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.firstParsing(
                        GenericArguments.seq(
                                GenericArguments.literal(Text.of(this.withGroupsKey), this.withGroupsKey),
                                GenericArguments.optional(
                                        GenericArguments.literal(Text.of(this.acceptGroupKey), this.acceptGroupKey))),
                        GenericArguments.flags()
                                .flag("r", "-reset")
                                .flag("i", "-inherit")
                                .buildWith(GenericArguments.seq(
                            GenericArguments.onlyOne(GenericArguments.enumValue(Text.of(this.roleKey), SuggestedLevel.class)),
                            GenericArguments.onlyOne(new GroupArgument(Text.of(this.groupKey), serviceCollection.messageProvider())))))
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        IPermissionService permissionService = context.getServiceCollection().permissionService();
        if (context.hasAny(this.withGroupsKey)) {
            if (permissionService.isOpOnly()) {
                // Fail
                return context.errorResult("args.permissiongroup.noservice");
            }

            if (context.hasAny(this.acceptGroupKey)) {
                setupGroups(context);
            } else {
                context.sendMessage("command.nucleus.permission.groups.info");
                context.getCommandSource().sendMessage(
                        context.getServiceCollection().messageProvider().getMessageFor(
                                context.getCommandSource(), "command.nucleus.permission.groups.info2")
                            .toBuilder().onClick(TextActions.runCommand("/nucleus:nucleus setupperms -g -y"))
                            .onHover(TextActions.showText(Text.of("/nucleus:nucleus setupperms -g -y")))
                            .build()
                );
            }

            return context.successResult();
        }

        // The GroupArgument should have already checked for this.
        SuggestedLevel sl = context.requireOne(this.roleKey, SuggestedLevel.class);
        Subject group = context.requireOne(this.groupKey, Subject.class);
        boolean reset = context.hasAny("r");
        boolean inherit = context.hasAny("i");

        setupPerms(context, group, sl, reset, inherit);

        return context.successResult();
    }

    private void setupGroups(ICommandContext<? extends CommandSource> context) throws CommandException {
        IMessageProviderService messageProvider = context.getServiceCollection().messageProvider();
        String ownerGroup = "owner";
        String adminGroup = "admin";
        String modGroup = "mod";
        String defaultGroup = "default";

        // Create groups
        PermissionService permissionService = Sponge.getServiceManager().provide(PermissionService.class)
                .orElseThrow(() -> context.createException("args.permissiongroup.noservice"));

        // check for admin
        Subject owner = getSubject(ownerGroup, context, permissionService);
        Subject admin = getSubject(adminGroup, context, permissionService);
        Subject mod = getSubject(modGroup, context, permissionService);
        Subject defaults = getSubject(defaultGroup, context, permissionService);

        BiFunction<String, String, CommandException> biFunction = (key, group) -> new CommandException(
                messageProvider.getMessageFor(context.getCommandSourceUnchecked(), key, group)
        );

        context.sendMessage("command.nucleus.permission.inherit", adminGroup, ownerGroup);
        addParent(owner, admin, biFunction);

        context.sendMessage("command.nucleus.permission.inherit", modGroup, adminGroup);
        addParent(admin, mod, biFunction);

        context.sendMessage("command.nucleus.permission.inherit", defaultGroup, modGroup);
        addParent(mod, defaults, biFunction);

        context.sendMessage("command.nucleus.permission.perms");
        setupPerms(context, owner, SuggestedLevel.OWNER, false, false);
        setupPerms(context, admin, SuggestedLevel.ADMIN, false, false);
        setupPerms(context, mod, SuggestedLevel.MOD, false, false);
        setupPerms(context, defaults, SuggestedLevel.USER, false, false);
        context.sendMessage("command.nucleus.permission.completegroups");
    }

    private void addParent(Subject parent, Subject target, BiFunction<String, String, CommandException> exceptionBiFunction) throws CommandException {
        if (!target.getSubjectData().addParent(ImmutableSet.of(), parent.asSubjectReference()).join()) {
            // there's a problem
            throw exceptionBiFunction.apply("command.nucleus.permission.group.fail", target.getIdentifier());
        }
    }

    private Subject getSubject(String group, ICommandContext<? extends CommandSource> src, PermissionService service) {
        return service.getGroupSubjects().getSubject(group).orElseGet(() -> {
            src.sendMessage("command.nucleus.permission.create", group);
            return service.getGroupSubjects().loadSubject(group).join();
        });
    }

    private void setupPerms(ICommandContext<? extends CommandSource> src, Subject group, SuggestedLevel level, boolean reset, boolean inherit) {
        if (inherit && level.getLowerLevel() != null) {
            setupPerms(src, group, level.getLowerLevel(), reset, inherit);
        }

        Set<Context> globalContext = Sets.newHashSet();
        SubjectData data = group.getSubjectData();
        Set<String> definedPermissions = data.getPermissions(ImmutableSet.of()).keySet();
        Logger logger = src.getServiceCollection().logger();
        IMessageProviderService messageProvider = src.getServiceCollection().messageProvider();
        IPermissionService permissionService = src.getServiceCollection().permissionService();

        // Register all the permissions, but only those that have yet to be assigned.
        permissionService.getAllMetadata().stream()
                .filter(x -> x.getSuggestedLevel() == level)
                .filter(x -> reset || !definedPermissions.contains(x.getPermission()))
                .forEach(x -> {
                    logger.info(messageProvider.getMessageString("command.nucleus.permission.added", x.getPermission(), group.getIdentifier()));
                    data.setPermission(globalContext, x.getPermission(), Tristate.TRUE);
                });

        src.sendMessage("command.nucleus.permission.complete", level.toString().toLowerCase(), group.getIdentifier());
    }

    private static class GroupArgument extends CommandElement {

        private final IMessageProviderService messageProviderService;

        GroupArgument(@Nullable Text key, IMessageProviderService messageProviderService) {
            super(key);
            this.messageProviderService = messageProviderService;
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            String a = args.next();
            Optional<String> ls = getGroups(source, args).stream().filter(x -> x.equalsIgnoreCase(a)).findFirst();
            if (ls.isPresent()) {
                return Sponge.getServiceManager().provide(PermissionService.class).get()
                        .getGroupSubjects().getSubject(ls.get()).get();
            }

            throw args.createError(this.messageProviderService.getMessageFor(source, "args.permissiongroup.nogroup", a));
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            try {
                String a = args.peek();
                return getGroups(src, args).stream().filter(x -> x.toLowerCase().contains(a)).collect(Collectors.toList());
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }

        private Set<String> getGroups(CommandSource source, CommandArgs args) throws ArgumentParseException {
            Optional<PermissionService> ops = Sponge.getServiceManager().provide(PermissionService.class);
            if (!ops.isPresent()) {
                throw args.createError(this.messageProviderService.getMessageFor(source, "args.permissiongroup.noservice"));
            }

            PermissionService ps = ops.get();
            try {
                return Sets.newHashSet(ps.getGroupSubjects().getAllIdentifiers().get());
            } catch (Exception e) {
                e.printStackTrace();
                throw args.createError(this.messageProviderService.getMessageFor(source, "args.permissiongroup.failed"));
            }
        }
    }
}
