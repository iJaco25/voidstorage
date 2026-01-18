package net.momo.silo.ui;

import java.util.Map;
import java.util.UUID;

/** Contract for UI page providers. Register with UIRegistry. */
public interface UIPageProvider {

    /** Page type identifier (e.g., "storage", "sigil_config"). */
    String pageType();

    /** Opens the page for a player with optional parameters. */
    void open(UUID playerId, Map<String, Object> params);
}
