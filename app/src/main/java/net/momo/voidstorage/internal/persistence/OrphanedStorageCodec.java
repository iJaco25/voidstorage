package net.momo.voidstorage.internal.persistence;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momo.silo.persistence.Codec;
import net.momo.voidstorage.internal.essence.OrphanedStorageRegistry.OrphanedStorage;

import java.util.UUID;

/** Codec for OrphanedStorage serialization. */
public final class OrphanedStorageCodec implements Codec<OrphanedStorage> {

    @Override
    public JsonElement serialize(OrphanedStorage orphan) {
        JsonObject json = new JsonObject();
        json.addProperty("storageId", orphan.storageId().toString());
        json.addProperty("orphanedAt", orphan.orphanedAt());
        return json;
    }

    @Override
    public OrphanedStorage deserialize(JsonElement element) {
        JsonObject json = element.getAsJsonObject();
        UUID storageId = UUID.fromString(json.get("storageId").getAsString());
        long orphanedAt = json.get("orphanedAt").getAsLong();
        return new OrphanedStorage(storageId, orphanedAt);
    }
}
