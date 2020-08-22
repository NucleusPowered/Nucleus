/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.messageprovider;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.nucleuspowered.nucleus.api.core.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.guice.ConfigDirectory;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.messageprovider.repository.ConfigFileMessagesRepository;
import io.github.nucleuspowered.nucleus.services.impl.messageprovider.repository.IMessageRepository;
import io.github.nucleuspowered.nucleus.services.impl.messageprovider.repository.PropertiesMessageRepository;
import io.github.nucleuspowered.nucleus.services.impl.messageprovider.repository.UTF8Control;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserPreferenceService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.translation.locale.Locales;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MessageProviderService implements IMessageProviderService, IReloadableService.Reloadable {

    private static final String LANGUAGE_KEY_PREFIX = "language.";
    private static final String MESSAGES_BUNDLE = "assets.nucleus.messages";

    private static final String MESSAGES_BUNDLE_RESOURCE_LOC = "messages_{0}.properties";

    private static final Set<Locale> KNOWN_LOCALES = ImmutableSet.<Locale>builder()
            .add(Locale.UK)
            .add(Locale.FRANCE)
            .add(Locale.GERMANY)
            .add(Locales.ES_ES)
            .add(Locale.ITALY)
            .add(Locales.PT_BR)
            .add(Locale.SIMPLIFIED_CHINESE)
            .add(Locale.TRADITIONAL_CHINESE)
            .add(Locales.RU_RU)
            .build();
    private static final Map<String, Locale> LOCALES = new HashMap<>();

    private final INucleusServiceCollection serviceCollection;
    private final IUserPreferenceService userPreferenceService;

    private Locale defaultLocale = Sponge.getServer().getConsole().getLocale();
    private boolean useMessagesFile;
    private boolean useClientLocalesWhenPossible;

    private final PropertiesMessageRepository defaultMessagesResource;
    private final ConfigFileMessagesRepository configFileMessagesRepository;

    private final Map<Locale, PropertiesMessageRepository> messagesMap = new HashMap<>();
    private final LoadingCache<UUID, Locale> localeCache = Caffeine.newBuilder()
            .build(new CacheLoader<UUID, Locale>() {
                @CheckForNull
                @Override
                public Locale load(@Nonnull UUID key) {
                    NucleusUserPreferenceService.PreferenceKey<Locale> l =
                            userPreferenceService.keys().playerLocale().get();
                    return userPreferenceService.get(key, l).orElse(new Locale(""));
                }
            });

    @Inject
    public MessageProviderService(
            INucleusServiceCollection serviceCollection, @ConfigDirectory Path configPath) {
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
                () -> getPropertiesMessagesRepository(this.defaultLocale)
        );
        this.userPreferenceService = serviceCollection.userPreferenceService();
    }

    @Override
    public boolean hasKey(String key) {
        return getMessagesRepository(this.defaultLocale).hasEntry(key);
    }

    @Override
    public Locale getDefaultLocale() {
        return this.defaultLocale;
    }

    @Override
    public Optional<Locale> getLocaleFromName(String l) {
        if (LOCALES.isEmpty()) {
            // for each locale, get the language name
            for (final Locale locale : KNOWN_LOCALES) {
                for (final Locale innerLocale : KNOWN_LOCALES) {
                    String key = LANGUAGE_KEY_PREFIX + innerLocale.toString().toLowerCase(Locale.ENGLISH);
                    String name =
                            getPropertiesMessagesRepository(locale)
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
    public void invalidateLocaleCacheFor(UUID uuid) {
        this.localeCache.invalidate(uuid);
    }

    @Override
    public Locale getLocaleFor(CommandSource commandSource) {
        if (commandSource instanceof User) {
            Locale l = this.localeCache.get(((User) commandSource).getUniqueId());
            if (l != null && !l.toString().isEmpty()) {
                return l;
            }
        }

        final Locale toUse;
        if (this.useClientLocalesWhenPossible) {
            toUse = commandSource.getLocale();
        } else {
            toUse = this.defaultLocale;
        }

        return toUse;
    }

    @Override public Text getMessageFor(Locale locale, String key) {
        return getMessagesRepository(locale).getText(key);
    }

    @Override
    public Text getMessageFor(Locale locale, String key, Text... args) {
        return getMessagesRepository(locale).getText(key, args);
    }

    @Override public Text getMessageFor(Locale locale, String key, Object... replacements) {
        return getMessagesRepository(locale).getText(key, replacements);
    }

    @Override public Text getMessageFor(Locale locale, String key, String... replacements) {
        return getMessagesRepository(locale).getText(key, replacements);
    }

    @Override public String getMessageString(Locale locale, String key, String... replacements) {
        return getMessagesRepository(locale).getString(key, replacements);
    }

    @Override public String getMessageString(Locale locale, String key, Object... replacements) {
        return  getMessagesRepository(locale).getString(key, replacements);
    }

    @Override public boolean reloadMessageFile() {
        if (this.useMessagesFile) {
            this.configFileMessagesRepository.invalidateIfNecessary();
            return true;
        }

        return false;
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        CoreConfig coreConfig = serviceCollection.moduleDataProvider().getModuleConfig(CoreConfig.class);
        this.useMessagesFile = coreConfig.isCustommessages();
        this.useClientLocalesWhenPossible = coreConfig.isClientLocaleWhenPossible();
        this.defaultLocale = Locale.forLanguageTag(coreConfig.getServerLocale().replace("_", "-"));
        this.serviceCollection.logger().info(getMessageString("language.set", this.defaultLocale.toLanguageTag()));
        reloadMessageFile();
    }

    @Override public IMessageRepository getMessagesRepository(Locale locale) {
        if (this.useMessagesFile) {
            return this.configFileMessagesRepository;
        }

        return getPropertiesMessagesRepository(locale);
    }

    @Override public ConfigFileMessagesRepository getConfigFileMessageRepository() {
        return this.configFileMessagesRepository;
    }

    @Override public String getTimeString(Locale locale, Duration duration) {
        return getTimeString(locale, duration.getSeconds());
    }

    @Override public String getTimeString(Locale locale, long time) {
        time = Math.abs(time);
        long sec = time % 60;
        long min = (time / 60) % 60;
        long hour = (time / 3600) % 24;
        long day = time / 86400;

        if (time == 0) {
            return getMessageString(locale, "standard.inamoment");
        }

        StringBuilder sb = new StringBuilder();
        if (day > 0) {
            sb.append(day).append(" ");
            if (day > 1) {
                sb.append(getMessageString(locale, "standard.days"));
            } else {
                sb.append(getMessageString(locale, "standard.day"));
            }
        }

        if (hour > 0) {
            appendComma(sb);
            sb.append(hour).append(" ");
            if (hour > 1) {
                sb.append(getMessageString(locale, "standard.hours"));
            } else {
                sb.append(getMessageString(locale, "standard.hour"));
            }
        }

        if (min > 0) {
            appendComma(sb);
            sb.append(min).append(" ");
            if (min > 1) {
                sb.append(getMessageString(locale, "standard.minutes"));
            } else {
                sb.append(getMessageString(locale, "standard.minute"));
            }
        }

        if (sec > 0) {
            appendComma(sb);
            sb.append(sec).append(" ");
            if (sec > 1) {
                sb.append(getMessageString(locale, "standard.seconds"));
            } else {
                sb.append(getMessageString(locale, "standard.second"));
            }
        }

        if (sb.length() > 0) {
            return sb.toString();
        } else {
            return getMessageString(locale, "standard.unknown");
        }
    }

    @Override
    public List<String> getAllLocaleNames() {
        return ImmutableList.copyOf(LOCALES.keySet());
    }

    private void appendComma(StringBuilder stringBuilder) {
        if (stringBuilder.length() > 0) {
            stringBuilder.append(", ");
        }
    }

    private PropertiesMessageRepository getPropertiesMessagesRepository(Locale locale) {
        return this.messagesMap.computeIfAbsent(locale, key -> {
            if (Sponge.getAssetManager().getAsset(
                    this.serviceCollection.pluginContainer(),
                    MessageFormat.format(MESSAGES_BUNDLE_RESOURCE_LOC, locale.toString())).isPresent()) {
                // it exists
                return new PropertiesMessageRepository(
                        this.serviceCollection.textStyleService(),
                        this.serviceCollection.playerDisplayNameService(),
                        ResourceBundle.getBundle(MESSAGES_BUNDLE, locale, UTF8Control.INSTANCE));
            } else {
                return this.defaultMessagesResource;
            }
        });
    }

}
