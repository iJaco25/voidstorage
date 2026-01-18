# Multi-Server Synchronization Plan

## Overview

This document outlines the architecture for supporting multiple server instances and worlds sharing storage state with data reliability.

## Goals

1. **Multi-Instance** - Multiple server instances can share the same storage network
2. **Multi-World** - Different dimensions/worlds can share storage
3. **Data Reliability** - Consistency and durability guarantees
4. **Pluggable Backends** - Support Redis, database, or other sync mechanisms

---

## Architecture

### Layer 1: Sync Abstractions

New package: `net.momo.silo.sync/`

```
sync/
├── SyncProvider.java          # Core abstraction for sync backends
├── SyncEvent.java             # Immutable event representation
├── SyncEventType.java         # Event type enumeration
├── SyncSubscriber.java        # Event listener contract
├── ServerIdentity.java        # Server identification
└── conflict/
    ├── ConflictResolver.java  # Resolution strategy interface
    └── CrdtCounter.java       # CRDT for quantity merging
```

### Core Interfaces

#### SyncProvider

The main abstraction for sync backends:

```java
public interface SyncProvider {
    // Lifecycle
    CompletableFuture<Void> connect(ServerIdentity identity);
    CompletableFuture<Void> disconnect();
    boolean isConnected();

    // Event publication
    CompletableFuture<Void> publish(SyncEvent event);
    CompletableFuture<Void> publishBatch(List<SyncEvent> events);

    // Subscription
    void subscribe(SyncSubscriber subscriber);
    void unsubscribe(SyncSubscriber subscriber);

    // Distributed operations
    <T> CompletableFuture<T> executeAtomic(String lockKey, Supplier<T> operation, Duration timeout);
    CompletableFuture<Long> incrementCounter(String key, long delta);

    // State sync
    CompletableFuture<byte[]> getState(String key);
    CompletableFuture<Void> setState(String key, byte[] value, Duration ttl);
}
```

#### SyncEvent

Immutable event for cross-server communication:

```java
public record SyncEvent(
    UUID eventId,
    SyncEventType type,
    UUID entityId,
    String entityType,      // "anchor", "transfer", "storage"
    ServerIdentity origin,
    long timestamp,
    long vectorClock,       // For ordering
    byte[] payload          // Serialized delta
) {}
```

#### SyncEventType

```java
public enum SyncEventType {
    // Structural changes (VoidStorage-specific)
    ANCHOR_CREATED,
    ANCHOR_DELETED,
    TRANSFER_CREATED,
    TRANSFER_UPDATED,
    TRANSFER_DELETED,

    // Storage operations (generic Silo)
    STORAGE_DEPOSIT,
    STORAGE_WITHDRAW,
    STORAGE_SYNC_FULL,      // Full state transfer

    // Cluster management
    SERVER_JOIN,
    SERVER_LEAVE
}
```

#### ServerIdentity

```java
public record ServerIdentity(
    String serverId,
    String clusterId,
    String hostname,
    int port
) {}
```

---

## Consistency Model

### Per-Operation Consistency

| Operation              | Consistency             | Rationale                         |
| ---------------------- | ----------------------- | --------------------------------- |
| Anchor create/delete   | Strong                  | Structural changes need agreement |
| Transfer create/delete | Strong                  | Structural changes need agreement |
| Storage deposit        | Eventual                | Commutative, can merge via CRDT   |
| Storage withdraw       | Optimistic + Compensate | CAS with rollback on conflict     |
| Read operations        | Local                   | Performance, staleness acceptable |

### Consistency Levels

```java
public enum ConsistencyLevel {
    LOCAL,          // Read from local cache only
    EVENTUAL,       // Async propagation, no guarantees
    QUORUM,         // Majority of servers acknowledge
    STRONG          // All servers acknowledge (or leader-based)
}
```

---

## Conflict Resolution

### Strategy: CRDT for Quantities

For storage item quantities, use **Commutative Replicated Data Types**:

**Deposits (G-Counter):**

- Each server maintains its own deposit count
- Merge = take max per server, then sum
- Commutative: order doesn't matter

**Withdrawals (PN-Counter):**

- Track positive (deposits) and negative (withdrawals) per server
- Merge = merge both counters
- Net quantity = sum(deposits) - sum(withdrawals)

```java
public final class CrdtCounter {
    // Map<ServerId, Count>
    private final Map<String, Long> deposits;
    private final Map<String, Long> withdrawals;

    public long value() {
        return deposits.values().stream().mapToLong(Long::longValue).sum()
             - withdrawals.values().stream().mapToLong(Long::longValue).sum();
    }

    public CrdtCounter merge(CrdtCounter other) {
        // Take max of each server's count
    }
}
```

### Conflict Resolver Interface

```java
public interface ConflictResolver<T> {
    T resolve(T local, T remote, ConflictContext context);
    boolean requiresManualResolution(T local, T remote);
}
```

---

## Data Flow

### Deposit Operation (Eventual Consistency)

```
Server A                          Sync Layer                       Server B
   |                                  |                                |
   |--deposit(item, 100)-->           |                                |
   |  [local CAS succeeds]            |                                |
   |--publish(STORAGE_DEPOSIT)------->|                                |
   |                                  |--broadcast----------------->   |
   |                                  |                    [apply delta]
   |<--ack---------------------------|                                |
```

### Withdraw Operation (Optimistic + Compensate)

```
Server A                          Sync Layer                       Server B
   |                                  |                                |
   |--withdraw(item, 50)-->           |                                |
   |  [check local qty >= 50]         |                                |
   |  [local CAS: qty -= 50]          |                                |
   |--publish(STORAGE_WITHDRAW, v=1)->|                                |
   |                                  |--broadcast----------------->   |
   |                                  |           [concurrent withdraw]
   |                                  |<--publish(WITHDRAW, v=1)-------|
   |                                  |                                |
   |  [conflict detected: both v=1]   |                                |
   |  [resolver: A wins (lower UUID)] |                                |
   |                                  |--compensate(B, rollback)------>|
   |                                  |                    [restore qty]
```

---

## Integration

### SyncAwarePersistence

Wraps existing persistence with sync capabilities:

```java
public final class SyncAwarePersistence implements PersistenceProvider {
    private final PersistenceProvider local;    // WorldPersistence (VoidStorage)
    private final SyncProvider sync;
    private final ConflictResolver<?> resolver;

    @Override
    public void load() {
        // 1. Load from local file
        local.load();

        // 2. Fetch cluster state
        byte[] clusterState = sync.getState("storage").join();

        // 3. Merge with conflict resolution
        mergeState(clusterState);
    }

    @Override
    public void save() {
        // 1. Save locally
        local.save();

        // 2. Publish state to cluster
        sync.setState("storage", serializeState(), Duration.ofHours(1));
    }
}
```

### Registry Wrappers

Each registry gets a sync-aware wrapper. For VoidStorage-specific registries:

```java
public final class SyncAwareAnchorRegistry {
    private final AnchorRegistry delegate;  // voidstorage/internal/anchor/
    private final SyncProvider sync;

    public void register(StorageAnchor anchor) {
        // 1. Acquire distributed lock (for strong consistency)
        sync.executeAtomic("anchor:" + anchor.id(), () -> {
            // 2. Register locally
            delegate.register(anchor);

            // 3. Publish event
            sync.publish(new SyncEvent(
                UUID.randomUUID(),
                SyncEventType.ANCHOR_CREATED,
                anchor.id(),
                "anchor",
                localServer,
                System.currentTimeMillis(),
                vectorClock.increment(),
                serialize(anchor)
            ));

            return null;
        }, Duration.ofSeconds(5)).join();
    }
}
```

For generic Silo registries:

```java
public final class SyncAwareStorageRegistry {
    private final StorageRegistry delegate;  // silo/storage/
    private final SyncProvider sync;

    // Sync-aware storage operations
}
```

---

## Backend Implementations

### Phase 1: InMemorySyncProvider

For single-server and testing:

```java
public final class InMemorySyncProvider implements SyncProvider {
    // Local event bus, no actual distribution
    // Useful for development and testing
}
```

### Phase 2: RedisSyncProvider

For production multi-server:

- **Pub/Sub** for event propagation
- **Sorted Sets** for vector clocks
- **Lua Scripts** for atomic counter operations
- **Key pattern**: `silo:{cluster}:{entity_type}:{entity_id}`

### Phase 3: DatabaseSyncProvider

For environments without Redis:

- **Outbox pattern** for reliable event publishing
- **Polling or CDC** for event consumption
- **Optimistic locking** with version columns

---

## Configuration

```java
public record SyncConfig(
    String clusterId,
    ServerIdentity localServer,
    SyncBackendType backendType,        // MEMORY, REDIS, DATABASE
    ConsistencyLevel defaultConsistency,
    Duration eventRetention,
    int maxBatchSize,
    Duration syncInterval,
    boolean enableCompression
) {}
```

---

## Multi-World Support

### World-Scoped Identifiers

```java
public record WorldPosition(
    String worldId,      // Dimension/world identifier
    Position position
) {
    public long toGlobalKey() {
        return worldId.hashCode() ^ position.toKey();
    }
}
```

### Partitioning Strategy

- Each world can be a separate sync partition
- Cross-world anchors share storage but have world-local position indexes
- Event routing based on world ID for efficiency

---

## Implementation Phases

### Phase 1: Interfaces (Current)

- Create `silo/sync/` package structure
- Define all interfaces and records
- Create `InMemorySyncProvider` for testing

### Phase 2: Integration

- Create `SyncAwarePersistence`
- Add sync hooks to registries (both Silo and VoidStorage)
- Implement CRDT counters for storage

### Phase 3: Redis Backend

- Implement `RedisSyncProvider`
- Add Lua scripts for atomic operations
- Test multi-server scenarios

### Phase 4: Database Backend (Optional)

- Implement `DatabaseSyncProvider`
- Add outbox table and polling
- Support SQL and NoSQL options

---

## Files to Create

### Silo (Generic Sync Framework)

| File                                          | Purpose                 |
| --------------------------------------------- | ----------------------- |
| `silo/sync/SyncProvider.java`                 | Core abstraction        |
| `silo/sync/SyncEvent.java`                    | Event record            |
| `silo/sync/SyncEventType.java`                | Event types enum        |
| `silo/sync/SyncSubscriber.java`               | Listener interface      |
| `silo/sync/ServerIdentity.java`               | Server ID record        |
| `silo/sync/SyncConfig.java`                   | Configuration record    |
| `silo/sync/ConsistencyLevel.java`             | Consistency enum        |
| `silo/sync/conflict/ConflictResolver.java`    | Resolution interface    |
| `silo/sync/conflict/CrdtCounter.java`         | CRDT implementation     |
| `silo/sync/backend/InMemorySyncProvider.java` | Test/single-server impl |

### VoidStorage (Sync Wrappers for World-Specific Registries)

| File                                                       | Purpose                    |
| ---------------------------------------------------------- | -------------------------- |
| `voidstorage/internal/sync/SyncAwareAnchorRegistry.java`   | Sync wrapper for anchors   |
| `voidstorage/internal/sync/SyncAwareTransferRegistry.java` | Sync wrapper for transfers |

---

## Trade-offs

| Aspect              | Choice                | Trade-off                              |
| ------------------- | --------------------- | -------------------------------------- |
| Default consistency | Eventual for storage  | Higher throughput, brief inconsistency |
| Conflict resolution | CRDT for quantities   | Automatic merge, rare data loss        |
| Event delivery      | At-least-once         | Requires idempotent handlers           |
| Partition tolerance | Prefer availability   | Servers operate during partition       |
| State sync          | Delta + periodic full | Bandwidth efficient                    |
