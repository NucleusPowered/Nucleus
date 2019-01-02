package io.github.nucleuspowered.nucleus.modules.ignore.services;

import com.google.common.collect.ImmutableList;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.modules.ignore.IgnoreKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IgnoreService implements ServiceBase {

    private final Map<UUID, List<UUID>> ignoredBy = new HashMap<>();

    public void addPlayer(UUID player, List<UUID> ignored) {
        removePlayer(player);
        this.ignoredBy.put(player, new ArrayList<>(ignored));
    }

    public void removePlayer(UUID player) {
        this.ignoredBy.remove(player);
    }

    public void ignore(UUID ignorer, UUID ignoree) {
        List<UUID> uuid = get(ignorer);
        if (!uuid.contains(ignoree)) {
            uuid.add(ignoree);
            Nucleus.getNucleus().getStorageManager().getUserService()
                    .getOrNew(ignorer)
                    .thenAccept(x -> x.set(IgnoreKeys.IGNORED, new ArrayList<>(uuid)));
        }
    }

    public void unignore(UUID ignorer, UUID ignoree) {
        List<UUID> uuid = get(ignorer);
        if (uuid.contains(ignoree)) {
            uuid.remove(ignoree);
            Nucleus.getNucleus().getStorageManager().getUserService()
                    .getOrNew(ignorer)
                    .thenAccept(x -> x.set(IgnoreKeys.IGNORED, new ArrayList<>(uuid)));
        }
    }

    public boolean isIgnored(UUID ignorer, UUID ignoree) {
        return get(ignorer).contains(ignoree);
    }

    public List<UUID> getAllIgnored(UUID ignorer) {
        return ImmutableList.copyOf(get(ignorer));
    }

    private List<UUID> get(UUID player) {
        if (!this.ignoredBy.containsKey(player)) {
            addPlayer(player,
                    Nucleus.getNucleus().getStorageManager().getUserService()
                            .getOnThread(player)
                            .flatMap(x -> x.get(IgnoreKeys.IGNORED))
                            .orElseGet(ImmutableList::of));
        }

        return this.ignoredBy.get(player);
    }
}
