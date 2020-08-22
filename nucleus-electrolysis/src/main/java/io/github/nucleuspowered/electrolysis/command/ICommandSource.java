package io.github.nucleuspowered.electrolysis.command;

import io.github.nucleuspowered.electrolysis.player.IPlayer;

import java.util.Optional;

public interface ICommandSource {

    Optional<IPlayer> getPlayer();

    boolean hasPermission(String permission);

}
