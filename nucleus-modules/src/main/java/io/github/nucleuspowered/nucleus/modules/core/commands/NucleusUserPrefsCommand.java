/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.core.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.PreferenceKeyImpl;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.UserPreferenceService;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserPreferenceService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

@Command(
        aliases = { "nuserprefs", "userprefs" },
        basePermission = CorePermissions.BASE_NUSERPREFS,
        commandDescriptionKey = "nuserprefs",
        prefixAliasesWithN = false,
        async = true,
        associatedPermissions = CorePermissions.OTHERS_NUSERPREFS
)
@NonnullByDefault
public class NucleusUserPrefsCommand implements ICommandExecutor<CommandSource> {

    private static final Text SEPARATOR = Text.of(": ");
    private final IUserPreferenceService userPreferenceService;

    @Inject
    public NucleusUserPrefsCommand(INucleusServiceCollection serviceCollection) {
        this.userPreferenceService = serviceCollection.userPreferenceService();
    }

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier().createOtherUserPermissionElement(false, CorePermissions.OTHERS_NUSERPREFS),
                GenericArguments.optional(this.userPreferenceService.getElement())
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User target = context.getUserFromArgs();
        if (context.hasAny(UserPreferenceService.PREFERENCE_ARG.toPlain())) {
            // do we get or set?
            if (context.hasAny(UserPreferenceService.VALUE_ARG.toPlain())) {
                return set(context, target, context.is(target),
                        context.requireOne(UserPreferenceService.PREFERENCE_ARG.toPlain(), PreferenceKeyImpl.class),
                        context.requireOne(UserPreferenceService.VALUE_ARG.toPlain(), Object.class));
            } else {
                return get(context, target, context.requireOne(UserPreferenceService.PREFERENCE_ARG.toPlain(), PreferenceKeyImpl.class));
            }
        } else {
            return list(context, target);
        }

    }

    private <T> ICommandResult set(
            ICommandContext<? extends CommandSource> context,
            User target,
            boolean isSelf,
            PreferenceKeyImpl<T> key,
            @Nullable Object value) {
        this.userPreferenceService.set(target.getUniqueId(), key, key.getValueClass().cast(value));
        if (isSelf) {
            context.sendMessage("command.userprefs.set.self", key.getID(), value);
        } else {
            context.sendMessage("command.userprefs.set.other", target, key.getID(), value);
        }
        return context.successResult();
    }

    private <T> ICommandResult get(ICommandContext<? extends CommandSource> context, User target, PreferenceKeyImpl<T> key) throws CommandException {
        context.sendMessageText(
                get(context, context.getServiceCollection().userPreferenceService(),
                        key,
                        this.userPreferenceService.get(target.getUniqueId(), key).orElse(null)));
        return context.successResult();
    }

    private ICommandResult list(ICommandContext<? extends CommandSource> context, User target) throws CommandException {
        Map<NucleusUserPreferenceService.PreferenceKey<?>, Object> ret = this.userPreferenceService.get(target);

        List<Text> entry = new ArrayList<>();
        for (Map.Entry<NucleusUserPreferenceService.PreferenceKey<?>, Object> e : ret.entrySet()) {
            NucleusUserPreferenceService.PreferenceKey<?> key = e.getKey();
            Object value = e.getValue();
            entry.add(get(context, this.userPreferenceService, key, value));
        }

        Util.getPaginationBuilder(context.getCommandSource())
                .title(context.getServiceCollection().messageProvider().getMessageFor(
                        context.getCommandSource(), "command.userprefs.title", target.getName()))
            .contents(entry).build().sendTo(context.getCommandSource());
        return context.successResult();
    }

    private Text get(ICommandContext<? extends CommandSource> context,
            IUserPreferenceService userPreferenceService,
            NucleusUserPreferenceService.PreferenceKey<?> key,
            @Nullable Object value) throws CommandException {
        Text.Builder tb = Text.builder(key.getID().replaceAll("^nucleus:", ""));
        tb.append(SEPARATOR);
        Text result;
        CommandSource commandSource = context.getCommandSource();
        if (value == null) {
            result = context.getServiceCollection().messageProvider().getMessageFor(commandSource, "standard.unset");
        } else if (value instanceof Boolean) {
            result = context.getServiceCollection().messageProvider().getMessageFor(commandSource, "standard." + (boolean) value);
        } else {
            result = Text.of(value);
        }

        tb.append(result);
        String desc = userPreferenceService.getDescription(key);
        if (desc != null && !desc.isEmpty()) {
            tb.onHover(TextActions.showText(
                    key instanceof PreferenceKeyImpl ?
                        context.getServiceCollection().messageProvider()
                                .getMessageFor(commandSource, ((PreferenceKeyImpl<?>) key).getDescriptionKey()) :
                        Text.of(desc)));
        }
        return tb.build();
    }

}
