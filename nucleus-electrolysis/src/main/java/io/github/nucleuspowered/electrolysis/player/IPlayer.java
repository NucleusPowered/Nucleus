package io.github.nucleuspowered.electrolysis.player;

import io.github.nucleuspowered.electrolysis.command.ICommandSource;

import java.util.UUID;

public interface IPlayer {

    UUID getUniqueId();

    String getName();

}
