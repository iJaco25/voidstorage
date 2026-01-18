package net.momo.voidstorage.internal.persistence;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momo.silo.core.Services;
import net.momo.silo.persistence.Codec;
import net.momo.silo.storage.Storage;
import net.momo.silo.storage.StorageRegistry;
import net.momo.silo.util.Position;
import net.momo.voidstorage.internal.anchor.StorageAnchor;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

/** Codec for StorageAnchor serialization. */
public final class AnchorCodec implements Codec<StorageAnchor> {

    @Override
    public JsonElement serialize(StorageAnchor anchor) {
        JsonObject json = new JsonObject();
        json.addProperty("id", anchor.id().toString());
        json.addProperty("x", anchor.position().x());
        json.addProperty("y", anchor.position().y());
        json.addProperty("z", anchor.position().z());
        json.addProperty("storageCapacity", anchor.storageCapacity().toString());
        json.addProperty("accessRange", anchor.accessRange());
        json.addProperty("createdAt", anchor.createdAt());

        // Serialize storage items
        StorageRegistry storageRegistry = Services.get(StorageRegistry.class);
        storageRegistry.get(anchor.id()).ifPresent(storage -> {
            JsonObject items = new JsonObject();
            for (Map.Entry<String, Long> entry : storage.getItemsAsMap().entrySet()) {
                items.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("items", items);
        });

        return json;
    }

    @Override
    public StorageAnchor deserialize(JsonElement element) {
        JsonObject json = element.getAsJsonObject();

        UUID id = UUID.fromString(json.get("id").getAsString());
        Position position = Position.of(
            json.get("x").getAsInt(),
            json.get("y").getAsInt(),
            json.get("z").getAsInt()
        );
        BigInteger storageCapacity = new BigInteger(json.get("storageCapacity").getAsString());
        int accessRange = json.get("accessRange").getAsInt();
        long createdAt = json.get("createdAt").getAsLong();

        StorageAnchor anchor = StorageAnchor.restore(id, position, storageCapacity, accessRange, createdAt);

        // Restore storage items
        if (json.has("items")) {
            StorageRegistry storageRegistry = Services.get(StorageRegistry.class);
            Storage storage = storageRegistry.getOrCreate(anchor.id(), anchor.storageCapacity());

            JsonObject items = json.getAsJsonObject("items");
            for (String itemId : items.keySet()) {
                long quantity = items.get(itemId).getAsLong();
                storage.deposit(itemId, quantity);
            }
        }

        return anchor;
    }
}
