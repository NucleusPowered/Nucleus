/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.internal.services;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Identifiable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AFKHandler {
    private final HashMap<UUID, AFKData> data = new HashMap<>();

    public void login(UUID user) {
        getData(user);
    }

    /**
     * Updates activity.
     * @param user The user to update.
     *
     * @return <code>true</code> if the user has returned from AFK.
     */
    public boolean updateUserActivity(UUID user) {
        return getData(user).update();
    }

    public void setNoTrack(UUID user, boolean track) {
        getData(user).noTrack = track;
    }

    public void setAFK(UUID user, boolean afk) {
        AFKData a = getData(user);
        a.isAFK = !a.noTrack && afk;
    }

    public void purgeNotOnline() {
        List<UUID> uuid = Sponge.getServer().getOnlinePlayers().stream().map(Identifiable::getUniqueId).collect(Collectors.toList());

        // CMEs BE GONE! The collect part of this creates a new list, avoiding the CME.
        data.keySet().stream().filter(x -> !uuid.contains(x)).collect(Collectors.toList()).forEach(data::remove);
    }

    public AFKData getAFKData(Player pl) {
        return getData(pl.getUniqueId());
    }

    public List<UUID> checkForAfk(int amount) {
        Instant now = Instant.now();
        return data.entrySet().stream()
                .filter(x -> !x.getValue().noTrack && !x.getValue().isAFK && x.getValue().lastActivity.plus(amount, ChronoUnit.SECONDS).isBefore(now))
                .map(x -> {
                    x.getValue().isAFK = true;
                    return x.getKey();
                }).collect(Collectors.toList());
    }

    public List<UUID> checkForAfkKick(int amount) {
        Instant now = Instant.now();
        return data.entrySet().stream()
                .filter(x -> !x.getValue().noTrack && x.getValue().lastActivity.plus(amount, ChronoUnit.SECONDS).isBefore(now))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private AFKData getData(UUID user) {
        if (!data.containsKey(user)) {
            AFKData a = new AFKData();
            data.put(user, a);
            return a;
        }

        return data.get(user);
    }

    public static class AFKData {
        private boolean isAFK = false;
        private boolean noTrack = false;
        private Instant lastActivity = Instant.now();
        private final Object lockingObject = new Object();

        /**
         * Updates activity.
         * @return <code>true</code> if the user has returned from AFK.
         */
        private boolean update() {
            synchronized (lockingObject) {
                lastActivity = Instant.now();

                if (isAFK) {
                    isAFK = false;
                    return !noTrack;
                }

                return false;
            }
        }

        public boolean isAFK() {
            return isAFK;
        }

        public boolean notTracked() {
            return noTrack;
        }

        public Instant getLastActivity() {
            return lastActivity;
        }
    }
}
