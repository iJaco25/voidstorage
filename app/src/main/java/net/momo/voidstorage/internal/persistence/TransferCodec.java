package net.momo.voidstorage.internal.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momo.silo.persistence.Codec;
import net.momo.silo.util.Position;
import net.momo.voidstorage.internal.transfer.FilterMode;
import net.momo.voidstorage.internal.transfer.Transfer;
import net.momo.voidstorage.internal.transfer.TransferMode;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Codec for Transfer serialization. */
public final class TransferCodec implements Codec<Transfer> {

    @Override
    public JsonElement serialize(Transfer transfer) {
        JsonObject json = new JsonObject();
        json.addProperty("id", transfer.id().toString());
        json.addProperty("anchorId", transfer.anchorId().toString());
        json.addProperty("mode", transfer.mode().name());
        json.addProperty("x", transfer.position().x());
        json.addProperty("y", transfer.position().y());
        json.addProperty("z", transfer.position().z());
        json.addProperty("createdAt", transfer.createdAt());

        JsonArray filters = new JsonArray();
        for (String filter : transfer.itemFilters()) {
            filters.add(filter);
        }
        json.add("itemFilters", filters);
        json.addProperty("filterMode", transfer.filterMode().name());

        return json;
    }

    @Override
    public Transfer deserialize(JsonElement element) {
        JsonObject json = element.getAsJsonObject();

        UUID id = UUID.fromString(json.get("id").getAsString());
        UUID anchorId = UUID.fromString(json.get("anchorId").getAsString());
        TransferMode mode = TransferMode.valueOf(json.get("mode").getAsString());
        Position position = Position.of(
            json.get("x").getAsInt(),
            json.get("y").getAsInt(),
            json.get("z").getAsInt()
        );
        long createdAt = json.get("createdAt").getAsLong();

        Set<String> itemFilters = new HashSet<>();
        if (json.has("itemFilters")) {
            for (JsonElement filter : json.getAsJsonArray("itemFilters")) {
                itemFilters.add(filter.getAsString());
            }
        }

        FilterMode filterMode = FilterMode.WHITELIST;
        if (json.has("filterMode")) {
            filterMode = FilterMode.valueOf(json.get("filterMode").getAsString());
        }

        return Transfer.restore(id, anchorId, mode, position, itemFilters, filterMode, createdAt);
    }
}
