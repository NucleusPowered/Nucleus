/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.usercache;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.core.configurate.datatypes.UserCacheDataNode;
import io.github.nucleuspowered.nucleus.core.configurate.datatypes.UserCacheVersionNode;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.storage.query.IUserQueryObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.queryobjects.UserQueryObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserCacheService;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.services.IStorageService;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Singleton
public class UserCacheService implements IUserCacheService, IReloadableService.DataLocationReloadable {

    private static final int expectedVersion = new UserCacheVersionNode().getVersion();
    private boolean isWalking = false;

    private final Supplier<Path> dataDirectory;
    private final Object lockingObject = new Object();
    private final IStorageManager storageManager;

    private Function<IUserDataObject, String> jailProcessor = x -> null;
    private Predicate<IUserDataObject> mutedProcessor = x -> false;

    private UserCacheVersionNode data;

    @Inject
    public UserCacheService(final INucleusServiceCollection serviceCollection) {
        this.dataDirectory = serviceCollection.dataDir();
        this.storageManager = serviceCollection.storageManager();
        serviceCollection.reloadableService().registerDataFileReloadable(this);
    }

    @Override
    public void load() {
        try {
            this.data = this.configurationLoader()
                    .load()
                    .get(TypeToken.get(UserCacheVersionNode.class), (Supplier<UserCacheVersionNode>) UserCacheVersionNode::new);
        } catch (final IOException e) {
            e.printStackTrace();
            this.data = new UserCacheVersionNode();
        }
    }

    @Override
    public void save() {
        try {
            final GsonConfigurationLoader gsonConfigurationLoader = this.configurationLoader();
            final ConfigurationNode node = gsonConfigurationLoader.createNode();
            node.set(TypeToken.get(UserCacheVersionNode.class), this.data);
            gsonConfigurationLoader.save(node);
        } catch (final IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public List<UUID> getForIp(final String ip) {
        this.updateCacheForOnlinePlayers();
        final String ipToCheck = ip.replace("/", "");
        return this.data.getNode().entrySet().stream().filter(x -> x.getValue()
                .getIpAddress().map(y -> y.equals(ipToCheck)).orElse(false))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public List<UUID> getJailed() {
        this.updateCacheForOnlinePlayers();
        return this.data.getNode().entrySet().stream().filter(x -> x.getValue().isJailed())
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public List<UUID> getJailedIn(final String name) {
        this.updateCacheForOnlinePlayers();
        return this.data.getNode().entrySet().stream()
                .filter(x -> x.getValue().getJailName().map(y -> y.equalsIgnoreCase(name)).orElse(false))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public List<UUID> getMuted() {
        this.updateCacheForOnlinePlayers();
        return this.data.getNode().entrySet().stream().filter(x -> x.getValue().isMuted())
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public void updateCacheForOnlinePlayers() {
        final IUserQueryObject iuq = new UserQueryObject();
        iuq.addAllKeys(Sponge.server().onlinePlayers().stream().map(Identifiable::uniqueId).collect(Collectors.toList()));
        this.storageManager.getUserService().getAll(iuq).thenAccept(result ->
                result.forEach((uuid, obj) -> this.data.getNode().computeIfAbsent(uuid, x -> new UserCacheDataNode()).set(obj, this.mutedProcessor,
                        this.jailProcessor)));
    }

    @Override public void updateCacheForPlayer(final UUID uuid, final IUserDataObject u) {
        this.data.getNode().computeIfAbsent(uuid, x -> new UserCacheDataNode()).set(u, this.mutedProcessor, this.jailProcessor);
    }

    @Override public void updateCacheForPlayer(final UUID uuid) {
        this.storageManager.getUser(uuid).thenAccept(x -> x.ifPresent(u -> this.updateCacheForPlayer(uuid, u)));
    }

    @Override public void startFilewalkIfNeeded() {
        if (!this.isWalking && (!this.isCorrectVersion() || this.data.getNode().isEmpty())) {
            this.fileWalk();
        }
    }

    @Override public boolean isCorrectVersion() {
        return expectedVersion == this.data.getVersion();
    }

    @Override public boolean fileWalk() {
        synchronized (this.lockingObject) {
            if (this.isWalking) {
                return false;
            }

            this.isWalking = true;
        }

        try {
            final Map<UUID, UserCacheDataNode> data = new HashMap<>();
            final List<UUID> knownUsers = Sponge.server().userManager().streamAll()
                    .map(Identifiable::uniqueId).collect(Collectors.toList());

            int count = 0;
            final IStorageService.Keyed<UUID, IUserQueryObject, IUserDataObject> manager = this.storageManager.getUserService();
            for (final UUID user : knownUsers) {
                if (manager.exists(user).join()) {
                    manager.get(user).join().ifPresent(x -> data.put(user, new UserCacheDataNode().set(x, this.mutedProcessor, this.jailProcessor)));
                    if (++count >= 10) {
                        manager.clearCache();
                        count = 0;
                    }
                }
            }

            this.data = new UserCacheVersionNode();
            this.data.getNode().putAll(data);
            this.save();
        } finally {
            this.isWalking = false;
        }

        return true;
    }

    @Override
    public void setJailProcessor(final Function<IUserDataObject, String> func) {
        this.jailProcessor = func;
    }

    @Override
    public void setMutedProcessor(final Predicate<IUserDataObject> func) {
        this.mutedProcessor = func;
    }

    private GsonConfigurationLoader configurationLoader() {
        return GsonConfigurationLoader.builder()
                .path(this.dataDirectory.get().resolve("usercache.json"))
                .build();
    }

    @Override
    public void onDataFileLocationChange(final INucleusServiceCollection serviceCollection) {
        this.load();
    }
}
