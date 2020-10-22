/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.playername;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Constants;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatTemplateConfig;
import io.github.nucleuspowered.nucleus.modules.chat.services.ChatService;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerDisplayNameService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.ITextStyleService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextElement;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayerDisplayNameService implements IPlayerDisplayNameService, IReloadableService.Reloadable {

    private final LinkedHashSet<DisplayNameResolver> resolvers = new LinkedHashSet<>();
    private final LinkedHashSet<DisplayNameQuery> queries = new LinkedHashSet<>();

    private final IMessageProviderService messageProviderService;
    private final IPermissionService permissionService;
    private final INucleusServiceCollection serviceCollection;
    private final ITextStyleService textStyleService;

    private String commandNameOnClick = null;

    @Inject
    public PlayerDisplayNameService(INucleusServiceCollection serviceCollection) {
        this.messageProviderService = serviceCollection.messageProvider();
        this.permissionService = serviceCollection.permissionService();
        this.textStyleService = serviceCollection.textStyleService();
        this.serviceCollection = serviceCollection;
    }

    @Override
    public void provideDisplayNameResolver(DisplayNameResolver resolver) {
        this.resolvers.add(resolver);
    }

    @Override
    public void provideDisplayNameQuery(DisplayNameQuery resolver) {
        this.queries.add(resolver);
    }

    @Override
    public Optional<User> getUser(String displayName) {
        Optional<User> withRealName = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(displayName);
        if (withRealName.isPresent()) {
            return withRealName;
        }

        for (DisplayNameQuery query : this.queries) {
            Optional<User> user = query.resolve(displayName);
            if (user.isPresent()) {
                return user;
            }
        }

        return Optional.empty();
    }

    @Override public Map<UUID, List<String>> startsWith(String displayName) {
        Map<UUID, List<String>> uuids = new HashMap<>();
        Sponge.getServer().getOnlinePlayers().stream()
            .filter(x -> x.getName().toLowerCase().startsWith(displayName.toLowerCase()))
            .forEach(x -> uuids.put(x.getUniqueId(), Lists.newArrayList(x.getName())));

        for (DisplayNameQuery query : this.queries) {
            query.startsWith(displayName).forEach(
                    (uuid, name) -> uuids.computeIfAbsent(uuid, x -> new ArrayList<>()).add(name)
            );
        }

        return uuids;
    }

    @Override
    public Optional<User> getUser(Text displayName) {
        return this.getUser(displayName.toPlain());
    }

    @Override
    public Text getDisplayName(final UUID playerUUID) {
        final Text.Builder builder;
        if (playerUUID == Util.CONSOLE_FAKE_UUID) {
            return this.getName(Sponge.getServer().getConsole());
        }
        User user = Sponge.getServiceManager()
                    .provideUnchecked(UserStorageService.class)
                    .get(playerUUID)
                    .orElseThrow(() -> new IllegalArgumentException("UUID does not map to a player"));
        Text userName = null;
        for (DisplayNameResolver resolver : this.resolvers) {
            Optional<Text> optionalUserName = resolver.resolve(playerUUID);
            if (optionalUserName.isPresent()) {
                userName = optionalUserName.get();
                break;
            }
        }

        if (userName == null) {
            builder = Text.builder(user.getName());
        } else {
            builder = Text.builder().append(userName);
        }

        // Set name colours
        this.addCommandToNameInternal(builder, user);
        this.applyStyle(user, builder);
        return builder.build();
    }

    @Override
    public Text getDisplayName(CommandSource source) {
        if (source instanceof User) {
            return this.getDisplayName(((User) source).getUniqueId());
        }

        return this.getName(source);
    }

    @Override
    public Text getName(CommandSource user) {
        final Text.Builder builder = Text.builder(user.getName());
        this.applyStyle(user, builder);
        if (user instanceof User) {
            this.addCommandToNameInternal(builder, (User) user);
        }

        return builder.build();
    }

    private void applyStyle(final Subject subject, final Text.Builder builder) {
        builder.color(getStyle(subject, this.textStyleService::getColourFromString, ChatTemplateConfig::getChatcolour, TextColors.NONE,
                Constants.NAMECOLOUR, Constants.NAMECOLOR))
                .style(getStyle(subject, this.textStyleService::getTextStyleFromString, ChatTemplateConfig::getChatstyle, TextStyles.NONE,
                        Constants.NAMESTYLE));
    }

    @Override public Text addCommandToName(CommandSource p) {
        Text.Builder text = Text.builder(p.getName());
        if (p instanceof User) {
            addCommandToNameInternal(text, (User) p);
        }

        return text.build();
    }

    @Override public Text addCommandToDisplayName(CommandSource p) {
        Text.Builder name = getName(p).toBuilder();
        if (p instanceof User) {
            addCommandToNameInternal(name, (User)p);
        }

        return name.build();
    }

    private void addCommandToNameInternal(Text.Builder name, User user) {
        if (this.commandNameOnClick == null) {
            name.onHover(TextActions.showText(this.messageProviderService.getMessage("name.hover.ign", user.getName()))).build();
            return;
        }

        final String commandToRun = this.commandNameOnClick.replace("{{subject}}", user.getName()).replace("{{player}}", user.getName());
        Text.Builder hoverAction =
                Text.builder()
                    .append(this.messageProviderService.getMessage("name.hover.ign", user.getName()))
                    .append(Text.NEW_LINE)
                    .append(this.messageProviderService.getMessage("name.hover.command", commandToRun));
        name.onClick(TextActions.suggestCommand(commandToRun)).onHover(TextActions.showText(hoverAction.toText())).build();
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.commandNameOnClick = serviceCollection.moduleDataProvider().getModuleConfig(CoreConfig.class).getCommandOnNameClick();
        if (this.commandNameOnClick == null || this.commandNameOnClick.isEmpty()) {
            return;
        }

        if (!this.commandNameOnClick.startsWith("/")) {
            this.commandNameOnClick = "/" + this.commandNameOnClick;
        }

        if (!this.commandNameOnClick.endsWith(" ")) {
            this.commandNameOnClick = this.commandNameOnClick + " ";
        }
    }

    private <T extends TextElement> T getStyle(Subject player,
            Function<String, T> returnIfAvailable,
            Function<ChatTemplateConfig, String> fromTemplate,
            T def,
            String... options) {
        Optional<String> os = this.permissionService.getOptionFromSubject(player, options);
        if (os.isPresent()) {
            return returnIfAvailable.apply(os.get());
        }

        return this.serviceCollection.getService(ChatService.class)
                .map(templateUtil -> returnIfAvailable.apply(fromTemplate.apply(templateUtil.getTemplateNow(player))))
                .orElse(def);

    }

}
