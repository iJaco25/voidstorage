package net.momo.voidstorage.impl.interaction;

import com.hypixel.hytale.logger.HytaleLogger;
import net.momo.silo.core.Services;
import net.momo.voidstorage.internal.connector.NetworkService;
import net.momo.silo.interaction.InteractionHandler;
import net.momo.silo.interaction.InteractionResult;
import net.momo.platform.hytale.adapter.InteractionContextAdapter;
import net.momo.silo.util.Position;
import net.momo.silo.util.Result;

import java.util.UUID;
import java.util.logging.Level;

/** Handles Access Bell interactions to open the storage window. */
public final class AccessBellHandler implements InteractionHandler {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0001-000000000001");

    @Override
    public UUID id() {
        return ID;
    }

    @Override
    public InteractionResult handle(InteractionContextAdapter context) {
        UUID playerId = context.getPlayerId();
        if (playerId == null) {
            return InteractionResult.skipped("No player");
        }

        Position playerPos = context.getPlayerPosition();
        if (playerPos == null) {
            return InteractionResult.skipped("No player position");
        }

        NetworkService networkService = Services.get(NetworkService.class);
        Result<Void> result = networkService.openStorageWindow(playerId, playerPos);

        if (result.isFailure()) {
            logger.at(Level.INFO).log("Player %s failed to access storage: %s", playerId, result.error());
            return InteractionResult.skipped(result.error());
        }

        return InteractionResult.success();
    }
}
