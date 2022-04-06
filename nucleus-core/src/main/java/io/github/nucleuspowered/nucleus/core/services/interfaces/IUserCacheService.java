/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.usercache.UserCacheService;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

@ImplementedBy(UserCacheService.class)
public interface IUserCacheService {

    void load();

    void save();

    List<UUID> getForIp(String ip);

    List<UUID> getJailed();

    List<UUID> getJailedIn(String name);

    List<UUID> getMuted();

    void updateCacheForOnlinePlayers();

    void updateCacheForPlayer(UUID uuid, IUserDataObject u);

    void updateCacheForPlayer(UUID uuid);

    void startFilewalkIfNeeded();

    boolean isCorrectVersion();

    boolean fileWalk();

    void setJailProcessor(Function<IUserDataObject, String> func);

    void setMutedProcessor(Predicate<IUserDataObject> func);
}
