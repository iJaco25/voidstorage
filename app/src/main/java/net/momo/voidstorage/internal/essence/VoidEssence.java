package net.momo.voidstorage.internal.essence;

import net.momo.silo.util.Validation;

import java.util.UUID;

/** Represents a Void Essence - a token that links to an orphaned storage. */
public record VoidEssence(
    UUID storageId,
    long createdAt
) {
    public VoidEssence {
        Validation.requireNonNull(storageId, "storageId");
    }

    public static VoidEssence create(UUID storageId) {
        return new VoidEssence(storageId, System.currentTimeMillis());
    }

    /** NBT key for storing the storage ID on an item. */
    public static final String NBT_STORAGE_ID = "void_essence_storage_id";

    /** NBT key for storing creation timestamp. */
    public static final String NBT_CREATED_AT = "void_essence_created_at";
}
