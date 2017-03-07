/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.NicknameArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.SelectorWrapperArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.UUIDArgument;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.dataservices.modular.ModularUserService;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.CommandBuilder;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.datamodules.CoreUserDataModule;
import io.github.nucleuspowered.nucleus.modules.misc.commands.SpeedCommand;
import io.github.nucleuspowered.nucleus.modules.playerinfo.handlers.SeenHandler;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportPositionCommand;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.JoinData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Permissions
@RunAsync
@RegisterCommand({"seen", "seenplayer", "lookup"})
@EssentialsEquivalent("seen")
public class SeenCommand extends AbstractCommand<CommandSource> {

    @Inject private UserDataManager udm;
    @Inject private SeenHandler seenHandler;

    private static final String EXTENDED_SUFFIX = "extended";
    public static final String EXTENDED_PERMISSION = PermissionRegistry.PERMISSIONS_PREFIX + "seen." + EXTENDED_SUFFIX;

    private final String uuid = "uuid";
    private final String playerKey = "subject";
    private final Text notEmpty = Text.of(" ");

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put(EXTENDED_SUFFIX, PermissionInformation.getWithTranslation("permission.seen.extended", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.firstParsing(
                GenericArguments.onlyOne(UUIDArgument.user(Text.of(uuid))),
                GenericArguments.onlyOne(SelectorWrapperArgument.nicknameSelector(Text.of(playerKey), NicknameArgument.UnderlyingType.USER)))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        User user = args.<User>getOne(uuid).isPresent() ? args.<User>getOne(uuid).get() : args.<User>getOne(playerKey).get();
        ModularUserService iqsu = udm.get(user).get();
        CoreUserDataModule coreUserDataModule = iqsu.get(CoreUserDataModule.class);

        List<Text> messages = new ArrayList<>();
        final MessageProvider messageProvider = plugin.getMessageProvider();

        // Everyone gets the last online time.
        if (user.isOnline()) {
            messages.add(messageProvider.getTextMessageWithFormat("command.seen.iscurrently.online", user.getName()));
            coreUserDataModule.getLastLogin().ifPresent(x -> messages.add(
                    messageProvider.getTextMessageWithFormat("command.seen.loggedon", Util.getTimeToNow(x))));
        } else {
            messages.add(messageProvider.getTextMessageWithFormat("command.seen.iscurrently.offline", user.getName()));
            coreUserDataModule.getLastLogout().ifPresent(x -> messages.add(
                    messageProvider.getTextMessageWithFormat("command.seen.loggedoff", Util.getTimeToNow(x))));
        }

        messages.add(messageProvider.getTextMessageWithFormat("command.seen.displayname", TextSerializers.FORMATTING_CODE.serialize(plugin.getNameUtil().getName(user))));

        if (permissions.testSuffix(src, EXTENDED_SUFFIX)) {
            messages.add(notEmpty);
            messages.add(messageProvider.getTextMessageWithFormat("command.seen.uuid", user.getUniqueId().toString()));

            if (user.isOnline()) {
                Player pl = user.getPlayer().get();
                messages.add(messageProvider.getTextMessageWithFormat("command.seen.ipaddress",
                        pl.getConnection().getAddress().getAddress().toString()));

                messages.add(messageProvider.getTextMessageWithFormat("command.seen.firstplayed",
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                .withLocale(src.getLocale())
                                .withZone(ZoneId.systemDefault()).format(pl.getJoinData().firstPlayed().get())));

                messages.add(messageProvider.getTextMessageWithFormat("command.seen.speed.walk",
                        String.valueOf(Math.round(pl.get(Keys.WALKING_SPEED).orElse(0.1d) * SpeedCommand.multiplier))));

                messages.add(messageProvider.getTextMessageWithFormat("command.seen.speed.fly",
                        String.valueOf(Math.round(pl.get(Keys.FLYING_SPEED).orElse(0.05d) * SpeedCommand.multiplier))));

                messages.add(getLocationString("command.seen.currentlocation", pl.getLocation(), src));

                messages.add(messageProvider.getTextMessageWithFormat("command.seen.canfly", getYesNo(pl.get(Keys.CAN_FLY).orElse(false))));
                messages.add(messageProvider.getTextMessageWithFormat("command.seen.isflying", getYesNo(pl.get(Keys.IS_FLYING).orElse(false))));
            } else {
                coreUserDataModule.getLastIp().ifPresent(x ->
                    messages.add(messageProvider.getTextMessageWithFormat("command.seen.lastipaddress", x))
                );

                Optional<Instant> i = user.get(Keys.FIRST_DATE_PLAYED);
                if (!i.isPresent()) {
                    i = coreUserDataModule.getFirstJoin();
                }

                i.ifPresent(x -> messages.add(messageProvider.getTextMessageWithFormat("command.seen.firstplayed",
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                .withLocale(src.getLocale())
                                .withZone(ZoneId.systemDefault()).format(x))));

                Optional<Location<World>> olw = coreUserDataModule.getLogoutLocation();

                olw.ifPresent(worldLocation -> messages
                    .add(getLocationString("command.seen.lastlocation", worldLocation, src)));

                user.get(JoinData.class).ifPresent(x -> {
                    Optional<Instant> oi = x.firstPlayed().getDirect();
                    oi.ifPresent(instant -> messages.add(messageProvider.getTextMessageWithFormat("command.seen.firstplayed",
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                            .withLocale(src.getLocale())
                            .withZone(ZoneId.systemDefault()).format(instant))));
                });
            }
        }

        // Add the extra module information.
        messages.addAll(seenHandler.buildInformation(src, user));

        PaginationService ps = Sponge.getServiceManager().provideUnchecked(PaginationService.class);
        ps.builder().contents(messages).padding(Text.of(TextColors.GREEN, "-"))
                .title(messageProvider.getTextMessageWithFormat("command.seen.title", user.getName())).sendTo(src);
        return CommandResult.success();
    }

    private Text getLocationString(String key, Location<World> lw, CommandSource source) {
        Text text = plugin.getMessageProvider().getTextMessageWithFormat(key,
            plugin.getMessageProvider().getMessageWithFormat("command.seen.locationtemplate", lw.getExtent().getName(), lw.getBlockPosition().toString()));
        if (CommandBuilder.isCommandRegistered(TeleportPositionCommand.class)
            && plugin.getPermissionRegistry().getPermissionsForNucleusCommand(TeleportPositionCommand.class).testBase(source)) {

            return text.toBuilder().onHover(TextActions.showText(
                plugin.getMessageProvider().getTextMessageWithFormat("command.seen.teleportposition")
            )).onClick(TextActions.executeCallback(cs -> {
                if (cs instanceof Player) {
                    ((Player) cs).setLocation(lw);
                }
            })).build();
        }

        return text;
    }

    private String getYesNo(Boolean bool) {
        if (bool == null) {
            bool = false;
        }

        return Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("standard.yesno." + bool.toString().toLowerCase());
    }
}
