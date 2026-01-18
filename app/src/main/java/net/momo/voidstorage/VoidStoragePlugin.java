package net.momo.voidstorage;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import net.momo.silo.core.ModConfig;
import net.momo.silo.core.Services;
import net.momo.voidstorage.internal.connector.NetworkService;
import net.momo.silo.storage.StorageRegistry;
import net.momo.silo.interaction.HandlerRegistry;
import net.momo.silo.interaction.InteractionResult;
import net.momo.silo.interaction.MutableInteractionContext;
import net.momo.silo.interceptor.impl.RateLimitInterceptor;
import net.momo.silo.mechanic.MechanicRunner;
import net.momo.silo.ui.UIRegistry;
import net.momo.silo.persistence.JsonPersistence;
import net.momo.silo.persistence.PersistenceProvider;
import net.momo.silo.util.Position;
import net.momo.platform.hytale.adapter.InventoryAdapter;
import net.momo.platform.hytale.impl.HytaleWorldAdapter;
import net.momo.platform.hytale.interaction.AnomalyCoreInteraction;
import net.momo.platform.hytale.interaction.SigilAbsorptionInteraction;
import net.momo.platform.hytale.interaction.SigilConfigInteraction;
import net.momo.platform.hytale.interaction.SigilManifestationInteraction;
import net.momo.platform.hytale.interaction.VoidBellInteraction;
import net.momo.voidstorage.impl.interaction.AccessBellHandler;
import net.momo.voidstorage.impl.interaction.AnchorCoreHandler;
import net.momo.voidstorage.impl.interaction.TransferConfigHandler;
import net.momo.voidstorage.impl.interaction.TransferHandler;
import net.momo.voidstorage.impl.mechanic.NetworkVerifyMechanic;
import net.momo.voidstorage.impl.mechanic.TransferTickMechanic;
import net.momo.voidstorage.impl.ui.StoragePageProvider;
import net.momo.voidstorage.impl.ui.TransferConfigPageProvider;
import net.momo.voidstorage.internal.anchor.AnchorRegistry;
import net.momo.voidstorage.internal.anchor.AnchorStorageResolver;
import net.momo.voidstorage.internal.essence.OrphanedStorageRegistry;
import net.momo.voidstorage.internal.essence.VoidEssence;
import net.momo.voidstorage.internal.persistence.AnchorCodec;
import net.momo.voidstorage.internal.persistence.OrphanedStorageCodec;
import net.momo.voidstorage.internal.persistence.TransferCodec;
import net.momo.voidstorage.internal.transfer.TransferMode;
import net.momo.voidstorage.internal.transfer.TransferRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Void Storage - A magical logistics system for Hytale. */
public final class VoidStoragePlugin extends JavaPlugin {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private volatile boolean codecsRegistered;
    private ScheduledFuture<?> cleanupTask;
    private ScheduledFuture<?> orphanCleanupTask;

    public VoidStoragePlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    public CompletableFuture<Void> preLoad() {
        registerCodecs();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected void setup() {
        logger.at(Level.INFO).log("%s setting up...", ModConfig.DISPLAY_NAME);
        initializeServices();
        registerHandlers();
        registerMechanics();
        registerUI();
        registerCodecs();
        registerEvents();
        registerCommands();
    }

    @Override
    protected void start() {
        logger.at(Level.INFO).log("%s starting...", ModConfig.DISPLAY_NAME);
        loadData();
        Services.get(MechanicRunner.class).start();
        startCleanupTask();
        logger.at(Level.INFO).log("%s started successfully", ModConfig.DISPLAY_NAME);
    }

    @Override
    protected void shutdown() {
        logger.at(Level.INFO).log("%s shutting down...", ModConfig.DISPLAY_NAME);
        stopCleanupTask();
        Services.get(MechanicRunner.class).stop();
        saveData();
        Services.clear();
        logger.at(Level.INFO).log("%s shutdown complete", ModConfig.DISPLAY_NAME);
    }

    private void initializeServices() {
        AnchorRegistry anchorRegistry = new AnchorRegistry();
        TransferRegistry transferRegistry = new TransferRegistry();
        StorageRegistry storageRegistry = new StorageRegistry();

        Services.register(AnchorRegistry.class, anchorRegistry);
        Services.register(TransferRegistry.class, transferRegistry);
        Services.register(StorageRegistry.class, storageRegistry);
        Services.register(OrphanedStorageRegistry.class, new OrphanedStorageRegistry());
        Services.register(HandlerRegistry.class, new HandlerRegistry());
        Services.register(MechanicRunner.class, new MechanicRunner());
        Services.register(UIRegistry.class, new UIRegistry());

        // Persistence: Silo provides JsonPersistence, VoidStorage provides codecs
        OrphanedStorageRegistry orphanedStorageRegistry = Services.get(OrphanedStorageRegistry.class);
        JsonPersistence persistence = new JsonPersistence(getDataDirectory(), "network_storage.json")
            .bind("anchors", new AnchorCodec(),
                anchorRegistry::getAll,
                anchorRegistry::register)
            .bind("transfers", new TransferCodec(),
                transferRegistry::getAll,
                transferRegistry::register)
            .bind("orphaned", new OrphanedStorageCodec(),
                orphanedStorageRegistry::getAll,
                orphanedStorageRegistry::register);
        Services.register(PersistenceProvider.class, persistence);

        AnchorStorageResolver storageResolver = new AnchorStorageResolver(anchorRegistry, storageRegistry);
        NetworkService networkService = new NetworkService(storageResolver);
        networkService.setWindowOpener((playerId, storage) -> {
            Map<String, Object> params = new HashMap<>();
            params.put("storage", storage);
            Services.get(UIRegistry.class).open("storage", playerId, params);
        });
        Services.register(NetworkService.class, networkService);
    }

    private void registerHandlers() {
        HandlerRegistry registry = Services.get(HandlerRegistry.class);

        // Rate limit: 5 interactions per second per player
        registry.interceptors().register(
            RateLimitInterceptor.<MutableInteractionContext, InteractionResult>builder()
                .keyExtractor(ctx -> ctx.playerId().toString())
                .maxRequests(5)
                .window(1000)
                .rejectedResult(() -> InteractionResult.skipped("Rate limited"))
                .build()
        );

        registry.register(new AnchorCoreHandler());
        registry.register(new AccessBellHandler());
        registry.register(new TransferHandler(TransferMode.INPUT));
        registry.register(new TransferHandler(TransferMode.OUTPUT));
        registry.register(new TransferConfigHandler());
    }

    private void registerMechanics() {
        MechanicRunner runner = Services.get(MechanicRunner.class);
        runner.register(new TransferTickMechanic());
        runner.register(new NetworkVerifyMechanic());
    }

    private void registerUI() {
        UIRegistry registry = Services.get(UIRegistry.class);
        registry.register(new StoragePageProvider());
        registry.register(new TransferConfigPageProvider());
    }

    private void registerCodecs() {
        if (codecsRegistered) {
            return;
        }
        var registry = getCodecRegistry(Interaction.CODEC);
        registry.register(ModConfig.key("void_bell"), VoidBellInteraction.class, VoidBellInteraction.CODEC);
        registry.register(ModConfig.key("anomaly_core"), AnomalyCoreInteraction.class, AnomalyCoreInteraction.CODEC);
        registry.register(ModConfig.key("sigil_absorption"), SigilAbsorptionInteraction.class, SigilAbsorptionInteraction.CODEC);
        registry.register(ModConfig.key("sigil_manifestation"), SigilManifestationInteraction.class, SigilManifestationInteraction.CODEC);
        registry.register(ModConfig.key("sigil_config"), SigilConfigInteraction.class, SigilConfigInteraction.CODEC);
        codecsRegistered = true;
        logger.at(Level.INFO).log("Interactions registered");
    }

    private void registerEvents() {
        getEventRegistry().registerGlobal(PlayerConnectEvent.class,
            event -> logger.at(Level.INFO).log("Player connected: %s", event.getPlayerRef().getUuid()));

        getEventRegistry().registerGlobal(BreakBlockEvent.class, this::onBlockBreak);
        logger.at(Level.INFO).log("Events registered");
    }

    private void onBlockBreak(BreakBlockEvent event) {
        String blockType = event.getBlockType() != null ? event.getBlockType().getId() : null;
        if (blockType == null) return;

        var targetBlock = event.getTargetBlock();
        if (targetBlock == null) return;

        Position pos = Position.of(targetBlock.x, targetBlock.y, targetBlock.z);
        String blockId = ModConfig.stripNamespace(blockType);

        if (VoidStorageItems.ANOMALY_CORE.equals(blockId)) {
            // BreakBlockEvent doesn't include entity ref - storage orphaned without VoidEssence
            handleAnchorCoreBreak(pos, null);
        } else if (VoidStorageItems.SIGIL_ABSORPTION.equals(blockId) || VoidStorageItems.SIGIL_MANIFESTATION.equals(blockId)) {
            handleTransferBreak(pos);
        }
    }

    private void handleAnchorCoreBreak(Position pos, InventoryAdapter inventory) {
        AnchorRegistry anchorRegistry = Services.get(AnchorRegistry.class);
        anchorRegistry.getAtPosition(pos).ifPresent(anchor -> {
            // Break the structure blocks
            for (World w : Universe.get().getWorlds().values()) {
                var worldAdapter = new HytaleWorldAdapter(w);
                for (Position structurePos : AnchorCoreHandler.getStructurePositions(pos)) {
                    if (!structurePos.equals(pos)) {
                        worldAdapter.breakBlock(structurePos);
                    }
                }
                break;
            }

            UUID storageId = anchor.id();

            // Give VoidEssence to player instead of deleting storage
            if (inventory != null) {
                Map<String, Object> nbt = new HashMap<>();
                nbt.put(VoidEssence.NBT_STORAGE_ID, storageId.toString());
                nbt.put(VoidEssence.NBT_CREATED_AT, System.currentTimeMillis());

                boolean given = inventory.giveItemWithNbt(VoidStorageItems.VOID_ESSENCE_KEY, 1, nbt);
                if (given) {
                    // Mark storage as orphaned (don't delete it)
                    Services.get(OrphanedStorageRegistry.class).markOrphaned(storageId);
                    logger.at(Level.INFO).log("Gave VoidEssence for storage %s to player", storageId);
                } else {
                    // Failed to give item - don't orphan, keep storage active
                    logger.at(Level.WARNING).log("Failed to give VoidEssence, storage %s remains active", storageId);
                }
            } else {
                // No player - mark as orphaned anyway (e.g., explosion)
                Services.get(OrphanedStorageRegistry.class).markOrphaned(storageId);
                logger.at(Level.WARNING).log("No player to give VoidEssence, storage %s orphaned", storageId);
            }

            // Unregister anchor and transfers (but NOT the storage)
            anchorRegistry.unregister(storageId);
            Services.get(TransferRegistry.class).unregisterByAnchor(storageId);
            logger.at(Level.INFO).log("Removed anchor %s (block broken at %s)", storageId, pos);
        });
    }

    private void handleTransferBreak(Position pos) {
        TransferRegistry transferRegistry = Services.get(TransferRegistry.class);
        transferRegistry.getAtPosition(pos).ifPresent(transfer -> {
            transferRegistry.unregister(transfer.id());
            logger.at(Level.INFO).log("Removed transfer %s (block broken at %s)", transfer.id(), pos);
        });
    }

    private void registerCommands() {
        var commands = new net.momo.voidstorage.impl.command.VoidStorageCommands();
        getCommandRegistry().registerCommand(commands.createHelpCommand());
        getCommandRegistry().registerCommand(commands.createStatusCommand());
        logger.at(Level.FINE).log("Commands registered");
    }

    private void loadData() {
        try {
            Services.get(PersistenceProvider.class).load();
            logger.at(Level.INFO).log("Loaded %d anchors, %d transfers, %d orphaned storages",
                Services.get(AnchorRegistry.class).size(),
                Services.get(TransferRegistry.class).size(),
                Services.get(OrphanedStorageRegistry.class).size());
        } catch (Exception e) {
            logger.at(Level.SEVERE).withCause(e).log("Failed to load data");
        }
    }

    private void saveData() {
        try {
            Services.get(PersistenceProvider.class).save();
            logger.at(Level.INFO).log("Saved %d anchors, %d transfers, %d orphaned storages",
                Services.get(AnchorRegistry.class).size(),
                Services.get(TransferRegistry.class).size(),
                Services.get(OrphanedStorageRegistry.class).size());
        } catch (Exception e) {
            logger.at(Level.SEVERE).withCause(e).log("Failed to save data");
        }
    }

    private void startCleanupTask() {
        cleanupTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
            () -> Services.get(HandlerRegistry.class).cleanup(),
            5, 5, TimeUnit.MINUTES
        );

        // Orphan cleanup runs daily - cleans up storages past retention period
        orphanCleanupTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
            this::cleanupOrphanedStorages,
            1, 24, TimeUnit.HOURS
        );
    }

    private void cleanupOrphanedStorages() {
        try {
            OrphanedStorageRegistry orphanRegistry = Services.get(OrphanedStorageRegistry.class);
            StorageRegistry storageRegistry = Services.get(StorageRegistry.class);

            java.util.List<UUID> expired = orphanRegistry.cleanupExpired();
            for (UUID storageId : expired) {
                storageRegistry.unregister(storageId);
                logger.at(Level.INFO).log("Deleted expired orphaned storage %s", storageId);
            }

            if (!expired.isEmpty()) {
                logger.at(Level.INFO).log("Cleaned up %d expired orphaned storages", expired.size());
            }
        } catch (Exception e) {
            logger.at(Level.SEVERE).withCause(e).log("Failed to cleanup orphaned storages");
        }
    }

    private void stopCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel(false);
        }
        if (orphanCleanupTask != null) {
            orphanCleanupTask.cancel(false);
        }
    }
}
