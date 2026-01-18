package net.momo.platform.hytale.impl;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerBlockState;
import net.momo.platform.hytale.adapter.ContainerAdapter;
import net.momo.platform.hytale.adapter.WorldAdapter;
import net.momo.silo.util.Position;

import javax.annotation.Nullable;
import java.util.logging.Level;

/** Hytale implementation of WorldAdapter. */
public final class HytaleWorldAdapter implements WorldAdapter {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private final World world;

    public HytaleWorldAdapter(World world) {
        this.world = world;
    }

    @Override
    public String getBlockAt(Position pos) {
        int blockId = world.getBlock(pos.x(), pos.y(), pos.z());
        BlockType blockType = (BlockType) BlockType.getAssetMap().getAsset(blockId);
        if (blockType != null) {
            return blockType.getId();
        }
        return "hytale:block_" + blockId;
    }

    @Override
    public boolean isChunkLoaded(Position pos) {
        return getChunkAt(pos) != null;
    }

    @Override
    public boolean hasContainerAt(Position pos) {
        return getContainerAt(pos) != null;
    }

    @Override
    public boolean placeBlock(Position pos, String blockTypeKey) {
        WorldChunk chunk = getChunkAt(pos);
        if (chunk == null) {
            logger.at(Level.WARNING).log("placeBlock failed: chunk not loaded at %s", pos);
            return false;
        }

        try {
            boolean result = chunk.setBlock(pos.x(), pos.y(), pos.z(), blockTypeKey);
            logger.at(Level.INFO).log("placeBlock at %s with key '%s': %s", pos, blockTypeKey, result ? "success" : "failed");
            return result;
        } catch (IllegalArgumentException e) {
            logger.at(Level.WARNING).log("placeBlock failed: unknown block type key '%s' - %s", blockTypeKey, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean breakBlock(Position pos) {
        WorldChunk chunk = getChunkAt(pos);
        if (chunk == null) return false;

        return chunk.breakBlock(pos.x(), pos.y(), pos.z());
    }

    @Override
    @Nullable
    public ContainerAdapter getContainerAt(Position pos) {
        WorldChunk chunk = getChunkAt(pos);
        if (chunk == null) {
            return null;
        }

        BlockComponentChunk blockComponentChunk = chunk.getBlockComponentChunk();
        if (blockComponentChunk == null) {
            return null;
        }

        int localX = pos.x() & 31;
        int localZ = pos.z() & 31;
        int blockIndex = ChunkUtil.indexBlockInColumn(localX, pos.y(), localZ);
        if (!blockComponentChunk.hasComponents(blockIndex)) {
            return null;
        }

        BlockState state = chunk.getState(localX, pos.y(), localZ);
        if (state instanceof ItemContainerBlockState containerState) {
            return new HytaleContainerAdapter(containerState.getItemContainer());
        }

        return null;
    }

    private WorldChunk getChunkAt(Position pos) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x(), pos.z());
        return world.getChunkIfLoaded(chunkIndex);
    }

    @Override
    public void spawnTransferEffect(Position from, Position to, String itemId, int quantity, boolean isAbsorption) {
        // TODO: Implement particle effect spawning via SpawnParticleSystem packet.
        // This requires broadcasting to nearby players which needs the Store<EntityStore>
        // from the world's entity system. For now, this is a no-op placeholder.
        // The particle system ID could be "Block_Gem_Sparks" with custom colors
        // (blue #0af for absorption, orange #fa0 for manifestation).
    }

    @Override
    public void spawnParticles(Position pos, String systemId, String color) {
        // TODO: Implement via SpawnParticleSystem packet to nearby players.
        // For now, the block itself has particles defined in BlockType.Particles.
        logger.at(Level.FINE).log("spawnParticles at %s with system '%s' color '%s' (not yet implemented)",
            pos, systemId, color);
    }

    @Override
    public void playSound(Position pos, String soundId) {
        // TODO: Implement via PlaySound packet to nearby players.
        logger.at(Level.FINE).log("playSound at %s with id '%s' (not yet implemented)", pos, soundId);
    }
}
