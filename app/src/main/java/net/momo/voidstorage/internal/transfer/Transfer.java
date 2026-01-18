package net.momo.voidstorage.internal.transfer;

import net.momo.silo.util.Position;
import net.momo.silo.util.Validation;

import java.util.*;

/** Transfer - a network interface that moves items between containers and the storage network. */
public final class Transfer {

    private final UUID id;
    private final UUID anchorId;
    private final TransferMode mode;
    private final Position position;
    private final Set<String> itemFilters;
    private final FilterMode filterMode;
    private final long createdAt;

    private Transfer(UUID id, UUID anchorId, TransferMode mode, Position position,
                     Set<String> itemFilters, FilterMode filterMode, long createdAt) {
        Validation.requireNonNull(id, "id");
        Validation.requireNonNull(anchorId, "anchorId");
        Validation.requireNonNull(mode, "mode");
        Validation.requireNonNull(position, "position");
        this.id = id;
        this.anchorId = anchorId;
        this.mode = mode;
        this.position = position;
        this.itemFilters = itemFilters != null
            ? Collections.unmodifiableSet(new HashSet<>(itemFilters))
            : Collections.emptySet();
        this.filterMode = filterMode != null ? filterMode : FilterMode.WHITELIST;
        this.createdAt = createdAt;
    }

    public static Transfer create(UUID anchorId, TransferMode mode, Position position) {
        return new Transfer(UUID.randomUUID(), anchorId, mode, position, null, FilterMode.WHITELIST, System.currentTimeMillis());
    }

    public static Transfer restore(UUID id, UUID anchorId, TransferMode mode, Position position,
                                   Set<String> itemFilters, FilterMode filterMode, long createdAt) {
        return new Transfer(id, anchorId, mode, position, itemFilters, filterMode, createdAt);
    }

    public UUID id() { return id; }
    public UUID anchorId() { return anchorId; }
    public TransferMode mode() { return mode; }
    public Position position() { return position; }
    public Set<String> itemFilters() { return itemFilters; }
    public FilterMode filterMode() { return filterMode; }
    public long createdAt() { return createdAt; }

    public Position targetPosition() {
        return position.below();
    }

    public boolean acceptsItem(String itemId) {
        if (itemFilters.isEmpty()) {
            return filterMode == FilterMode.BLACKLIST;
        }
        boolean inFilter = itemFilters.contains(itemId);
        return filterMode == FilterMode.WHITELIST ? inFilter : !inFilter;
    }

    public Transfer withFilters(Set<String> newFilters) {
        return new Transfer(id, anchorId, mode, position, newFilters, filterMode, createdAt);
    }

    public Transfer withFilterMode(FilterMode newFilterMode) {
        return new Transfer(id, anchorId, mode, position, itemFilters, newFilterMode, createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transfer)) return false;
        return id.equals(((Transfer) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Transfer[id=%s, mode=%s, pos=%s]", id, mode, position);
    }
}
