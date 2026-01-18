package net.momo.voidstorage.impl.interaction;

import com.hypixel.hytale.logger.HytaleLogger;
import net.momo.silo.core.ModConfig;
import net.momo.silo.core.Services;
import net.momo.silo.storage.Storage;
import net.momo.silo.storage.StorageRegistry;
import net.momo.voidstorage.internal.anchor.AnchorRegistry;
import net.momo.voidstorage.internal.anchor.StorageAnchor;
import net.momo.voidstorage.internal.essence.OrphanedStorageRegistry;
import net.momo.voidstorage.internal.essence.VoidEssence;
import net.momo.silo.interaction.InteractionHandler;
import net.momo.silo.interaction.InteractionResult;
import net.momo.voidstorage.VoidStorageItems;
import net.momo.platform.hytale.adapter.InteractionContextAdapter;
import net.momo.platform.hytale.adapter.InventoryAdapter;
import net.momo.platform.hytale.adapter.WorldAdapter;
import net.momo.silo.util.Position;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/** Handles Anchor Core placement to create new StorageAnchors. */
public final class AnchorCoreHandler implements InteractionHandler {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0001-000000000002");

    private static final BigInteger DEFAULT_STORAGE_CAPACITY = BigInteger.valueOf(1_000_000_000);
    private static final int DEFAULT_ACCESS_RANGE = Integer.MAX_VALUE;

    private static final String PILLAR_BLOCK = "Anomaly_Anchor";
    private static final String PARTICLE_COLOR = "#a0f";
    private static final String PARTICLE_SYSTEM = "Block_Gem_Sparks";

    @Override
    public UUID id() {
        return ID;
    }

    @Override
    public InteractionResult handle(InteractionContextAdapter context) {
        UUID playerId = context.getPlayerId();
        if (playerId == null) {
            return InteractionResult.skipped("No player");
        }

        WorldAdapter world = context.getWorld();
        if (world == null) {
            return InteractionResult.skipped("No world");
        }

        Position targetBlock = context.getTargetBlockPosition();
        if (targetBlock == null) {
            return InteractionResult.skipped("No target block");
        }

        Position corePos = Position.of(targetBlock.x(), targetBlock.y() + 1, targetBlock.z());

        AnchorRegistry anchorRegistry = Services.get(AnchorRegistry.class);
        if (anchorRegistry.getAtPosition(corePos).isPresent()) {
            return InteractionResult.skipped(ModConfig.ANCHOR_NAME + " already exists");
        }

        InventoryAdapter inventory = context.getInventory();

        // Check if player is using VoidEssence to link to existing storage
        UUID linkedStorageId = tryExtractVoidEssenceStorageId(inventory);
        
        StorageAnchor anchor;
        StorageRegistry storageRegistry = Services.get(StorageRegistry.class);

        if (linkedStorageId != null) {
            // Link to existing orphaned storage
            Optional<Storage> existingStorage = storageRegistry.get(linkedStorageId);
            if (existingStorage.isEmpty()) {
                return InteractionResult.failed("Void Essence links to non-existent storage");
            }

            // Reclaim from orphan registry
            OrphanedStorageRegistry orphanRegistry = Services.get(OrphanedStorageRegistry.class);
            orphanRegistry.reclaim(linkedStorageId);

            // Create anchor with the linked storage ID
            anchor = StorageAnchor.createWithId(linkedStorageId, corePos, DEFAULT_STORAGE_CAPACITY, DEFAULT_ACCESS_RANGE);
            logger.at(Level.INFO).log("Linking %s to existing storage %s via Void Essence", ModConfig.ANCHOR_NAME, linkedStorageId);
        } else {
            // Create new anchor and storage
            anchor = StorageAnchor.create(corePos, DEFAULT_STORAGE_CAPACITY, DEFAULT_ACCESS_RANGE);
            storageRegistry.getOrCreate(anchor.id(), anchor.storageCapacity());
        }

        anchorRegistry.register(anchor);

        buildStructure(world, corePos);
        spawnPlacementEffects(world, corePos);
        world.playSound(corePos, "block.amethyst.break");

        if (inventory != null) {
            inventory.consumeItemInHand();
        }

        logger.at(Level.INFO).log("Created %s %s at %s with structure", ModConfig.ANCHOR_NAME, anchor.id(), corePos);
        return InteractionResult.success();
    }

    /** Extracts storage ID from VoidEssence item in hand, or null if not holding VoidEssence. */
    private UUID tryExtractVoidEssenceStorageId(InventoryAdapter inventory) {
        if (inventory == null) return null;

        String itemId = inventory.getItemIdInHand();
        if (!VoidStorageItems.isVoidEssence(itemId)) return null;

        String storageIdStr = inventory.getNbtString(VoidEssence.NBT_STORAGE_ID);
        if (storageIdStr == null) return null;

        try {
            return UUID.fromString(storageIdStr);
        } catch (IllegalArgumentException e) {
            logger.at(Level.WARNING).log("Invalid storage ID in Void Essence: %s", storageIdStr);
            return null;
        }
    }

    private void buildStructure(WorldAdapter world, Position corePos) {
        world.placeBlock(corePos, VoidStorageItems.ANOMALY_CORE);

        int[][] cornerOffsets = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        for (int[] offset : cornerOffsets) {
            Position pillarBase = Position.of(corePos.x() + offset[0], corePos.y(), corePos.z() + offset[1]);
            Position pillarTop = Position.of(corePos.x() + offset[0], corePos.y() + 1, corePos.z() + offset[1]);
            world.placeBlock(pillarBase, PILLAR_BLOCK);
            world.placeBlock(pillarTop, PILLAR_BLOCK);
        }

        logger.at(Level.FINE).log("Built anchor structure at %s", corePos);
    }

    private void spawnPlacementEffects(WorldAdapter world, Position corePos) {
        world.spawnParticles(corePos, PARTICLE_SYSTEM, PARTICLE_COLOR);

        int[][] cornerOffsets = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        for (int[] offset : cornerOffsets) {
            Position pillarPos = Position.of(corePos.x() + offset[0], corePos.y() + 1, corePos.z() + offset[1]);
            world.spawnParticles(pillarPos, PARTICLE_SYSTEM, PARTICLE_COLOR);
        }
    }

    /** Gets all block positions that are part of the anchor structure. */
    public static List<Position> getStructurePositions(Position corePos) {
        List<Position> positions = new ArrayList<>();
        positions.add(corePos);

        int[][] cornerOffsets = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        for (int[] offset : cornerOffsets) {
            positions.add(Position.of(corePos.x() + offset[0], corePos.y(), corePos.z() + offset[1]));
            positions.add(Position.of(corePos.x() + offset[0], corePos.y() + 1, corePos.z() + offset[1]));
        }

        return positions;
    }
}
