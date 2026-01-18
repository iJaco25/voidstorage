# VoidStorage Architecture

## Overview

VoidStorage is a modular storage network mod for Hytale. The architecture cleanly separates:

- **Silo** (`net.momo.silo`) - Generic storage framework (game-agnostic, reusable)
- **Platform** (`net.momo.platform`) - Game engine adapters
- **VoidStorage** (`net.momo.voidstorage`) - World-based storage network implementation

## Design Philosophy

Silo provides **generic building blocks** for storage systems. VoidStorage extends these to create a **world-based storage network** with anchors, transfers, and network mechanics.

**Framework vs Implementation:**

| Layer       | Provides                                  | Knows About                   |
| ----------- | ----------------------------------------- | ----------------------------- |
| Silo        | Storage, Persistence, Extension points    | Generic storage operations    |
| VoidStorage | Anchors, Transfers, Codecs, Network logic | World blocks, spatial queries |

## Package Structure

```
net.momo/
├── silo/                           # Framework (reusable for any storage mod)
│   ├── core/                       # ModConfig, Services
│   ├── interaction/                # InteractionHandler, HandlerRegistry
│   ├── mechanic/                   # TickMechanic, MechanicRunner
│   ├── interceptor/                # Interceptor chain system
│   ├── ui/                         # UIPageProvider, UIRegistry
│   ├── storage/                    # Storage, StorageRegistry
│   ├── persistence/                # Codec, JsonPersistence, PersistenceProvider
│   └── util/                       # ObjectPool, utilities
│
├── platform/
│   └── hytale/                     # Hytale SDK adapters
│       ├── adapter/                # Abstract adapters
│       ├── impl/                   # Hytale implementations
│       └── interaction/            # Hytale interaction bindings
│
└── voidstorage/                    # This mod (world-based storage network)
    ├── VoidStoragePlugin.java      # Bootstrap
    ├── VoidStorageItems.java       # Item IDs
    ├── util/                       # Utilities
    ├── internal/                   # VoidStorage-specific internals
    │   ├── anchor/                 # StorageAnchor, AnchorRegistry, AnchorStorageResolver
    │   ├── transfer/               # Transfer, TransferRegistry, TransferMode
    │   ├── connector/              # StorageResolver, NetworkService, NetworkInterface
    │   └── persistence/            # AnchorCodec, TransferCodec
    └── impl/                       # Extension implementations
        ├── interaction/            # Handlers
        ├── mechanic/               # Mechanics
        ├── ui/                     # UI providers
        └── command/                # Commands
```

## Layers

### Silo Framework (`net.momo.silo`)

Generic storage framework. Game-agnostic, scope-agnostic.

#### Core (`silo/core/`)

| File             | Purpose                                                 |
| ---------------- | ------------------------------------------------------- |
| `ModConfig.java` | Single source of truth for mod identity and terminology |
| `Services.java`  | Service locator for dependency injection                |

#### Storage (`silo/storage/`)

Generic storage container and registry.

| File                   | Purpose                                     |
| ---------------------- | ------------------------------------------- |
| `Storage.java`         | Thread-safe storage container with capacity |
| `StorageRegistry.java` | Registry for storage instances (by UUID)    |
| `StoredItem.java`      | Item representation in storage              |

#### Extension Points

**Interaction (`silo/interaction/`)** - Player interaction handling

| File                             | Purpose                               |
| -------------------------------- | ------------------------------------- |
| `InteractionHandler.java`        | Interface for handlers (UUID id)      |
| `InteractionResult.java`         | Handler result                        |
| `HandlerRegistry.java`           | Dispatches to handlers with intercept |
| `MutableInteractionContext.java` | Poolable context for interceptors     |

**Mechanic (`silo/mechanic/`)** - Game loop mechanics

| File                      | Purpose                                |
| ------------------------- | -------------------------------------- |
| `TickMechanic.java`       | Interface for tick mechanics (UUID id) |
| `MechanicRunner.java`     | Schedules and runs mechanics           |
| `MutableTickContext.java` | Poolable context for interceptors      |

**Interceptor (`silo/interceptor/`)** - Chain-of-responsibility middleware

| File                       | Purpose                                   |
| -------------------------- | ----------------------------------------- |
| `Interceptor.java`         | Core interface with UUID id and priority  |
| `InterceptorChain.java`    | Chain continuation interface              |
| `InterceptorRegistry.java` | Thread-safe registry with chain execution |

**UI (`silo/ui/`)** - User interface pages

| File                  | Purpose                   |
| --------------------- | ------------------------- |
| `UIPageProvider.java` | Interface for UI pages    |
| `UIRegistry.java`     | Registry for UI providers |

**Persistence (`silo/persistence/`)** - Data persistence

| File                       | Purpose                                             |
| -------------------------- | --------------------------------------------------- |
| `PersistenceProvider.java` | Interface for persistence (load/save)               |
| `Codec.java`               | Interface for serialize/deserialize                 |
| `JsonPersistence.java`     | JSON file persistence with backup and atomic writes |

#### Utilities (`silo/util/`)

| File              | Purpose                                  |
| ----------------- | ---------------------------------------- |
| `ObjectPool.java` | Thread-safe object pool for GC reduction |

### VoidStorage Internal (`net.momo.voidstorage.internal`)

World-specific storage network components.

#### Anchor (`internal/anchor/`)

| File                         | Purpose                             |
| ---------------------------- | ----------------------------------- |
| `StorageAnchor.java`         | World block that owns a storage     |
| `AnchorRegistry.java`        | Sharded registry for anchors        |
| `AnchorStorageResolver.java` | Resolves storage via nearest anchor |

#### Transfer (`internal/transfer/`)

| File                    | Purpose                        |
| ----------------------- | ------------------------------ |
| `Transfer.java`         | Transfer interface entity      |
| `TransferMode.java`     | INPUT or OUTPUT                |
| `TransferRegistry.java` | Sharded registry for transfers |

#### Connector (`internal/connector/`)

| File                         | Purpose                                     |
| ---------------------------- | ------------------------------------------- |
| `StorageResolver.java`       | Interface for resolving storage (pluggable) |
| `AnchorStorageResolver.java` | Resolves storage via nearest anchor         |
| `NetworkService.java`        | Service for opening storage windows         |

#### Persistence (`internal/persistence/`)

| File                 | Purpose                           |
| -------------------- | --------------------------------- |
| `AnchorCodec.java`   | Serializes/deserializes anchors   |
| `TransferCodec.java` | Serializes/deserializes transfers |

### Platform (`net.momo.platform`)

Game engine adapters. Isolates framework from engine specifics.

#### Hytale (`platform/hytale/`)

```
hytale/
├── adapter/         # Abstract adapters (WorldAdapter, ContainerAdapter, etc.)
├── impl/            # Hytale-specific implementations
└── interaction/     # Hytale interaction wrappers
```

### VoidStorage Implementations (`net.momo.voidstorage.impl`)

Extension point implementations.

**Handlers** (`impl/interaction/`):

- `AnchorCoreHandler` - Creates storage anchors
- `AccessBellHandler` - Opens storage UI
- `TransferHandler` - Places transfer interfaces
- `TransferConfigHandler` - Configures transfer filters

**Mechanics** (`impl/mechanic/`):

- `TransferTickMechanic` - Moves items between containers and storage
- `NetworkVerifyMechanic` - Verifies blocks still exist in world

**UI Providers** (`impl/ui/`):

- `StoragePageProvider` - Storage browsing UI
- `TransferConfigPageProvider` - Transfer filter configuration UI

## Persistence Pattern

Silo provides persistence infrastructure. VoidStorage provides codecs for its domain objects.

### Silo Provides

- `PersistenceProvider` - interface with `load()` and `save()`
- `Codec<T>` - interface with `serialize()` and `deserialize()`
- `JsonPersistence` - JSON file persistence with backup and atomic writes

### VoidStorage Provides

- `AnchorCodec` - serializes/deserializes `StorageAnchor`
- `TransferCodec` - serializes/deserializes `Transfer`

### Usage

```java
// VoidStorage wires codecs to Silo's JsonPersistence
JsonPersistence persistence = new JsonPersistence(dataDir, "network_storage.json")
    .bind("anchors", new AnchorCodec(),
        anchorRegistry::getAll,
        anchorRegistry::register)
    .bind("transfers", new TransferCodec(),
        transferRegistry::getAll,
        transferRegistry::register);
```

## StorageResolver Pattern (VoidStorage)

VoidStorage defines how storage is resolved for players. This is implementation-specific, not part of Silo.

```java
@FunctionalInterface
public interface StorageResolver {
    Optional<Storage> resolve(UUID playerId, Position position);
}
```

VoidStorage uses `AnchorStorageResolver` which finds the nearest anchor within range and returns its associated storage.

## Data Flow

```
Player Interaction
       │
       ▼
┌─────────────────┐
│ Platform Layer  │  Hytale interaction wrappers
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ HandlerRegistry │  Dispatches to appropriate handler
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Interceptors    │  Pre/post processing middleware
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Handler         │  Business logic (e.g., AccessBellHandler)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ NetworkService  │  Uses StorageResolver
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ StorageResolver │  AnchorStorageResolver (or PlayerStorageResolver)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Storage         │  Updates item quantities
└─────────────────┘
```

## Rebranding

All user-facing terminology is configurable in `ModConfig.java`:

```java
public static final String ANCHOR_NAME = "Anchor";      // or "Nexus", "Core"
public static final String TRANSFER_NAME = "Transfer";  // or "Conduit", "Pipe"
public static final String ACCESS_NAME = "Covenant";    // or "Terminal", "Interface"
public static final String STORAGE_NAME = "Storage";    // or "Vault", "Cache"
```

## Adding New Features

### New Interaction Handler

1. Create class implementing `InteractionHandler` in `impl/interaction/`
2. Register in `VoidStoragePlugin.registerHandlers()`
3. Create Hytale interaction wrapper in `platform/hytale/interaction/`

```java
public final class MyHandler implements InteractionHandler {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0001-000000000010");

    @Override public UUID id() { return ID; }

    @Override public InteractionResult handle(InteractionContextAdapter ctx) {
        return InteractionResult.success();
    }
}
```

### New Tick Mechanic

1. Create class implementing `TickMechanic` in `impl/mechanic/`
2. Register in `VoidStoragePlugin.registerMechanics()`

```java
public final class MyMechanic implements TickMechanic {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0002-000000000010");

    @Override public UUID id() { return ID; }
    @Override public long intervalMs() { return 1000; }
    @Override public void tick(WorldAdapter world) { }
}
```

### New Interceptor

1. Create class implementing `Interceptor<C, R>` in `impl/interceptor/`
2. Register with appropriate registry (`HandlerRegistry` or `MechanicRunner`)

```java
public final class LoggingInterceptor
        implements Interceptor<MutableInteractionContext, InteractionResult> {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0003-000000000001");

    @Override public UUID id() { return ID; }
    @Override public int priority() { return 100; } // Higher = executes first

    @Override
    public InteractionResult intercept(
            MutableInteractionContext ctx,
            InterceptorChain<MutableInteractionContext, InteractionResult> chain) {
        // Pre-processing
        long start = System.nanoTime();

        // Continue chain (or return early to short-circuit)
        InteractionResult result = chain.proceed(ctx);

        // Post-processing
        long elapsed = System.nanoTime() - start;
        logger.info("Handler {} took {}ns", ctx.handler().id(), elapsed);

        return result;
    }
}
```

### New StorageResolver

1. Create class implementing `StorageResolver`
2. Configure in `VoidStoragePlugin` when creating `NetworkService`

```java
public final class MyStorageResolver implements StorageResolver {
    @Override
    public Optional<Storage> resolve(UUID playerId, Position position) {
        // Custom resolution logic
        return storageRegistry.get(computeStorageId(playerId, position));
    }
}
```

### New UI Page

1. Create class implementing `UIPageProvider` in `impl/ui/`
2. Register in `VoidStoragePlugin.registerUI()`

```java
public final class MyPageProvider implements UIPageProvider {
    @Override public String pageType() { return "my_page"; }
    @Override public void open(UUID playerId, Map<String, Object> params) { }
}
```

## Object Pooling

To reduce GC pressure on hot paths, the framework uses object pooling for context objects.

### Poolable Context Classes

- `MutableInteractionContext` - Used by `HandlerRegistry` for interaction dispatch
- `MutableTickContext` - Used by `MechanicRunner` for tick dispatch

### ObjectPool Usage

```java
// Create pool with factory, reset function, and max size
ObjectPool<MutableInteractionContext> pool = new ObjectPool<>(
    MutableInteractionContext::new,    // Factory
    MutableInteractionContext::reset,  // Reset before returning to pool
    64                                 // Max pooled objects
);

// Acquire from pool (creates new if empty)
MutableInteractionContext ctx = pool.acquire();

// Set values for this use
ctx.set(handler, adapter, playerId);

// Use the context...
InteractionResult result = doWork(ctx);

// Return to pool (reset is called automatically)
pool.release(ctx);
```

### Design Principles

- Mutable contexts have no-arg constructors for pooling
- `set()` method initializes all fields (returns `this` for fluent use)
- `reset()` nulls all references for safe pooling
- Pool uses `ConcurrentLinkedQueue` for lock-free thread safety
- Max size prevents unbounded memory growth

## Persistence

Data is saved to JSON via `WorldPersistence`:

```
data/
└── network_storage.json    # Anchors, transfers, and stored items
```

## Thread Safety

- All registries use `ConcurrentHashMap`
- `AnchorRegistry` and `TransferRegistry` use sharded `StampedLock` for scalability
- `Storage` uses atomic operations for item counts
- `MechanicRunner` prevents concurrent tick execution per mechanic
- `InterceptorRegistry` uses `ConcurrentHashMap` with volatile sorted chain
- `ObjectPool` uses `ConcurrentLinkedQueue` for lock-free pooling

## UUID Identifiers

All extension point interfaces use `UUID id()` instead of String for type safety:

```java
public interface InteractionHandler {
    UUID id();  // Unique identifier
    InteractionResult handle(InteractionContextAdapter context);
}
```

Implementations use static UUID constants for efficiency:

```java
private static final UUID ID = UUID.fromString("00000000-0000-0000-0001-000000000001");
@Override public UUID id() { return ID; }
```

UUID format convention:

- `0001-*` - Interaction handlers
- `0002-*` - Tick mechanics
- `0003-*` - Interceptors
