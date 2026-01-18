package net.momo.voidstorage;

import net.momo.silo.core.ModConfig;

/** Item ID constants for Void Storage. Must match JSON identifiers. */
public final class VoidStorageItems {

    private VoidStorageItems() {}

    // Short names (used for comparison after stripping namespace)
    public static final String ANOMALY_CORE = "Anomaly_Core";
    public static final String ANOMALY_ANCHOR = "Anomaly_Anchor";
    public static final String VOID_BELL = "Void_Bell";
    public static final String SIGIL_ABSORPTION = "Sigil_Absorption";
    public static final String SIGIL_MANIFESTATION = "Sigil_Manifestation";
    public static final String VOID_ESSENCE = "Void_Essence";

    // Full namespaced IDs (used for block placement and item giving)
    public static final String ANOMALY_CORE_KEY = ModConfig.key(ANOMALY_CORE);
    public static final String SIGIL_ABSORPTION_KEY = ModConfig.key(SIGIL_ABSORPTION);
    public static final String SIGIL_MANIFESTATION_KEY = ModConfig.key(SIGIL_MANIFESTATION);
    public static final String VOID_ESSENCE_KEY = ModConfig.key(VOID_ESSENCE);

    public static boolean isModItem(String itemId) {
        if (itemId == null) return false;
        String id = stripNamespace(itemId);
        return ANOMALY_CORE.equals(id) 
            || ANOMALY_ANCHOR.equals(id)
            || VOID_BELL.equals(id)
            || SIGIL_ABSORPTION.equals(id)
            || SIGIL_MANIFESTATION.equals(id)
            || VOID_ESSENCE.equals(id);
    }

    public static boolean isVoidEssence(String itemId) {
        if (itemId == null) return false;
        return VOID_ESSENCE.equals(stripNamespace(itemId));
    }

    public static boolean isSigil(String itemId) {
        if (itemId == null) return false;
        String id = stripNamespace(itemId);
        return SIGIL_ABSORPTION.equals(id) || SIGIL_MANIFESTATION.equals(id);
    }

    public static String stripNamespace(String itemId) {
        return ModConfig.stripNamespace(itemId);
    }
}
