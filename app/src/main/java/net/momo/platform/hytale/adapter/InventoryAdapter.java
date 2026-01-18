package net.momo.platform.hytale.adapter;

import javax.annotation.Nullable;
import java.util.Map;

/** Adapter interface for inventory operations. */
public interface InventoryAdapter {
    @Nullable
    String getItemIdInHand();

    /** Gets NBT string value from item in hand. */
    @Nullable
    String getNbtString(String key);

    /** Gets NBT long value from item in hand. */
    @Nullable
    Long getNbtLong(String key);

    void consumeItemInHand();

    /** Gives an item to the player. Returns true if successful. */
    boolean giveItem(String itemId, int quantity);

    /** Gives an item with NBT data to the player. Returns true if successful. */
    boolean giveItemWithNbt(String itemId, int quantity, Map<String, Object> nbt);
}
