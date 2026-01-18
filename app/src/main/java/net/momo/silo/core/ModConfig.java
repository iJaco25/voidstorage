package net.momo.silo.core;

/** Single source of truth for mod identity. Change these values to rebrand the mod. */
public final class ModConfig {

    private ModConfig() {}

    // ==================== Mod Identity ====================
    public static final String NAMESPACE = "voidstorage";
    public static final String DISPLAY_NAME = "Void Storage";
    public static final String VERSION = "0.0.1";
    public static final String UI_PATH = "Pages";

    // ==================== Architecture Names ====================

    /** Name for the storage anchor (world-placed access point). Default: "Anchor" */
    public static final String ANCHOR_NAME = "Anchor";
    public static final String ANCHOR_NAME_PLURAL = "Anchors";

    /** Name for the transfer interface (item mover). Default: "Transfer" */
    public static final String TRANSFER_NAME = "Transfer";
    public static final String TRANSFER_NAME_PLURAL = "Transfers";

    /** Name for the input transfer mode. Default: "Input" */
    public static final String TRANSFER_INPUT_NAME = "Input";

    /** Name for the output transfer mode. Default: "Output" */
    public static final String TRANSFER_OUTPUT_NAME = "Output";

    /** Name for the access interface (player UI access). Default: "Covenant" */
    public static final String ACCESS_NAME = "Covenant";

    /** Name for the storage network. Default: "Network" */
    public static final String NETWORK_NAME = "Network";

    /** Name for the storage itself. Default: "Storage" */
    public static final String STORAGE_NAME = "Storage";

    // ==================== Utility Methods ====================

    /** Creates a namespaced key (e.g., "voidstorage:Anomaly_Core"). */
    public static String key(String localName) {
        return NAMESPACE + ":" + localName;
    }

    /** Strips namespace from a key, returning the local name. */
    public static String stripNamespace(String key) {
        if (key == null) return null;
        int idx = key.indexOf(':');
        return idx != -1 ? key.substring(idx + 1) : key;
    }

    /** Checks if a key belongs to this mod. */
    public static boolean isOwnKey(String key) {
        return key != null && key.startsWith(NAMESPACE + ":");
    }

    /** Returns the UI resource path for a page. */
    public static String uiPath(String page) {
        return UI_PATH + "/" + page;
    }
}
