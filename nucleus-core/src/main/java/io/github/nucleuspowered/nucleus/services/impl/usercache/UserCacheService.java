/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.usercache;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.configurate.datatypes.UserCacheDataNode;
import io.github.nucleuspowered.nucleus.configurate.datatypes.UserCacheVersionNode;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.services.impl.storage.queryobjects.IUserQueryObject;
import io.github.nucleuspowered.nucleus.services.impl.storage.queryobjects.UserQueryObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserCacheService;
import io.github.nucleuspowered.storage.services.IStorageService;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.util.Identifiable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Singleton
public class UserCacheService implements IUserCacheService, IReloadableService.DataLocationReloadable {

    private static final int expectedVersion = new UserCacheVersionNode().getVersion();
    private boolean isWalking = false;

    private final Supplier<Path> dataDirectory;
    private final Object lockingObject = new Object();
    private final IStorageManager storageManager;

    private UserCacheVersionNode data;

    @Inject
    public UserCacheService(INucleusServiceCollection serviceCollection) {
        this.dataDirectory = serviceCollection.dataDir();
        this.storageManager = serviceCollection.storageManager();
        serviceCollection.reloadableService().registerDataFileReloadable(this);
        load();
    }

    @Override public void load() {
        try {
            this.data = configurationLoader()
                    .load()
                    .getValue(TypeToken.of(UserCacheVersionNode.class), (Supplier<UserCacheVersionNode>) UserCacheVersionNode::new);
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
            this.data = new UserCacheVersionNode();
        }
    }

    @Override public void save() {
        try {
            GsonConfigurationLoader gsonConfigurationLoader = configurationLoader();
            ConfigurationNode node = gsonConfigurationLoader.createEmptyNode();
            node.setValue(TypeToken.of(UserCacheVersionNode.class), this.data);
            gsonConfigurationLoader.save(node);
        } catch (ObjectMappingException | IOException e) {
            e.printStackTrace();
        }

    }

    @Override public List<UUID> getForIp(String ip) {
        updateCacheForOnlinePlayers();
        String ipToCheck = ip.replace("/", "");
        return this.data.getNode().entrySet().stream().filter(x -> x.getValue()
                .getIpAddress().map(y -> y.equals(ipToCheck)).orElse(false))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override public List<UUID> getJailed() {
        updateCacheForOnlinePlayers();
        return this.data.getNode().entrySet().stream().filter(x -> x.getValue().isJailed())
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override public List<UUID> getJailedIn(String name) {
        updateCacheForOnlinePlayers();
        return this.data.getNode().entrySet().stream()
                .filter(x -> x.getValue().getJailName().map(y -> y.equalsIgnoreCase(name)).orElse(false))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override public List<UUID> getMuted() {
        updateCacheForOnlinePlayers();
        return this.data.getNode().entrySet().stream().filter(x -> x.getValue().isMuted())
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override public void updateCacheForOnlinePlayers() {
        IUserQueryObject iuq = new UserQueryObject();
        iuq.addAllKeys(Sponge.getServer().getOnlinePlayers().stream().map(Identifiable::getUniqueId).collect(Collectors.toList()));
        this.storageManager.getUserService().getAll(iuq).thenAccept(result ->
                result.forEach((uuid, obj) -> this.data.getNode().computeIfAbsent(uuid, x -> new UserCacheDataNode()).set(obj)));
    }

    @Override public void updateCacheForPlayer(UUID uuid, IUserDataObject u) {
        this.data.getNode().computeIfAbsent(uuid, x -> new UserCacheDataNode()).set(u);
    }

    @Override public void updateCacheForPlayer(UUID uuid) {
        this.storageManager.getUser(uuid).thenAccept(x -> x.ifPresent(u -> updateCacheForPlayer(uuid, u)));
    }

    @Override public void startFilewalkIfNeeded() {
        if (!this.isWalking && (!isCorrectVersion() || this.data.getNode().isEmpty())) {
            fileWalk();
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
            Map<UUID, UserCacheDataNode> data = Maps.newHashMap();
            List<UUID> knownUsers = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).getAll().stream()
                    .map(Identifiable::getUniqueId).collect(Collectors.toList());

            int count = 0;
            IStorageService.Keyed<UUID, IUserQueryObject, IUserDataObject> manager = this.storageManager.getUserService();
            for (UUID user : knownUsers) {
                if (manager.exists(user).join()) {
                    manager.get(user).join().ifPresent(x -> data.put(user, new UserCacheDataNode(x)));
                    if (++count >= 10) {
                        manager.clearCache();
                        count = 0;
                    }
                }
            }

            this.data = new UserCacheVersionNode();
            this.data.getNode().putAll(data);
            save();
        } finally {
            this.isWalking = false;
        }

        return true;
    }

    private GsonConfigurationLoader configurationLoader() {
        return GsonConfigurationLoader.builder()
                .setPath(this.dataDirectory.get().resolve("usercache.json"))
                .build();
    }

    @Override
    public void onDataFileLocationChange(INucleusServiceCollection serviceCollection) {
        load();
    }
}
