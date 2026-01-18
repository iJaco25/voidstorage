package net.momo.platform.hytale.impl;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import net.momo.platform.hytale.adapter.InventoryAdapter;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.BsonValue;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.logging.Level;

/** Hytale implementation of InventoryAdapter. */
public final class HytaleInventoryAdapter implements InventoryAdapter {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private final Inventory inventory;

    public HytaleInventoryAdapter(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    @Nullable
    public String getItemIdInHand() {
        ItemStack item = inventory.getItemInHand();
        if (item == null || item.isEmpty()) return null;
        return item.getItem().getId();
    }

    @Override
    @Nullable
    public String getNbtString(String key) {
        ItemStack item = inventory.getItemInHand();
        if (item == null || item.isEmpty()) return null;

        BsonDocument metadata = item.getMetadata();
        if (metadata == null || !metadata.containsKey(key)) return null;

        BsonValue value = metadata.get(key);
        return value.isString() ? value.asString().getValue() : null;
    }

    @Override
    @Nullable
    public Long getNbtLong(String key) {
        ItemStack item = inventory.getItemInHand();
        if (item == null || item.isEmpty()) return null;

        BsonDocument metadata = item.getMetadata();
        if (metadata == null || !metadata.containsKey(key)) return null;

        BsonValue value = metadata.get(key);
        if (value.isInt64()) return value.asInt64().getValue();
        if (value.isInt32()) return (long) value.asInt32().getValue();
        return null;
    }

    @Override
    public void consumeItemInHand() {
        ItemStack item = inventory.getItemInHand();
        if (item == null || item.isEmpty()) return;

        byte activeSlot = inventory.getActiveHotbarSlot();
        if (activeSlot < 0) return;

        var hotbar = inventory.getHotbar();
        int qty = item.getQuantity();
        if (qty <= 1) {
            hotbar.setItemStackForSlot(activeSlot, null);
        } else {
            hotbar.setItemStackForSlot(activeSlot, item.withQuantity(qty - 1));
        }
    }

    @Override
    public boolean giveItem(String itemId, int quantity) {
        return giveItemWithNbt(itemId, quantity, null);
    }

    @Override
    public boolean giveItemWithNbt(String itemId, int quantity, Map<String, Object> nbtData) {
        try {
            BsonDocument metadata = null;
            if (nbtData != null && !nbtData.isEmpty()) {
                metadata = new BsonDocument();
                for (Map.Entry<String, Object> entry : nbtData.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String s) {
                        metadata.put(entry.getKey(), new BsonString(s));
                    } else if (value instanceof Long l) {
                        metadata.put(entry.getKey(), new BsonInt64(l));
                    } else if (value instanceof Integer i) {
                        metadata.put(entry.getKey(), new BsonInt64(i.longValue()));
                    }
                }
            }

            ItemStack stack = new ItemStack(itemId, quantity, metadata);

            var hotbar = inventory.getHotbar();
            var transaction = hotbar.addItemStack(stack);
            return transaction.succeeded();
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to give item %s x%d: %s", itemId, quantity, e.getMessage());
            return false;
        }
    }
}
