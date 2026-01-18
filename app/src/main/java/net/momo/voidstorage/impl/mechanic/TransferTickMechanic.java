package net.momo.voidstorage.impl.mechanic;

import com.hypixel.hytale.logger.HytaleLogger;
import net.momo.silo.core.Services;
import net.momo.silo.storage.StorageRegistry;
import net.momo.silo.storage.StoredItem;
import net.momo.silo.storage.Storage;
import net.momo.voidstorage.internal.transfer.Transfer;
import net.momo.voidstorage.internal.transfer.TransferRegistry;
import net.momo.silo.mechanic.TickMechanic;
import net.momo.platform.hytale.adapter.ContainerAdapter;
import net.momo.platform.hytale.adapter.WorldAdapter;
import net.momo.silo.util.Position;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/** Handles periodic transfer tick processing for item movement. */
public final class TransferTickMechanic implements TickMechanic {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0002-000000000001");
    private static final int ITEMS_PER_TICK = 64;

    @Override
    public UUID id() {
        return ID;
    }

    @Override
    public long intervalMs() {
        return 500;
    }

    @Override
    public long initialDelayMs() {
        return 1000;
    }

    @Override
    public void tick(WorldAdapter world) {
        TransferRegistry transferRegistry = Services.get(TransferRegistry.class);
        StorageRegistry storageRegistry = Services.get(StorageRegistry.class);

        for (Transfer transfer : transferRegistry.getAll()) {
            try {
                processTransfer(world, transfer, storageRegistry);
            } catch (Exception e) {
                logger.at(Level.WARNING).withCause(e).log("Error processing transfer %s", transfer.id());
            }
        }
    }

    private void processTransfer(WorldAdapter world, Transfer transfer, StorageRegistry storageRegistry) {
        Position targetPos = transfer.targetPosition();

        if (!world.isChunkLoaded(targetPos)) {
            return;
        }

        ContainerAdapter container = world.getContainerAt(targetPos);
        if (container == null) {
            return;
        }

        Optional<Storage> storageOpt = storageRegistry.get(transfer.anchorId());
        if (storageOpt.isEmpty()) {
            return;
        }

        Storage storage = storageOpt.get();

        switch (transfer.mode()) {
            case INPUT -> processInput(world, transfer, storage, container);
            case OUTPUT -> processOutput(world, transfer, storage, container);
        }
    }

    private void processInput(WorldAdapter world, Transfer transfer, Storage storage, ContainerAdapter container) {
        if (storage.getRemainingCapacity() <= 0) {
            return;
        }

        int transferred = 0;
        List<ContainerAdapter.Slot> slots = container.getNonEmptySlots();

        for (ContainerAdapter.Slot slot : slots) {
            if (transferred >= ITEMS_PER_TICK) {
                break;
            }

            String itemId = slot.itemId();
            if (itemId == null || !transfer.acceptsItem(itemId)) {
                continue;
            }

            int toTransfer = Math.min(slot.quantity(), ITEMS_PER_TICK - transferred);
            toTransfer = (int) Math.min(toTransfer, storage.getRemainingCapacity());

            if (toTransfer <= 0) {
                continue;
            }

            int removed = container.removeFromSlot(slot.index(), toTransfer);
            if (removed <= 0) {
                continue;
            }

            var result = storage.deposit(itemId, removed);
            if (result.isFailure()) {
                int notAdded = container.addItem(itemId, removed);
                if (notAdded > 0) {
                    logger.at(Level.WARNING).log("Lost %d items of %s during input rollback", notAdded, itemId);
                }
                continue;
            }

            transferred += removed;
            world.spawnTransferEffect(transfer.targetPosition(), transfer.position(), itemId, removed, true);
            logger.at(Level.FINE).log("Transfer %s input %d of %s", transfer.id(), removed, itemId);
        }
    }

    private void processOutput(WorldAdapter world, Transfer transfer, Storage storage, ContainerAdapter container) {
        if (storage.getUniqueItemCount() == 0) {
            return;
        }

        int transferred = 0;
        List<StoredItem> items = storage.getItemsSorted();

        for (StoredItem item : items) {
            if (transferred >= ITEMS_PER_TICK) {
                break;
            }

            String itemId = item.itemId();
            if (!transfer.acceptsItem(itemId)) {
                continue;
            }

            int toTransfer = (int) Math.min(item.quantity(), ITEMS_PER_TICK - transferred);
            if (toTransfer <= 0) {
                continue;
            }

            if (!container.hasSpaceFor(itemId, toTransfer)) {
                toTransfer = 1;
                if (!container.hasSpaceFor(itemId, 1)) {
                    continue;
                }
            }

            var result = storage.withdraw(itemId, toTransfer);
            if (result.isFailure()) {
                continue;
            }

            long withdrawn = result.value();
            if (withdrawn <= 0) {
                continue;
            }

            int notAdded = container.addItem(itemId, (int) withdrawn);
            if (notAdded > 0) {
                storage.deposit(itemId, notAdded);
                withdrawn -= notAdded;
            }

            transferred += (int) withdrawn;
            world.spawnTransferEffect(transfer.position(), transfer.targetPosition(), itemId, (int) withdrawn, false);
            logger.at(Level.FINE).log("Transfer %s output %d of %s", transfer.id(), (int) withdrawn, itemId);
        }
    }
}
