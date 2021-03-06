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

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.firstParsing(
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
        // The GroupArgument should have already checked for this.
        SuggestedLevel sl = context.requireOne(this.roleKey, SuggestedLevel.class);
        Subject group = context.requireOne(this.groupKey, Subject.class);
        boolean reset = context.hasAny("r");
        boolean inherit = context.hasAny("i");

        return this.setupPerms(context, group, sl, reset, inherit);
    }

    private ICommandResult setupPerms(ICommandContext<? extends CommandSource> src, Subject group, SuggestedLevel level, boolean reset, boolean inherit) {
        final PermissionService ps = Sponge.getServiceManager().provideUnchecked(PermissionService.class);
        if (ps.getClass().getPackage().getName().startsWith("org.spongepowered.common")) {
            // nope the f out of here.
            return src.errorResult("command.nucleus.permission.noperms");
        }

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
        return src.successResult();
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

            final PermissionService ps = ops.get();
            if (ps.getClass().getPackage().getName().startsWith("org.spongepowered.common")) {
                // nope the f out of here.
                throw args.createError(this.messageProviderService.getMessageFor(source,  "command.nucleus.permission.noperms"));
            }

            try {
                return Sets.newHashSet(ps.getGroupSubjects().getAllIdentifiers().get());
            } catch (Exception e) {
                e.printStackTrace();
                throw args.createError(this.messageProviderService.getMessageFor(source, "args.permissiongroup.failed"));
            }
        }
    }
}
