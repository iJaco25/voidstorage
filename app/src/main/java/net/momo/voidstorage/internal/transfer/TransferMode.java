package net.momo.voidstorage.internal.transfer;

import net.momo.silo.core.ModConfig;

/** Transfer modes for network interfaces. */
public enum TransferMode {

    /** INPUT - pulls items from container into the storage network. */
    INPUT("input", true),

    /** OUTPUT - pushes items from the storage network into container. */
    OUTPUT("output", false);

    private final String id;
    private final boolean isInput;

    TransferMode(String id, boolean isInput) {
        this.id = id;
        this.isInput = isInput;
    }

    public String id() { return id; }
    public boolean isInput() { return isInput; }
    public boolean isOutput() { return !isInput; }

    /** Returns the display name from ModConfig. */
    public String displayName() {
        return isInput ? ModConfig.TRANSFER_INPUT_NAME : ModConfig.TRANSFER_OUTPUT_NAME;
    }

    public static TransferMode fromId(String id) {
        for (TransferMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return null;
    }
}
