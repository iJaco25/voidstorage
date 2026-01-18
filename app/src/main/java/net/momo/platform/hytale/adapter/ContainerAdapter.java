package net.momo.platform.hytale.adapter;

import javax.annotation.Nullable;
import java.util.List;

/** Adapter interface for container operations. */
public interface ContainerAdapter {

    /** Container slot representation. */
    record Slot(short index, @Nullable String itemId, int quantity) {
        public boolean isEmpty() {
            return itemId == null || quantity <= 0;
        }
    }

    /** Returns the number of slots in this container. */
    short getCapacity();

    /** Returns the item at the given slot, or null if empty. */
    @Nullable Slot getSlot(short slot);

    /** Returns all non-empty slots. */
    List<Slot> getNonEmptySlots();

    /** Removes up to quantity items from the given slot. Returns actual amount removed. */
    int removeFromSlot(short slot, int quantity);

    /** Adds an item to the container. Returns amount that could not be added. */
    int addItem(String itemId, int quantity);

    /** Checks if container has space for the given item. */
    boolean hasSpaceFor(String itemId, int quantity);
}
