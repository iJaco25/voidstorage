package net.momo.silo.util;

import java.util.Objects;

/** Immutable 3D position with efficient encoding for spatial lookups. */
public final class Position {

    private final int x;
    private final int y;
    private final int z;

    private Position(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Position of(int x, int y, int z) {
        return new Position(x, y, z);
    }

    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }

    public Position offset(int dx, int dy, int dz) {
        return new Position(x + dx, y + dy, z + dz);
    }

    public Position above() { return offset(0, 1, 0); }
    public Position below() { return offset(0, -1, 0); }

    /** Encode position to a single long key for O(1) map lookups. */
    public long toKey() {
        return encode(x, y, z);
    }

    public static long encode(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF)
             | (((long) z & 0x3FFFFFF) << 26)
             | (((long) y & 0xFFF) << 52);
    }

    public static Position fromKey(long key) {
        int x = (int) ((key << 38) >> 38);
        int z = (int) ((key << 12) >> 38);
        int y = (int) ((key >> 52) & 0xFFF);
        return Position.of(x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return x == p.x && y == p.y && z == p.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("(%d, %d, %d)", x, y, z);
    }
}
