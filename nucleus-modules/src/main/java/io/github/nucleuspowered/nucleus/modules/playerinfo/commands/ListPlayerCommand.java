/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.modules.afk.services.AFKHandler;
import io.github.nucleuspowered.nucleus.modules.playerinfo.PlayerInfoPermissions;
import io.github.nucleuspowered.nucleus.modules.playerinfo.config.ListConfig;
import io.github.nucleuspowered.nucleus.modules.playerinfo.config.PlayerInfoConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerOnlineService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@NonnullByDefault
@EssentialsEquivalent({"list", "who", "playerlist", "online", "plist"})
@Command(
        async = true,
        aliases = {"list", "listplayers", "ls"},
        basePermission = PlayerInfoPermissions.BASE_LIST,
        commandDescriptionKey = "list",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = PlayerInfoPermissions.EXEMPT_COOLDOWN_LIST),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = PlayerInfoPermissions.EXEMPT_WARMUP_LIST),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = PlayerInfoPermissions.EXEMPT_COST_LIST)
        },
        associatedPermissions = PlayerInfoPermissions.LIST_SEEVANISHED
)
public class ListPlayerCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    public static final String LIST_OPTION = "nucleus.list.group";

    private ListConfig listConfig = new ListConfig();

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        boolean showVanished = context.testPermission(PlayerInfoPermissions.LIST_SEEVANISHED);

        Collection<Player> players = Sponge.getServer().getOnlinePlayers();
        long playerCount = players.size();
        long hiddenCount = players.stream().filter(x -> x.get(Keys.VANISH).orElse(false)).count();

        Text header;
        if (showVanished && hiddenCount > 0) {
            header = context.getMessage("command.list.playercount.hidden", String.valueOf(playerCount),
                    String.valueOf(Sponge.getServer().getMaxPlayers()), String.valueOf(hiddenCount));
        } else {
            header = context.getMessage("command.list.playercount.base", String.valueOf(playerCount - hiddenCount),
                    String.valueOf(Sponge.getServer().getMaxPlayers()));
        }

        PaginationList.Builder builder = Util.getPaginationBuilder(context.getCommandSource()).title(header);

        Optional<PermissionService> optPermissionService = Sponge.getServiceManager().provide(PermissionService.class);
        if (this.listConfig.isGroupByPermissionGroup() && optPermissionService.isPresent()) {
            builder.contents(listByPermissionGroup(context, showVanished));
        } else {
            // If we have players, send them on.
            builder.contents(getPlayerList(players, showVanished, context));
        }

        builder.sendTo(context.getCommandSource());
        return context.successResult();
    }

    private List<Text> listByPermissionGroup(ICommandContext<? extends CommandSource> context, boolean showVanished) {
        // Messages
        final List<Text> messages = Lists.newArrayList();

        String defName = this.listConfig.getDefaultGroupName();
        final Map<String, List<Player>> groupToPlayer = playerList(
                context,
                showVanished,
                defName
        );

        this.listConfig.getOrder().forEach(alias -> {
            List<Player> plList = groupToPlayer.get(alias);
            if (plList != null && !plList.isEmpty()) {
                // Get and put the player list into the map, if there is a
                // player to show. There might not be, they might be vanished!
                getList(plList, showVanished, messages, alias, context);
            }

            groupToPlayer.remove(alias);
        });

        groupToPlayer.entrySet().stream()
                .filter(x -> !x.getValue().isEmpty())
                .filter(x -> !x.getKey().equals(defName))
                .sorted((x, y) -> x.getKey().compareToIgnoreCase(y.getKey()))
                .forEach(x -> getList(x.getValue(), showVanished, messages, x.getKey(), context));

        List<Player> pl = groupToPlayer.get(defName);
        if (pl != null && !pl.isEmpty()) {
            getList(pl, showVanished, messages, defName, context);
        }

        return messages;
    }

    private Map<String, List<Player>> playerList(
            ICommandContext<? extends CommandSource> context,
            boolean showVanished,
            String def) {
        IPlayerOnlineService playerOnlineService = context.getServiceCollection().playerOnlineService();
        IPermissionService permissionService = context.getServiceCollection().permissionService();
        Map<String, List<Player>> map = new HashMap<>();
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            if (showVanished || playerOnlineService.isOnline(context.getCommandSourceUnchecked(), player)) {
                String perm = permissionService.getOptionFromSubject(player, LIST_OPTION).orElse(def);
                if (perm.trim().isEmpty()) {
                    perm = def;
                }
                map.computeIfAbsent(perm, y -> new ArrayList<>()).add(player);
            }
        }
        return map;
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.listConfig = serviceCollection.moduleDataProvider().getModuleConfig(PlayerInfoConfig.class).getList();
    }

    private void getList(Collection<Player> player, boolean showVanished, List<Text> messages, String groupName,
            ICommandContext<? extends CommandSource> context) {
        Text groupNameText = TextSerializers.FORMATTING_CODE.deserialize(groupName);
        List<Text> m = getPlayerList(player, showVanished, context);
        if (this.listConfig.isCompact()) {
            boolean isFirst = true;
            for (Text y : m) {
                Text.Builder tb = Text.builder();
                if (isFirst) {
                    tb.append(Text.of(TextColors.YELLOW, groupNameText, ": "));
                }
                isFirst = false;
                messages.add(tb.append(y).build());
            }
        } else {
            messages.add(Text.of(TextColors.YELLOW, groupNameText, ":"));
            messages.addAll(m);
        }
    }

    /**
     * Gets {@link Text} that represents the provided player list.
     *
     * @param playersToList The {@link Player}s to list.
     * @param showVanished <code>true</code> if those who are vanished are to be
     *        shown.
     * @return An {@link Optional} of {@link Text} objects, returning
     *         <code>empty</code> if the player list is of zero length.
     */
    @SuppressWarnings("ConstantConditions")
    private List<Text> getPlayerList(Collection<Player> playersToList, boolean showVanished, ICommandContext<? extends CommandSource> context) {
        NucleusTextTemplate template = this.listConfig.getListTemplate();
        final AFKHandler handler = context.getServiceCollection().getService(AFKHandler.class).orElse(null);
        final Text afk = context.getMessage("command.list.afk");
        final Text hidden = context.getMessage("command.list.hidden");

        List<Text> playerList = playersToList.stream().filter(x -> showVanished || !x.get(Keys.VANISH).orElse(false))
                .sorted((x, y) -> x.getName().compareToIgnoreCase(y.getName())).map(x -> {
                    Text.Builder tb = Text.builder();
                    boolean appendSpace = false;
                    if (handler != null && handler.isAFK(x)) {
                        tb.append(afk);
                        appendSpace = true;
                    }

                    if (x.get(Keys.VANISH).orElse(false)) {
                        tb.append(hidden);
                        appendSpace = true;
                    }

                    if (appendSpace) {
                        tb.append(Text.of(" "));
                    }

                    if (template != null) { // it shouldn't be, but if it is, fallback...
                        return tb.append(template.getForCommandSource(x)).build();
                    } else {
                        return tb.append(context.getDisplayName(x.getUniqueId())).build();
                    }
                }).collect(Collectors.toList());

        if (this.listConfig.isCompact() && !playerList.isEmpty()) {
            List<Text> toReturn = new ArrayList<>();
            List<List<Text>> parts = Lists.partition(playerList, this.listConfig.getMaxPlayersPerLine());
            for (List<Text> p : parts) {
                Text.Builder tb = Text.builder();
                boolean isFirst = true;
                for (Text text : p) {
                    if (!isFirst) {
                        tb.append(Text.of(TextColors.WHITE, ", "));
                    }

                    tb.append(text);
                    isFirst = false;
                }

                toReturn.add(tb.build());
            }

            return toReturn;
        }

        return playerList;
    }
}
