/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.messageprovider.repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigException;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerDisplayNameService;
import io.github.nucleuspowered.nucleus.services.interfaces.ITextStyleService;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

public class ConfigFileMessagesRepository extends AbstractMessageRepository implements IMessageRepository {

    private static final Pattern KEYS = Pattern.compile("\\{(\\d+)}");

    private boolean isFailed = false;
    private final Path file;
    private final Logger logger;
    private final Supplier<PropertiesMessageRepository> messageRepositorySupplier;
    private final Map<String, String> entryCache = new HashMap<>();
    private CommentedConfigurationNode node = CommentedConfigurationNode.root();

    public ConfigFileMessagesRepository(
            ITextStyleService textStyleService,
            IPlayerDisplayNameService playerDisplayNameService,
            Logger logger,
            Path file,
            Supplier<PropertiesMessageRepository> messageRepositorySupplier) {
        super(textStyleService, playerDisplayNameService);
        this.file = file;
        this.messageRepositorySupplier = messageRepositorySupplier;
        this.logger = logger;
    }

    public boolean isFailed() {
        return this.isFailed;
    }

    @Override
    public void invalidateIfNecessary() {
        invalidateIfNecessary(false);
    }

    public void invalidateIfNecessary(boolean firstLoad) {
        this.cachedMessages.clear();
        this.cachedStringMessages.clear();
        this.load(firstLoad);
    }

    @Override
    public boolean hasEntry(String key) {
        return this.entryCache.containsKey(key) || !this.node.getNode((Object[]) key.split("\\.")).isVirtual();
    }

    @Override
    String getEntry(String key) {
        if (!this.entryCache.containsKey(key)) {
            this.entryCache.put(key, this.node.getNode((Object[]) key.split("\\.")).getString());
        }
        return this.entryCache.get(key);
    }

    protected CommentedConfigurationNode getDefaults() {
        CommentedConfigurationNode ccn = CommentedConfigurationNode.root();
        PropertiesMessageRepository repository = this.messageRepositorySupplier.get();

        repository.getKeys()
                .forEach(x ->
                        ccn.getNode((Object[])x.split("\\.")).setValue(repository.getEntry(x)));

        return ccn;
    }

    protected HoconConfigurationLoader getLoader(Path file) {
        return HoconConfigurationLoader.builder().setPath(file).build();
    }

    public Optional<String> getKey(@Nonnull String key) {
        Preconditions.checkNotNull(key);
        Object[] obj = key.split("\\.");
        return Optional.ofNullable(this.node.getNode(obj).getString());
    }

    public List<String> walkThroughForMismatched() {
        Matcher keyMatcher = KEYS.matcher("");
        final List<String> keysToFix = Lists.newArrayList();
        PropertiesMessageRepository propertiesMessageRepository = this.messageRepositorySupplier.get();
        propertiesMessageRepository.getKeys().forEach(x -> {
            String resKey = propertiesMessageRepository.getEntry(x);
            Optional<String> msgKey = getKey(x);
            if (msgKey.isPresent() && getTokens(resKey, keyMatcher) != getTokens(msgKey.get(), keyMatcher)) {
                keysToFix.add(x);
            }
        });

        return keysToFix;
    }

    public void fixMismatched(List<String> toFix) {
        Preconditions.checkNotNull(toFix);
        final PropertiesMessageRepository propertiesMessageRepository = this.messageRepositorySupplier.get();
        toFix.forEach(x -> {
            String resKey = propertiesMessageRepository.getEntry(x);
            Optional<String> msgKey = getKey(x);

            Object[] nodeKey = x.split("\\.");
            CommentedConfigurationNode cn = this.node.getNode(nodeKey).setValue(resKey);
            msgKey.ifPresent(cn::setComment);
        });

        save();
    }

    private int getTokens(String message, Matcher matcher) {
        int result = -1;

        matcher.reset(message);
        while (matcher.find()) {
            result = Math.max(result, Integer.parseInt(matcher.group(1)));
        }

        return result;
    }

    private void load(boolean firstLoad) {
        try {
            this.node = this.getLoader(this.file).load();
            this.entryCache.clear();
            this.isFailed = false;
        } catch (IOException e) {
            this.isFailed = true;
            // On error, fallback.
            // Blegh, relocations
            if (e.getCause().getClass().getName().contains(ConfigException.class.getSimpleName())) {
                Throwable exception = e.getCause();
                this.logger.error("It appears that there is an error in your messages file! The error is: ");
                this.logger.error(exception.getMessage());
                this.logger.error("Please correct this then run /nucleus reload");
                this.logger.error("Ignoring messages.conf for now.");
                exception.printStackTrace();
            } else {
                this.logger.warn("Could not load custom messages file. Falling back.");
                e.printStackTrace();
            }
        }

        if (firstLoad) {
            // get defaults and merge in
            this.node.mergeValuesFrom(getDefaults());
            fixMismatched(walkThroughForMismatched());
            save();
        }
    }

    private void save() {
        try {
            getLoader(this.file).save(this.node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
