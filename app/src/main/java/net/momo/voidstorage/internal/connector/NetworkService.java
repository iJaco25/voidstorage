package net.momo.voidstorage.internal.connector;

import com.hypixel.hytale.logger.HytaleLogger;
import net.momo.silo.storage.Storage;
import net.momo.silo.util.Position;
import net.momo.silo.util.Result;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/** Manages storage access and operations via pluggable resolver. */
public final class NetworkService {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private final StorageResolver storageResolver;
    private BiConsumer<UUID, Storage> windowOpener;

    /** Creates a NetworkService with a storage resolver. */
    public NetworkService(StorageResolver storageResolver) {
        this.storageResolver = storageResolver;
    }

    public void setWindowOpener(BiConsumer<UUID, Storage> opener) {
        this.windowOpener = opener;
    }

    public Result<Void> openStorageWindow(UUID playerId, Position position) {
        logger.at(Level.INFO).log("Request to open storage window for %s at %s", playerId, position);

        if (windowOpener == null) {
            logger.at(Level.SEVERE).log("Window opener not initialized");
            return Result.failure("Window opener not initialized");
        }

        Optional<Storage> storage = storageResolver.resolve(playerId, position);
        if (storage.isEmpty()) {
            logger.at(Level.INFO).log("No storage available for %s at %s", playerId, position);
            return Result.failure("No storage available");
        }

        logger.at(Level.INFO).log("Opening storage for player %s", playerId);
        windowOpener.accept(playerId, storage.get());
        return Result.success(null);
    }

    /** Returns the storage resolver used by this service. */
    public StorageResolver storageResolver() {
        return storageResolver;
    }
}
