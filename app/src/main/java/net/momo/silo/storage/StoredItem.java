package net.momo.silo.storage;

import net.momo.silo.util.Validation;

/** Immutable representation of an item stored in the Void. */
public final class StoredItem {

    private final String itemId;
    private final long quantity;

    public StoredItem(String itemId, long quantity) {
        Validation.requireValidItemId(itemId);
        Validation.requireNonNegative(quantity, "quantity");
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public String itemId() { return itemId; }
    public long quantity() { return quantity; }

    public StoredItem withQuantity(long newQuantity) {
        return new StoredItem(itemId, newQuantity);
    }

    public StoredItem add(long delta) {
        return withQuantity(quantity + delta);
    }

    public StoredItem subtract(long delta) {
        return withQuantity(Math.max(0, quantity - delta));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoredItem)) return false;
        return itemId.equals(((StoredItem) o).itemId);
    }

    @Override
    public int hashCode() {
        return itemId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s x%d", itemId, quantity);
    }
}
