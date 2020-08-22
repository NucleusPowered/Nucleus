/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname.commands;

import io.github.nucleuspowered.nucleus.api.module.nickname.exception.NicknameException;
import io.github.nucleuspowered.nucleus.modules.nickname.NicknamePermissions;
import io.github.nucleuspowered.nucleus.modules.nickname.services.NicknameService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = {"nick", "nickname"},
        basePermission = NicknamePermissions.BASE_NICK,
        commandDescriptionKey = "nick",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = NicknamePermissions.EXEMPT_COOLDOWN_NICK),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = NicknamePermissions.EXEMPT_WARMUP_NICK),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = NicknamePermissions.EXEMPT_COST_NICK)
        },
        associatedPermissions = {
                NicknamePermissions.OTHERS_NICK,
                NicknamePermissions.NICKNAME_COLOUR,
                NicknamePermissions.NICKNAME_STYLE
        }
)
@EssentialsEquivalent(value = {"nick", "nickname"}, isExact = false,
        notes = "To remove a nickname, use '/delnick'")
public class NicknameCommand implements ICommandExecutor<CommandSource> {

    private final String nickName = "nickname";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier()
                    .createOtherUserPermissionElement(false, NicknamePermissions.OTHERS_NICK),
                GenericArguments.onlyOne(GenericArguments.string(Text.of(this.nickName)))};
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User pl = context.getUserFromArgs();
        Text name = TextSerializers.FORMATTING_CODE.deserialize(context.requireOne(this.nickName, String.class));

        try {
            context.getServiceCollection().getServiceUnchecked(NicknameService.class).setNick(pl, context.getCommandSource(), name, false);
        } catch (NicknameException e) {
            return context.errorResultLiteral(e.getTextMessage());
        }

        if (!context.is(pl)) {
            context.sendMessageText(
                    Text.builder().append(context.getMessage("command.nick.success.other", pl.getName())).append(Text.of(" - ", TextColors.RESET, name)).build());
        }

        return context.successResult();
    }

}
