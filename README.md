# VoidStorage

A modular storage network mod for Hytale, built on the **Silo** storage framework.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         Silo                                │
│              (Generic Storage Framework)                    │
│                                                             │
│  ┌─────────┐  ┌─────────────┐  ┌──────────────────────┐    │
│  │ Storage │  │ StorageReg. │  │ PersistenceProvider  │    │
│  │ (items) │  │ (UUID→Store)│  │ (save/load)          │    │
│  └─────────┘  └─────────────┘  └──────────────────────┘    │
│                                                             │
│  Extension Points: InteractionHandler, TickMechanic,        │
│                    Interceptor, UIPageProvider              │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ extends
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      VoidStorage                            │
│                  (World-Based Storage Mod)                  │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ StorageAnchor│  │ Transfer     │  │ NetworkService   │  │
│  │ (world block)│  │ (auto-move)  │  │ (resolve+open)   │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
│                                                             │
│  Handlers: AnchorCore, Transfer, AccessBell                 │
│  Mechanics: TransferTick, NetworkVerify                     │
└─────────────────────────────────────────────────────────────┘
```

**Silo** provides generic storage primitives. **VoidStorage** extends Silo with world-based anchors, transfers, and block interactions.

## Features

- **Centralized Storage** - Store thousands of items in a single network
- **Remote Access** - Access storage from anywhere within range via Access Bells
- **Auto-Transfer** - Sigils automatically move items between containers and storage
- **Filtering** - Configure which items transfer in/out
- **Extensible** - Interceptor middleware for custom logic
- **Multi-Scope** - Framework supports world-based, player-based, or API-based storage

## Blocks & Items

| Item                       | Purpose                                           |
| -------------------------- | ------------------------------------------------- |
| **Anomaly Core**           | Creates a storage anchor - the heart of a network |
| **Sigil of Absorption**    | Auto-transfers items FROM container TO storage    |
| **Sigil of Manifestation** | Auto-transfers items FROM storage TO container    |
| **Access Bell**            | Opens storage UI when interacted                  |

## Quick Start

1. **Create a Storage Network**
    - Right-click with an Anomaly Core to place an anchor
    - This creates a storage network with 100,000 item capacity

2. **Access Your Storage**
    - Place an Access Bell within range of the anchor
    - Right-click the bell to open the storage UI

3. **Automate Transfers**
    - Place a container (chest, barrel, etc.)
    - Right-click the TOP of the container with a Sigil
    - Sigil of Absorption: pulls items into storage
    - Sigil of Manifestation: pushes items from storage

4. **Configure Filters**
    - Right-click a placed Sigil to configure item filters
    - Whitelist or blacklist specific items

## Silo Framework

Silo is a **generic storage framework** that can be used independently of VoidStorage.

### Core Components

| Component         | Purpose                                          |
| ----------------- | ------------------------------------------------ |
| `Storage`         | Thread-safe item container with bounded capacity |
| `StorageRegistry` | Maps UUIDs to Storage instances                  |
| `JsonPersistence` | JSON file persistence with backup/atomic writes  |
| `Codec<T>`        | Serialize/deserialize interface                  |

### Extension Points

| Interface             | Purpose                            |
| --------------------- | ---------------------------------- |
| `InteractionHandler`  | Handle player interactions         |
| `TickMechanic`        | Periodic game loop tasks           |
| `Interceptor<C,R>`    | Middleware for pre/post processing |
| `UIPageProvider`      | Custom UI pages                    |
| `PersistenceProvider` | Save/load interface                |

## VoidStorage Implementation

VoidStorage extends Silo with world-based storage networks:

### Domain Objects

| Class              | Purpose                                              |
| ------------------ | ---------------------------------------------------- |
| `StorageAnchor`    | World block that owns a storage instance             |
| `AnchorRegistry`   | Spatial registry with sharded locks                  |
| `Transfer`         | Block that moves items between container and storage |
| `TransferRegistry` | Registry for transfers by position/anchor            |
| `NetworkService`   | Resolves and opens storage for players               |
| `AnchorCodec`      | Serializes/deserializes anchors for persistence      |
| `TransferCodec`    | Serializes/deserializes transfers for persistence    |

### Handlers

- `AnchorCoreHandler` - Creates storage anchors from Anomaly Core
- `AccessBellHandler` - Opens storage UI via NetworkService
- `TransferHandler` - Places transfer blocks (absorption/manifestation)
- `TransferConfigHandler` - Opens filter configuration UI

### Mechanics

- `TransferTickMechanic` - Moves items every 500ms
- `NetworkVerifyMechanic` - Removes orphaned anchors/transfers

## Building

### Prerequisites

- JDK 25
- Hytale Server Files (`HytaleServer.jar` and `Assets.zip` in `libs/`)

### Build

```bash
./gradlew build
```

JAR output: `app/build/libs/`

### Run Server

```bash
./gradlew runServer
```

## Project Structure

```
net.momo/
├── silo/                           # Generic storage framework
│   ├── core/                       # ModConfig, Services
│   ├── storage/                    # Storage, StorageRegistry, StoredItem
│   ├── interaction/                # HandlerRegistry, InteractionHandler
│   ├── mechanic/                   # MechanicRunner, TickMechanic
│   ├── interceptor/                # Interceptor chain system
│   ├── ui/                         # UIRegistry, UIPageProvider
│   ├── persistence/                # PersistenceProvider interface
│   └── util/                       # ObjectPool, Position, Result
│
├── platform/hytale/                # Hytale SDK adapters
│
└── voidstorage/                    # This mod's implementation
    ├── internal/
    │   ├── anchor/                 # StorageAnchor, AnchorRegistry, AnchorStorageResolver
    │   ├── transfer/               # Transfer, TransferRegistry
    │   ├── connector/              # NetworkService, StorageResolver
    │   └── persistence/            # AnchorCodec, TransferCodec
    ├── impl/
    │   ├── interaction/            # Handlers
    │   ├── mechanic/               # Mechanics
    │   └── ui/                     # UI providers
    └── VoidStoragePlugin.java      # Bootstrap
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md) - Detailed architecture documentation
- [Sync Plan](docs/SYNC_PLAN.md) - Multi-server synchronization design
