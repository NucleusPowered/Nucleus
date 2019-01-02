/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.service.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoCommandPrefix;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.userprefs.PreferenceKeyImpl;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPreferenceService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
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

@RunAsync
@Permissions(mainOverride = "userprefs", supportsOthers = true, suggestedLevel = SuggestedLevel.USER)
@NoModifiers
@RegisterCommand({ "nuserprefs", "userprefs" })
@NoCommandPrefix
@NonnullByDefault
public class NucleusUserPrefsCommand extends AbstractCommand.SimpleTargetOtherUser {

    private static final Text SEPARATOR = Text.of(": ");

    @Override
    protected CommandElement[] additionalArguments() {
        return new CommandElement[] {
                GenericArguments.optional(getServiceUnchecked(UserPreferenceService.class).getElement())
        };
    }

    @Override protected CommandResult executeWithPlayer(CommandSource source, User target, CommandContext args, boolean isSelf)
            throws Exception {

        if (args.hasAny(UserPreferenceService.PREFERENCE_ARG)) {
            // do we get or set?
            if (args.hasAny(UserPreferenceService.VALUE_ARG)) {
                return set(source, target, isSelf,
                        args.requireOne(UserPreferenceService.PREFERENCE_ARG),
                        args.requireOne(UserPreferenceService.VALUE_ARG));
            } else {
                return get(source, target, args.requireOne(UserPreferenceService.PREFERENCE_ARG));
            }
        } else {
            return list(source, target);
        }

    }

    private <T> CommandResult set(CommandSource source, User target, boolean isSelf, PreferenceKeyImpl<T> key,
            @Nullable Object value) {
        UserPreferenceService service = getServiceUnchecked(UserPreferenceService.class);
        service.set(target.getUniqueId(), key, key.getValueClass().cast(value));
        if (isSelf) {
            sendMessageTo(source, "command.userprefs.set.self", key.getID(), String.valueOf(value));
        } else {
            sendMessageTo(source, "command.userprefs.set.other", target, key.getID(), String.valueOf(value));
        }
        return CommandResult.success();
    }

    private <T> CommandResult get(CommandSource source, User target, PreferenceKeyImpl<T> key) {
        UserPreferenceService service = getServiceUnchecked(UserPreferenceService.class);
        source.sendMessage(get(source, key, service.get(target.getUniqueId(), key).orElse(null)));
        return CommandResult.success();
    }

    private CommandResult list(CommandSource source, User target) {
        UserPreferenceService service = getServiceUnchecked(UserPreferenceService.class);
        Map<NucleusUserPreferenceService.PreferenceKey<?>, Object> ret = service.get(target);

        List<Text> entry = new ArrayList<>();
        ret.forEach((key, value) -> entry.add(get(source, key, value)));

        Util.getPaginationBuilder(source).title(getMessageFor(source, "command.userprefs.title", target.getName()))
            .contents(entry).build().sendTo(source);
        return CommandResult.success();
    }

    private Text get(CommandSource source, NucleusUserPreferenceService.PreferenceKey<?> key, @Nullable Object value) {
        Text.Builder tb = Text.builder(key.getID().replaceAll("^nucleus:", ""));
        tb.append(SEPARATOR);
        Text result;
        if (value == null) {
            result = getMessageFor(source, "standard.unset");
        } else if (value instanceof Boolean) {
            result = getMessageFor(source, "standard." + (boolean) value);
        } else {
            result = Text.of(value);
        }

        tb.append(result);
        if (key.getDescription() != null && !key.getDescription().isEmpty()) {
            tb.onHover(TextActions.showText(
                    key instanceof PreferenceKeyImpl ?
                    getMessageFor(source, ((PreferenceKeyImpl<?>) key).getDescriptionKey()) :
                    Text.of(key.getDescription())));
        }
        return tb.build();
    }

}
