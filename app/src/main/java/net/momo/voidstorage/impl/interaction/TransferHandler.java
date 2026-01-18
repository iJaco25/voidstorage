package net.momo.voidstorage.impl.interaction;

import com.hypixel.hytale.logger.HytaleLogger;
import net.momo.silo.core.ModConfig;
import net.momo.silo.core.Services;
import net.momo.voidstorage.internal.anchor.AnchorRegistry;
import net.momo.voidstorage.internal.anchor.StorageAnchor;
import net.momo.voidstorage.internal.transfer.Transfer;
import net.momo.voidstorage.internal.transfer.TransferMode;
import net.momo.voidstorage.internal.transfer.TransferRegistry;
import net.momo.silo.interaction.InteractionHandler;
import net.momo.silo.interaction.InteractionResult;
import net.momo.voidstorage.VoidStorageItems;
import net.momo.platform.hytale.adapter.InteractionContextAdapter;
import net.momo.platform.hytale.adapter.InventoryAdapter;
import net.momo.platform.hytale.adapter.WorldAdapter;
import net.momo.silo.util.Position;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/** Handles Transfer placement interactions. */
public final class TransferHandler implements InteractionHandler {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final UUID INPUT_ID = UUID.fromString("00000000-0000-0000-0001-000000000003");
    private static final UUID OUTPUT_ID = UUID.fromString("00000000-0000-0000-0001-000000000004");

    private final TransferMode transferMode;
    private final UUID id;

    public TransferHandler(TransferMode transferMode) {
        this.transferMode = transferMode;
        this.id = transferMode == TransferMode.INPUT ? INPUT_ID : OUTPUT_ID;
    }

    @Override
    public UUID id() {
        return id;
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

        Position targetPos = Position.of(targetBlock.x(), targetBlock.y() + 1, targetBlock.z());

        TransferRegistry transferRegistry = Services.get(TransferRegistry.class);
        if (transferRegistry.getAtPosition(targetPos).isPresent()) {
            logger.at(Level.INFO).log("%s already exists at %s", ModConfig.TRANSFER_NAME, targetPos);
            return InteractionResult.skipped(ModConfig.TRANSFER_NAME + " already exists");
        }

        Position containerPos = targetPos.below();
        if (!world.hasContainerAt(containerPos)) {
            logger.at(Level.INFO).log("No container below %s position %s", ModConfig.TRANSFER_NAME.toLowerCase(), targetPos);
            return InteractionResult.skipped("No container below");
        }

        AnchorRegistry anchorRegistry = Services.get(AnchorRegistry.class);
        Optional<StorageAnchor> nearestAnchor = findNearestAnchor(anchorRegistry, targetPos);
        if (nearestAnchor.isEmpty()) {
            logger.at(Level.INFO).log("No %s in range for %s at %s", ModConfig.ANCHOR_NAME.toLowerCase(), ModConfig.TRANSFER_NAME.toLowerCase(), targetPos);
            return InteractionResult.skipped("No " + ModConfig.ANCHOR_NAME.toLowerCase() + " in range");
        }

        StorageAnchor anchor = nearestAnchor.get();
        Transfer transfer = Transfer.create(anchor.id(), transferMode, targetPos);
        transferRegistry.register(transfer);

        String blockKey = transferMode == TransferMode.INPUT
            ? VoidStorageItems.SIGIL_ABSORPTION
            : VoidStorageItems.SIGIL_MANIFESTATION;
        boolean placed = world.placeBlock(targetPos, blockKey);
        if (!placed) {
            logger.at(Level.WARNING).log("Failed to place Transfer block at %s", targetPos);
        }

        InventoryAdapter inventory = context.getInventory();
        if (inventory != null) {
            inventory.consumeItemInHand();
        }

        logger.at(Level.INFO).log("Created %s %s %s at %s linked to %s %s by player %s",
            transferMode.displayName(), ModConfig.TRANSFER_NAME, transfer.id(), targetPos, ModConfig.ANCHOR_NAME, anchor.id(), playerId);

        return InteractionResult.success();
    }

    private Optional<StorageAnchor> findNearestAnchor(AnchorRegistry registry, Position position) {
        StorageAnchor nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (StorageAnchor anchor : registry.getAll()) {
            double distance = calculateDistance(position, anchor.position());
            if (distance <= anchor.accessRange() && distance < nearestDistance) {
                nearest = anchor;
                nearestDistance = distance;
            }
        }

        return Optional.ofNullable(nearest);
    }

    private double calculateDistance(Position a, Position b) {
        int dx = a.x() - b.x();
        int dy = a.y() - b.y();
        int dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
