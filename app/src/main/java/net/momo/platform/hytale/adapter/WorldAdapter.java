package net.momo.platform.hytale.adapter;

import net.momo.silo.util.Position;

import javax.annotation.Nullable;

/** Adapter interface for world operations. */
public interface WorldAdapter {
    String getBlockAt(Position pos);
    boolean isChunkLoaded(Position pos);
    boolean hasContainerAt(Position pos);

    /** Places a block at the given position. Returns true if successful. */
    boolean placeBlock(Position pos, String blockTypeKey);

    /** Removes the block at the given position. Returns true if successful. */
    boolean breakBlock(Position pos);

    /** Gets a container adapter for the container at the given position, or null if none. */
    @Nullable ContainerAdapter getContainerAt(Position pos);

    /** Spawns a transfer particle effect from source to destination position. */
    void spawnTransferEffect(Position from, Position to, String itemId, int quantity, boolean isAbsorption);

    /** Spawns particles at the given position with specified color and system. */
    void spawnParticles(Position pos, String systemId, String color);

    /** Plays a sound at the given position. */
    void playSound(Position pos, String soundId);
}
