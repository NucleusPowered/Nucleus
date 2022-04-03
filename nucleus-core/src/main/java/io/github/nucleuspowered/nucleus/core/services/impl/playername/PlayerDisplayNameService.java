/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.playername;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.core.Constants;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.core.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlayerDisplayNameService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITextStyleService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Nameable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Singleton
public class PlayerDisplayNameService implements IPlayerDisplayNameService, IReloadableService.Reloadable {

    private final LinkedHashSet<DisplayNameResolver> resolvers = new LinkedHashSet<>();
    private final LinkedHashSet<DisplayNameQuery> queries = new LinkedHashSet<>();

    private final IMessageProviderService messageProviderService;
    private final IPermissionService permissionService;
    private final ITextStyleService textStyleService;

    private String commandNameOnClick = null;

    private Function<Subject, String> colourFromTemplateSupplier = subject -> "";
    private Function<Subject, String> styleFromTemplateSupplier = subject -> "";

    @Inject
    public PlayerDisplayNameService(final INucleusServiceCollection serviceCollection) {
        this.messageProviderService = serviceCollection.messageProvider();
        this.permissionService = serviceCollection.permissionService();
        this.textStyleService = serviceCollection.textStyleService();
    }

    @Override
    public void supplyColourFromTemplateSupplier(final Function<Subject, String> colourFromTemplateSupplier) {
        this.colourFromTemplateSupplier = colourFromTemplateSupplier;
    }

    @Override
    public void supplyStyleFromTemplateSupplier(final Function<Subject, String> styleFromTemplateSupplier) {
        this.styleFromTemplateSupplier = styleFromTemplateSupplier;
    }

    @Override
    public void provideDisplayNameResolver(final DisplayNameResolver resolver) {
        this.resolvers.add(resolver);
    }

    @Override
    public void provideDisplayNameQuery(final DisplayNameQuery resolver) {
        this.queries.add(resolver);
    }

    @Override
    public Optional<User> getUser(final String displayName) {
        final Optional<User> withRealName = Sponge.server().userManager().load(displayName).join();
        if (withRealName.isPresent()) {
            return withRealName;
        }

        for (final DisplayNameQuery query : this.queries) {
            final Optional<User> user = query.resolve(displayName);
            if (user.isPresent()) {
                return user;
            }
        }

        return Optional.empty();
    }

    @Override
    public Map<UUID, List<String>> startsWith(final String displayName) {
        final Map<UUID, List<String>> uuids = new HashMap<>();
        Sponge.server().onlinePlayers().stream()
            .filter(x -> x.name().toLowerCase().startsWith(displayName.toLowerCase()))
            .forEach(x -> {
                final List<String> names = new ArrayList<>();
                names.add(x.name());
                uuids.put(x.uniqueId(), names);
            });

        for (final DisplayNameQuery query : this.queries) {
            query.startsWith(displayName).forEach(
                    (uuid, name) -> uuids.computeIfAbsent(uuid, x -> new ArrayList<>()).add(name)
            );
        }

        return uuids;
    }

    @Override
    public Optional<User> getUser(final Component displayName) {
        return this.getUser(displayName.toString());
    }

    @Override
    public Component getDisplayName(final UUID playerUUID) {
        final TextComponent.Builder builder;
        if (playerUUID.equals(Util.CONSOLE_FAKE_UUID)) {
            return this.getName(Sponge.systemSubject());
        }
       final User user = Sponge.server()
                .userManager()
                .load(playerUUID)
               .join()
                .orElseThrow(() -> new IllegalArgumentException("UUID does not map to a player"));
        Component userName = null;
        for (final DisplayNameResolver resolver : this.resolvers) {
            final Optional<Component> optionalUserName = resolver.resolve(playerUUID);
            if (optionalUserName.isPresent()) {
                userName = optionalUserName.get();
                break;
            }
        }

        if (userName == null) {
            builder = Component.text().content(user.name());
        } else {
            builder = Component.text().append(userName);
        }

        // Set name colours
        this.addCommandToNameInternal(builder, user.name());
        this.applyStyle(user, builder);
        return builder.build();
    }

    private Optional<TextColor> getColour(final Subject player) {
        final Optional<TextColor> os = this.permissionService
                .getOptionFromSubject(player, Constants.NAMECOLOUR, Constants.NAMECOLOR)
                .flatMap(this.textStyleService::getColourFromString);
        if (!os.isPresent() && this.colourFromTemplateSupplier != null) {
            return this.textStyleService.getColourFromString(this.colourFromTemplateSupplier.apply(player));
        }
        return os;
    }

    private Style getStyle(final Subject player) {
        final Optional<Style> style = this.permissionService.getOptionFromSubject(player, Constants.NAMESTYLE)
                .map(this.textStyleService::getTextStyleFromString);
        if (!style.isPresent() && this.styleFromTemplateSupplier != null) {
            return this.textStyleService.getTextStyleFromString(this.styleFromTemplateSupplier.apply(player));
        }

        return style.orElseGet(Style::empty);
    }

    @Override
    public Component getDisplayName(final Audience source) {
        if (source instanceof SystemSubject || source instanceof Server) {
            return this.getDisplayName(Util.CONSOLE_FAKE_UUID);
        } if (source instanceof User) {
            return this.getDisplayName(((User) source).uniqueId());
        } else if (source instanceof ServerPlayer) {
            return this.getDisplayName(((ServerPlayer) source).uniqueId());
        } else if (source instanceof Nameable) {
            return Component.text(((Nameable) source).name());
        }

        return Component.text("Unknown");
    }

    @Override
    public Component getName(final Audience user) {
        if (user instanceof Nameable) {
            return this.getName((Nameable) user);
        } else if (user instanceof SystemSubject || user instanceof Server) {
            return Component.text("Server");
        }
        return Component.text("Unknown");
    }

    @Override
    public Component getName(final Nameable user) {
        final String name = user.name();
        final TextComponent.Builder builder = Component.text().content(name);
        if (user instanceof Subject) {
            this.applyStyle((Subject) user, builder);
            if (user instanceof User || user instanceof ServerPlayer) {
                this.addCommandToNameInternal(builder, name);
            }
        }


        return Component.text(name);
    }

    @Override
    public Component getName(final UUID uuid) {
        if (uuid == Util.CONSOLE_FAKE_UUID) {
            return Component.text("Server");
        }
        return Component.text(Sponge.server().userManager().load(uuid)
                        .join()
                        .map(User::name).orElse("unknown"));
    }

    @Override
    public Component addCommandToName(final Nameable p) {
        final TextComponent.Builder text = Component.text().content(p.name());
        if (p instanceof User || p instanceof ServerPlayer) {
            this.addCommandToNameInternal(text, p.name());
        }

        return text.build();
    }

    private void applyStyle(final Subject subject, final TextComponent.Builder builder) {
        final TextColor textColour = this.getColour(subject).orElse(null);
        final Style style = this.getStyle(subject);
        builder.color(textColour).style(style);
    }

    @Override
    public Component addCommandToDisplayName(final Nameable p) {
        final TextComponent.Builder name = Component.text().append(this.getName(p));
        if (p instanceof User || p instanceof ServerPlayer) {
            this.addCommandToNameInternal(name, p.name());
        }

        return name.build();
    }

    @Override
    public Component getName(final Object cs, final Component defaultName) {
        if (cs instanceof Nameable) {
            return this.getName((Nameable) cs);
        } else if (cs instanceof Audience) {
            return this.getName((Audience) cs);
        }
        return defaultName;
    }

    private void addCommandToNameInternal(final TextComponent.Builder name, final String user) {
        if (this.commandNameOnClick == null) {
            name.hoverEvent(HoverEvent.showText(this.messageProviderService.getMessage("name.hover.ign", user))).build();
            return;
        }

        final String commandToRun = this.commandNameOnClick.replace("{{subject}}", user).replace("{{player}}", user);
        final TextComponent.Builder hoverAction =
                Component.text().append(
                        this.messageProviderService.getMessage("name.hover.ign", user),
                        Component.newline(),
                        this.messageProviderService.getMessage("name.hover.command", commandToRun));
        name.clickEvent(ClickEvent.suggestCommand(commandToRun)).hoverEvent(HoverEvent.showText(hoverAction.build())).build();
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.commandNameOnClick = serviceCollection.configProvider().getModuleConfig(CoreConfig.class).getCommandOnNameClick();
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

}
