package net.momo.silo.ui;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Registry for UI page providers. */
public final class UIRegistry {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private final Map<String, UIPageProvider> providers = new ConcurrentHashMap<>();

    /** Registers a UI page provider. */
    public void register(UIPageProvider provider) {
        providers.put(provider.pageType(), provider);
        logger.at(Level.FINE).log("Registered UI provider: %s", provider.pageType());
    }

    /** Unregisters a provider by page type. */
    public void unregister(String pageType) {
        providers.remove(pageType);
    }

    /** Gets a provider by page type. */
    public Optional<UIPageProvider> get(String pageType) {
        return Optional.ofNullable(providers.get(pageType));
    }

    /** Opens a UI page for a player. */
    public void open(String pageType, UUID playerId, Map<String, Object> params) {
        UIPageProvider provider = providers.get(pageType);
        if (provider == null) {
            logger.at(Level.WARNING).log("UI provider not found: %s", pageType);
            return;
        }
        provider.open(playerId, params);
    }

    /** Returns the number of registered providers. */
    public int size() {
        return providers.size();
    }
}
