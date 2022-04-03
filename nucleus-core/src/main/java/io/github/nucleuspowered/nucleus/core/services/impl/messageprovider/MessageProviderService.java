/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.messageprovider;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.api.core.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.core.guice.ConfigDirectory;
import io.github.nucleuspowered.nucleus.core.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.messageprovider.repository.ConfigFileMessagesRepository;
import io.github.nucleuspowered.nucleus.core.services.impl.messageprovider.repository.IMessageRepository;
import io.github.nucleuspowered.nucleus.core.services.impl.messageprovider.repository.PropertiesMessageRepository;
import io.github.nucleuspowered.nucleus.core.services.impl.messageprovider.repository.UTF8Control;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserPreferenceService;
import io.vavr.control.Option;
import io.vavr.control.Try;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.resource.Resource;
import org.spongepowered.api.resource.ResourcePath;
import org.spongepowered.api.resource.pack.Pack;
import org.spongepowered.api.resource.pack.PackType;
import org.spongepowered.api.util.locale.LocaleSource;
import org.spongepowered.api.util.locale.Locales;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Singleton
public class MessageProviderService implements IMessageProviderService, IReloadableService.Reloadable {

    private static final String LANGUAGE_KEY_PREFIX = "language.";
    private static final String MESSAGES_BUNDLE = "data.plugin-nucleus.messages";
    private static final String MESSAGES_BUNDLE_PATH = "/" + MessageProviderService.MESSAGES_BUNDLE.replace('.', '/');

    private static final String MESSAGES_BUNDLE_RESOURCE_LOC = "messages_{0}.properties";
    private static final String MESSAGES_BUNDLE_RESOURCE_PATH_AND_LOC =
            MessageProviderService.MESSAGES_BUNDLE_PATH + "/" + MessageProviderService.MESSAGES_BUNDLE_RESOURCE_LOC;

    private static final Set<Locale> KNOWN_LOCALES;
    private static final Map<String, Locale> LOCALES = new HashMap<>();

    static {
        final Set<Locale> locales = new HashSet<>();
        locales.add(Locale.UK);
        locales.add(Locale.FRANCE);
        locales.add(Locale.GERMANY);
        locales.add(Locales.ES_ES);
        locales.add(Locale.ITALY);
        locales.add(Locales.PT_BR);
        locales.add(Locale.SIMPLIFIED_CHINESE);
        locales.add(Locale.TRADITIONAL_CHINESE);
        locales.add(Locales.RU_RU);
        KNOWN_LOCALES = locales;
    }

    private final INucleusServiceCollection serviceCollection;
    private final IUserPreferenceService userPreferenceService;

    private Locale defaultLocale = Locale.getDefault();
    private boolean useMessagesFile;
    private boolean useClientLocalesWhenPossible;

    private final PropertiesMessageRepository defaultMessagesResource;
    private final ConfigFileMessagesRepository configFileMessagesRepository;

    private final Map<Locale, PropertiesMessageRepository> messagesMap = new HashMap<>();
    private final LoadingCache<UUID, Locale> localeCache = Caffeine.newBuilder()
            .build(new CacheLoader<UUID, Locale>() {
                @Override
                public Locale load(@NonNull final UUID key) {
                    final NucleusUserPreferenceService.PreferenceKey<Locale> l =
                            MessageProviderService.this.userPreferenceService.keys().playerLocale().get();
                    return MessageProviderService.this.userPreferenceService.get(key, l).orElse(new Locale(""));
                }
            });

    @Inject
    public MessageProviderService(
            final INucleusServiceCollection serviceCollection, @ConfigDirectory final Path configPath) {
        this.serviceCollection = serviceCollection;
        serviceCollection.reloadableService().registerReloadable(this);
        this.defaultMessagesResource = new PropertiesMessageRepository(
                serviceCollection.textStyleService(),
                serviceCollection.playerDisplayNameService(),
                ResourceBundle.getBundle(MESSAGES_BUNDLE, Locale.ROOT, UTF8Control.INSTANCE));
        this.configFileMessagesRepository = new ConfigFileMessagesRepository(
                serviceCollection.textStyleService(),
                serviceCollection.playerDisplayNameService(),
                serviceCollection.logger(),
                configPath.resolve("messages.conf"),
                () -> this.getPropertiesMessagesRepository(this.defaultLocale)
        );
        this.userPreferenceService = serviceCollection.userPreferenceService();
    }

    @Override
    public boolean hasKey(final String key) {
        return this.getMessagesRepository(this.defaultLocale).hasEntry(key);
    }

    @Override
    public Locale getDefaultLocale() {
        return this.defaultLocale;
    }

    @Override
    public Optional<Locale> getLocaleFromName(final String l) {
        if (LOCALES.isEmpty()) {
            // for each locale, get the language name
            for (final Locale locale : KNOWN_LOCALES) {
                for (final Locale innerLocale : KNOWN_LOCALES) {
                    final String key = LANGUAGE_KEY_PREFIX + innerLocale.toString().toLowerCase(Locale.ENGLISH);
                    final String name =
                            this.getPropertiesMessagesRepository(locale)
                                    .getString(key)
                                    .toLowerCase(Locale.ENGLISH);
                    if (!LOCALES.containsKey(name)) {
                        LOCALES.put(name, innerLocale);
                    }
                }
                LOCALES.put(locale.toString().toLowerCase(Locale.ENGLISH), locale);
            }
        }

        return Optional.ofNullable(LOCALES.get(l.toLowerCase(Locale.ENGLISH)));
    }

    @Override
    public void invalidateLocaleCacheFor(final UUID uuid) {
        this.localeCache.invalidate(uuid);
    }

    @Override
    public Locale getLocaleFor(final Audience commandSource) {
        final Locale toUse;
        if (this.useClientLocalesWhenPossible && commandSource instanceof LocaleSource) {
            if (commandSource instanceof ServerPlayer) {
                return this.userPreferenceService.getPreferenceFor(((ServerPlayer) commandSource).uniqueId(),
                        this.userPreferenceService.keys().playerLocale().get()).orElseGet(((ServerPlayer) commandSource)::locale);
            }
            toUse = ((LocaleSource) commandSource).locale();
        } else {
            toUse = this.defaultLocale;
        }

        return toUse;
    }

    @Override
    public Component getMessageFor(final Locale locale, final String key) {
        return this.getMessagesRepository(locale).getText(key);
    }

    @Override
    public Component getMessageFor(final Locale locale, final String key, final Component... args) {
        return this.getMessagesRepository(locale).getText(key, args);
    }

    @Override
    public Component getMessageFor(final Locale locale, final String key, final Object... replacements) {
        return this.getMessagesRepository(locale).getText(key, replacements);
    }

    @Override
    public Component getMessageFor(final Locale locale, final String key, final String... replacements) {
        return this.getMessagesRepository(locale).getText(key, replacements);
    }

    @Override
    public String getMessageString(final Locale locale, final String key, final String... replacements) {
        return this.getMessagesRepository(locale).getString(key, replacements);
    }

    @Override
    public String getMessageString(final Locale locale, final String key, final Object... replacements) {
        return this.getMessagesRepository(locale).getString(key, replacements);
    }

    @Override
    public boolean reloadMessageFile() {
        if (this.useMessagesFile) {
            this.configFileMessagesRepository.invalidateIfNecessary();
            return true;
        }

        return false;
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        final CoreConfig coreConfig = serviceCollection.configProvider().getModuleConfig(CoreConfig.class);
        this.useMessagesFile = coreConfig.isCustommessages();
        this.useClientLocalesWhenPossible = coreConfig.isClientLocaleWhenPossible();
        this.defaultLocale = Locale.forLanguageTag(coreConfig.getServerLocale().replace("_", "-"));
        this.serviceCollection.logger().info(this.getMessageString("language.set", this.defaultLocale.toLanguageTag()));
        this.reloadMessageFile();
    }

    @Override
    public IMessageRepository getMessagesRepository(final Locale locale) {
        if (this.useMessagesFile) {
            return this.configFileMessagesRepository;
        }

        return this.getPropertiesMessagesRepository(locale);
    }

    @Override public ConfigFileMessagesRepository getConfigFileMessageRepository() {
        return this.configFileMessagesRepository;
    }

    @Override public String getTimeString(final Locale locale, final Duration duration) {
        return this.getTimeString(locale, duration.getSeconds());
    }

    @Override public String getTimeString(final Locale locale, long time) {
        time = Math.abs(time);
        final long sec = time % 60;
        final long min = (time / 60) % 60;
        final long hour = (time / 3600) % 24;
        final long day = time / 86400;

        if (time == 0) {
            return this.getMessageString(locale, "standard.inamoment");
        }

        final StringBuilder sb = new StringBuilder();
        if (day > 0) {
            sb.append(day).append(" ");
            if (day > 1) {
                sb.append(this.getMessageString(locale, "standard.days"));
            } else {
                sb.append(this.getMessageString(locale, "standard.day"));
            }
        }

        if (hour > 0) {
            this.appendComma(sb);
            sb.append(hour).append(" ");
            if (hour > 1) {
                sb.append(this.getMessageString(locale, "standard.hours"));
            } else {
                sb.append(this.getMessageString(locale, "standard.hour"));
            }
        }

        if (min > 0) {
            this.appendComma(sb);
            sb.append(min).append(" ");
            if (min > 1) {
                sb.append(this.getMessageString(locale, "standard.minutes"));
            } else {
                sb.append(this.getMessageString(locale, "standard.minute"));
            }
        }

        if (sec > 0) {
            this.appendComma(sb);
            sb.append(sec).append(" ");
            if (sec > 1) {
                sb.append(this.getMessageString(locale, "standard.seconds"));
            } else {
                sb.append(this.getMessageString(locale, "standard.second"));
            }
        }

        if (sb.length() > 0) {
            return sb.toString();
        } else {
            return this.getMessageString(locale, "standard.unknown");
        }
    }

    @Override
    public Collection<String> getAllLocaleNames() {
        return Collections.unmodifiableCollection(LOCALES.keySet());
    }

    private void appendComma(final StringBuilder stringBuilder) {
        if (stringBuilder.length() > 0) {
            stringBuilder.append(", ");
        }
    }

    private PropertiesMessageRepository getPropertiesMessagesRepository(final Locale locale) {
        return this.messagesMap.computeIfAbsent(locale, key -> {
            final InputStream toClose = this.getClass().getResourceAsStream(
                    MessageFormat.format(MessageProviderService.MESSAGES_BUNDLE_RESOURCE_PATH_AND_LOC, locale.toString()));
            final boolean found = toClose != null;
            if (found) {
                try {
                    toClose.close();
                } catch (final IOException ignored) {
                }
                return new PropertiesMessageRepository(
                        this.serviceCollection.textStyleService(),
                        this.serviceCollection.playerDisplayNameService(),
                        ResourceBundle.getBundle(MESSAGES_BUNDLE, locale, UTF8Control.INSTANCE));
            }

            return this.defaultMessagesResource;
        });
    }

}
