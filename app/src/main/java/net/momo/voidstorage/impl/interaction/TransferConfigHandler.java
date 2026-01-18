package net.momo.voidstorage.impl.interaction;

import com.hypixel.hytale.logger.HytaleLogger;
import net.momo.silo.core.Services;
import net.momo.voidstorage.internal.transfer.FilterMode;
import net.momo.voidstorage.internal.transfer.Transfer;
import net.momo.voidstorage.internal.transfer.TransferRegistry;
import net.momo.silo.interaction.InteractionHandler;
import net.momo.silo.interaction.InteractionResult;
import net.momo.silo.ui.UIRegistry;
import net.momo.platform.hytale.adapter.InteractionContextAdapter;
import net.momo.silo.util.Position;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Handles Transfer configuration when right-clicking a placed transfer. */
public final class TransferConfigHandler implements InteractionHandler {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0001-000000000005");

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

        Position targetBlock = context.getTargetBlockPosition();
        if (targetBlock == null) {
            return InteractionResult.skipped("No target block");
        }

        TransferRegistry transferRegistry = Services.get(TransferRegistry.class);
        Position transferPos = targetBlock;

        Optional<Transfer> transferOpt = transferRegistry.getAtPosition(transferPos);
        if (transferOpt.isEmpty()) {
            transferPos = Position.of(targetBlock.x(), targetBlock.y() + 1, targetBlock.z());
            transferOpt = transferRegistry.getAtPosition(transferPos);
        }

        if (transferOpt.isEmpty()) {
            logger.at(Level.FINE).log("No transfer found at %s", targetBlock);
            return InteractionResult.skipped("No transfer at position");
        }

        Transfer transfer = transferOpt.get();

        var inventory = context.getInventory();
        String heldItemId = inventory != null ? inventory.getItemIdInHand() : null;

        UIRegistry uiRegistry = Services.get(UIRegistry.class);
        Map<String, Object> params = new HashMap<>();
        params.put("transfer", transfer);
        params.put("heldItemId", heldItemId);
        uiRegistry.open("transfer_config", playerId, params);

        logger.at(Level.INFO).log("Opened transfer config for %s by player %s", transfer.id(), playerId);
        return InteractionResult.success();
    }

    /** Adds a filter to a transfer. */
    public static void addFilter(UUID transferId, String itemId) {
        TransferRegistry transferRegistry = Services.get(TransferRegistry.class);
        transferRegistry.get(transferId).ifPresent(transfer -> {
            Set<String> newFilters = new HashSet<>(transfer.itemFilters());
            newFilters.add(itemId);
            Transfer updated = transfer.withFilters(newFilters);
            transferRegistry.update(updated);
            logger.at(Level.INFO).log("Added filter %s to transfer %s", itemId, transferId);
        });
    }

    /** Removes a filter from a transfer. */
    public static void removeFilter(UUID transferId, String itemId) {
        TransferRegistry transferRegistry = Services.get(TransferRegistry.class);
        transferRegistry.get(transferId).ifPresent(transfer -> {
            Set<String> newFilters = new HashSet<>(transfer.itemFilters());
            newFilters.remove(itemId);
            Transfer updated = transfer.withFilters(newFilters);
            transferRegistry.update(updated);
            logger.at(Level.INFO).log("Removed filter %s from transfer %s", itemId, transferId);
        });
    }

    /** Clears all filters from a transfer. */
    public static void clearFilters(UUID transferId) {
        TransferRegistry transferRegistry = Services.get(TransferRegistry.class);
        transferRegistry.get(transferId).ifPresent(transfer -> {
            Transfer updated = transfer.withFilters(null);
            transferRegistry.update(updated);
            logger.at(Level.INFO).log("Cleared all filters from transfer %s", transferId);
        });
    }

    /** Toggles the filter mode (whitelist/blacklist) for a transfer. */
    public static void toggleFilterMode(UUID transferId) {
        TransferRegistry transferRegistry = Services.get(TransferRegistry.class);
        transferRegistry.get(transferId).ifPresent(transfer -> {
            FilterMode newMode = transfer.filterMode().toggle();
            Transfer updated = transfer.withFilterMode(newMode);
            transferRegistry.update(updated);
            logger.at(Level.INFO).log("Toggled filter mode to %s for transfer %s", newMode, transferId);
        });
    }
}
