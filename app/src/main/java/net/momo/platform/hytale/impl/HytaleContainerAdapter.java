package net.momo.platform.hytale.impl;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import net.momo.platform.hytale.adapter.ContainerAdapter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Hytale implementation of ContainerAdapter wrapping an ItemContainer. */
public final class HytaleContainerAdapter implements ContainerAdapter {

    private final ItemContainer container;

    public HytaleContainerAdapter(ItemContainer container) {
        this.container = container;
    }

    @Override
    public short getCapacity() {
        return container.getCapacity();
    }

    @Override
    @Nullable
    public Slot getSlot(short slot) {
        if (slot < 0 || slot >= container.getCapacity()) {
            return null;
        }
        ItemStack itemStack = container.getItemStack(slot);
        if (itemStack == null || itemStack.isEmpty()) {
            return new Slot(slot, null, 0);
        }
        return new Slot(slot, itemStack.getItemId(), itemStack.getQuantity());
    }

    @Override
    public List<Slot> getNonEmptySlots() {
        List<Slot> slots = new ArrayList<>();
        short capacity = container.getCapacity();
        for (short i = 0; i < capacity; i++) {
            ItemStack itemStack = container.getItemStack(i);
            if (itemStack != null && !itemStack.isEmpty()) {
                slots.add(new Slot(i, itemStack.getItemId(), itemStack.getQuantity()));
            }
        }
        return slots;
    }

    @Override
    public int removeFromSlot(short slot, int quantity) {
        if (slot < 0 || slot >= container.getCapacity() || quantity <= 0) {
            return 0;
        }

        ItemStack existing = container.getItemStack(slot);
        if (existing == null || existing.isEmpty()) {
            return 0;
        }

        int toRemove = Math.min(quantity, existing.getQuantity());
        var transaction = container.removeItemStackFromSlot(slot, toRemove);
        if (transaction.succeeded()) {
            ItemStack output = transaction.getOutput();
            return output != null ? output.getQuantity() : toRemove;
        }
        return 0;
    }

    @Override
    public int addItem(String itemId, int quantity) {
        if (itemId == null || quantity <= 0) {
            return quantity;
        }

        try {
            ItemStack toAdd = new ItemStack(itemId, quantity);
            var transaction = container.addItemStack(toAdd);
            if (transaction.succeeded()) {
                ItemStack remainder = transaction.getRemainder();
                return remainder != null ? remainder.getQuantity() : 0;
            }
        } catch (IllegalArgumentException e) {
            // Invalid item ID
        }
        return quantity;
    }

    @Override
    public boolean hasSpaceFor(String itemId, int quantity) {
        if (itemId == null || quantity <= 0) {
            return true;
        }

        try {
            ItemStack toAdd = new ItemStack(itemId, quantity);
            return container.canAddItemStack(toAdd);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
