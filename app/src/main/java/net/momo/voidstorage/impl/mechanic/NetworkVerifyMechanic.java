package net.momo.voidstorage.impl.mechanic;

import com.hypixel.hytale.logger.HytaleLogger;
import net.momo.silo.core.Services;
import net.momo.silo.storage.StorageRegistry;
import net.momo.voidstorage.internal.anchor.AnchorRegistry;
import net.momo.voidstorage.internal.anchor.StorageAnchor;
import net.momo.voidstorage.internal.transfer.Transfer;
import net.momo.voidstorage.internal.transfer.TransferRegistry;
import net.momo.silo.mechanic.TickMechanic;
import net.momo.voidstorage.impl.interaction.AnchorCoreHandler;
import net.momo.voidstorage.VoidStorageItems;
import net.momo.platform.hytale.adapter.WorldAdapter;
import net.momo.silo.util.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Verifies that registered anchors and transfers still have their blocks in the world. */
public final class NetworkVerifyMechanic implements TickMechanic {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0002-000000000002");

    @Override
    public UUID id() {
        return ID;
    }

    @Override
    public long intervalMs() {
        return 1000;
    }

    @Override
    public long initialDelayMs() {
        return 2000;
    }

    @Override
    public void tick(WorldAdapter world) {
        verifyAnchors(world);
        verifyTransfers(world);
    }

    private void verifyAnchors(WorldAdapter world) {
        AnchorRegistry anchorRegistry = Services.get(AnchorRegistry.class);
        StorageRegistry storageRegistry = Services.get(StorageRegistry.class);
        TransferRegistry transferRegistry = Services.get(TransferRegistry.class);

        List<UUID> toRemove = new ArrayList<>();
        Map<UUID, Position> positionsToRemove = new HashMap<>();

        for (StorageAnchor anchor : anchorRegistry.getAll()) {
            Position pos = anchor.position();
            if (!world.isChunkLoaded(pos)) {
                continue;
            }

            String blockAt = world.getBlockAt(pos);
            String blockId = VoidStorageItems.stripNamespace(blockAt);

            if (!VoidStorageItems.ANOMALY_CORE.equals(blockId)) {
                toRemove.add(anchor.id());
                positionsToRemove.put(anchor.id(), pos);
                logger.at(Level.INFO).log("Block verification: Anchor %s at %s is gone (block=%s)",
                    anchor.id(), pos, blockAt);
            }
        }

        for (UUID id : toRemove) {
            Position corePos = positionsToRemove.get(id);
            removeAnchorStructure(world, corePos);
            anchorRegistry.unregister(id);
            storageRegistry.unregister(id);
            transferRegistry.unregisterByAnchor(id);
        }
    }

    private void removeAnchorStructure(WorldAdapter world, Position corePos) {
        for (Position pos : AnchorCoreHandler.getStructurePositions(corePos)) {
            if (pos.equals(corePos)) {
                continue;
            }
            world.breakBlock(pos);
        }
        logger.at(Level.FINE).log("Removed anchor structure pillars at %s", corePos);
    }

    private void verifyTransfers(WorldAdapter world) {
        TransferRegistry transferRegistry = Services.get(TransferRegistry.class);

        List<UUID> toRemove = new ArrayList<>();

        for (Transfer transfer : transferRegistry.getAll()) {
            Position pos = transfer.position();
            if (!world.isChunkLoaded(pos)) {
                continue;
            }

            String blockAt = world.getBlockAt(pos);
            String blockId = VoidStorageItems.stripNamespace(blockAt);

            if (!VoidStorageItems.SIGIL_ABSORPTION.equals(blockId)
                && !VoidStorageItems.SIGIL_MANIFESTATION.equals(blockId)) {
                toRemove.add(transfer.id());
                logger.at(Level.INFO).log("Block verification: Transfer %s at %s is gone (block=%s)",
                    transfer.id(), pos, blockAt);
            }
        }

        for (UUID id : toRemove) {
            transferRegistry.unregister(id);
        }
    }
}
