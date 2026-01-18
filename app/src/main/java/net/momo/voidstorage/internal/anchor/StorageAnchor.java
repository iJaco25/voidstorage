package net.momo.voidstorage.internal.anchor;

import net.momo.silo.util.Position;
import net.momo.silo.util.Validation;

import java.math.BigInteger;
import java.util.UUID;

/** StorageAnchor - the world-placed access point to the storage network. */
public final class StorageAnchor {

    private final UUID id;
    private final Position position;
    private final BigInteger storageCapacity;
    private final int accessRange;
    private final long createdAt;

    private StorageAnchor(UUID id, Position position, BigInteger storageCapacity, int accessRange, long createdAt) {
        Validation.requireNonNull(id, "id");
        Validation.requireNonNull(position, "position");
        Validation.requirePositive(storageCapacity, "storageCapacity");
        Validation.requirePositive(accessRange, "accessRange");
        this.id = id;
        this.position = position;
        this.storageCapacity = storageCapacity;
        this.accessRange = accessRange;
        this.createdAt = createdAt;
    }

    public static StorageAnchor create(Position position, BigInteger storageCapacity, int accessRange) {
        return new StorageAnchor(UUID.randomUUID(), position, storageCapacity, accessRange, System.currentTimeMillis());
    }

    /** Creates an anchor linked to an existing storage ID (for VoidEssence restoration). */
    public static StorageAnchor createWithId(UUID id, Position position, BigInteger storageCapacity, int accessRange) {
        return new StorageAnchor(id, position, storageCapacity, accessRange, System.currentTimeMillis());
    }

    public static StorageAnchor restore(UUID id, Position position, BigInteger storageCapacity, int accessRange, long createdAt) {
        return new StorageAnchor(id, position, storageCapacity, accessRange, createdAt);
    }

    public UUID id() { return id; }
    public Position position() { return position; }
    public BigInteger storageCapacity() { return storageCapacity; }
    public int accessRange() { return accessRange; }
    public long createdAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StorageAnchor)) return false;
        return id.equals(((StorageAnchor) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("StorageAnchor[id=%s, pos=%s]", id, position);
    }
}
