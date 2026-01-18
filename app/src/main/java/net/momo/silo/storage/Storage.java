package net.momo.silo.storage;

import net.momo.silo.util.Result;
import net.momo.silo.util.Validation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Generic storage container. Thread-safe with bounded capacity. */
public final class Storage {

    private static final int MAX_UNIQUE_ITEMS = 10_000;
    private static final long MAX_QUANTITY_PER_ITEM = 1_000_000_000L;

    private final Map<String, AtomicLong> items = new ConcurrentHashMap<>();
    private final AtomicLong totalItems = new AtomicLong();
    private final AtomicInteger uniqueItemCount = new AtomicInteger();
    private final long capacity;

    public Storage(long capacity) {
        Validation.requirePositive(capacity, "capacity");
        this.capacity = capacity;
    }

    public Storage(BigInteger capacity) {
        Validation.requirePositive(capacity, "capacity");
        this.capacity = capacity.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    /** Deposit items into storage. Returns new total quantity on success. */
    public Result<Long> deposit(String itemId, long quantity) {
        Validation.requireValidItemId(itemId);
        if (quantity <= 0) {
            return Result.failure("Quantity must be positive");
        }

        AtomicLong counter = getOrCreateCounterForDeposit(itemId);
        if (counter == null) {
            return Result.failure("Maximum unique item types reached");
        }

        while (true) {
            long currentItem = counter.get();
            long maxForItem = MAX_QUANTITY_PER_ITEM - currentItem;
            if (maxForItem <= 0) {
                discardIfEmpty(itemId, counter);
                return Result.failure("Cannot add more of this item");
            }

            long desired = Math.min(quantity, maxForItem);
            long reserved = reserveCapacity(desired);
            if (reserved <= 0) {
                discardIfEmpty(itemId, counter);
                return Result.failure("Storage is full");
            }

            if (counter.compareAndSet(currentItem, currentItem + reserved)) {
                return Result.success(currentItem + reserved);
            }

            totalItems.addAndGet(-reserved);
        }
    }

    /** Withdraw items from storage. Returns amount actually withdrawn. */
    public Result<Long> withdraw(String itemId, long quantity) {
        Validation.requireValidItemId(itemId);
        if (quantity <= 0) {
            return Result.failure("Quantity must be positive");
        }

        AtomicLong counter = items.get(itemId);
        if (counter == null) {
            return Result.success(0L);
        }

        while (true) {
            long current = counter.get();
            if (current <= 0) {
                discardIfEmpty(itemId, counter);
                return Result.success(0L);
            }

            long toRemove = Math.min(quantity, current);
            long remaining = current - toRemove;
            if (counter.compareAndSet(current, remaining)) {
                totalItems.addAndGet(-toRemove);
                if (remaining == 0) {
                    discardIfEmpty(itemId, counter);
                }
                return Result.success(toRemove);
            }
        }
    }

    public Optional<StoredItem> getItem(String itemId) {
        AtomicLong counter = items.get(itemId);
        if (counter == null) {
            return Optional.empty();
        }
        long quantity = counter.get();
        if (quantity <= 0) {
            return Optional.empty();
        }
        return Optional.of(new StoredItem(itemId, quantity));
    }

    public long getQuantity(String itemId) {
        AtomicLong counter = items.get(itemId);
        return counter != null ? counter.get() : 0;
    }

    public boolean hasItem(String itemId) {
        return getQuantity(itemId) > 0;
    }

    public long getTotalItems() {
        return totalItems.get();
    }

    public int getUniqueItemCount() {
        return uniqueItemCount.get();
    }

    public long capacity() {
        return capacity;
    }

    public long getRemainingCapacity() {
        return Math.max(0, capacity - totalItems.get());
    }

    public List<StoredItem> getItemsSorted() {
        List<StoredItem> result = new ArrayList<>();
        items.forEach((itemId, counter) -> {
            long quantity = counter.get();
            if (quantity > 0) {
                result.add(new StoredItem(itemId, quantity));
            }
        });
        result.sort(Comparator.comparingLong(StoredItem::quantity).reversed());
        return result;
    }

    public List<StoredItem> searchItems(String query) {
        if (query == null || query.isEmpty()) {
            return getItemsSorted();
        }
        String lowerQuery = query.toLowerCase();
        List<StoredItem> result = new ArrayList<>();
        items.forEach((itemId, counter) -> {
            if (!itemId.toLowerCase().contains(lowerQuery)) {
                return;
            }
            long quantity = counter.get();
            if (quantity > 0) {
                result.add(new StoredItem(itemId, quantity));
            }
        });
        result.sort(Comparator.comparingLong(StoredItem::quantity).reversed());
        return result;
    }

    public Map<String, Long> getItemsAsMap() {
        Map<String, Long> result = new HashMap<>();
        items.forEach((itemId, counter) -> {
            long quantity = counter.get();
            if (quantity > 0) {
                result.put(itemId, quantity);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    public void clear() {
        items.clear();
        totalItems.set(0);
        uniqueItemCount.set(0);
    }

    private AtomicLong getOrCreateCounterForDeposit(String itemId) {
        AtomicLong existing = items.get(itemId);
        if (existing != null) {
            return existing;
        }

        if (!tryIncrementUniqueCount()) {
            return null;
        }

        AtomicLong created = new AtomicLong(0);
        AtomicLong raced = items.putIfAbsent(itemId, created);
        if (raced != null) {
            uniqueItemCount.decrementAndGet();
            return raced;
        }
        return created;
    }

    private boolean tryIncrementUniqueCount() {
        while (true) {
            int current = uniqueItemCount.get();
            if (current >= MAX_UNIQUE_ITEMS) {
                return false;
            }
            if (uniqueItemCount.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    private long reserveCapacity(long desired) {
        while (true) {
            long currentTotal = totalItems.get();
            if (currentTotal >= capacity) {
                return 0;
            }
            long maxByCapacity = capacity - currentTotal;
            long toReserve = Math.min(desired, maxByCapacity);
            if (toReserve <= 0) {
                return 0;
            }
            if (totalItems.compareAndSet(currentTotal, currentTotal + toReserve)) {
                return toReserve;
            }
        }
    }

    private void discardIfEmpty(String itemId, AtomicLong counter) {
        if (counter.get() == 0 && items.remove(itemId, counter)) {
            uniqueItemCount.decrementAndGet();
        }
    }
}
