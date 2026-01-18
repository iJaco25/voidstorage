package net.momo.voidstorage.internal.transfer;

/** Filter modes for transfer item filtering. */
public enum FilterMode {
    WHITELIST("whitelist", "Whitelist"),
    BLACKLIST("blacklist", "Blacklist");

    private final String id;
    private final String displayName;

    FilterMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }

    public FilterMode toggle() {
        return this == WHITELIST ? BLACKLIST : WHITELIST;
    }

    public static FilterMode fromId(String id) {
        for (FilterMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return WHITELIST;
    }
}
