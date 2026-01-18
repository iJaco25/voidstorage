package net.momo.silo.util;

import java.math.BigInteger;
import java.util.UUID;
import java.util.regex.Pattern;

/** Input validation utilities. Validate at system boundaries. */
public final class Validation {

    private Validation() {}

    private static final Pattern ITEM_ID_PATTERN =
        Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,63}$");

    public static boolean isValidItemId(String itemId) {
        return itemId != null && ITEM_ID_PATTERN.matcher(itemId).matches();
    }

    public static void requireValidItemId(String itemId) {
        if (!isValidItemId(itemId)) {
            throw new IllegalArgumentException("Invalid item ID: " + itemId);
        }
    }

    public static void requireNonNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    public static void requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive, was " + value);
        }
    }


    public static void requirePositive(BigInteger value, String name) {
        if (value == null || value.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException(name + " must be positive, was " + value);
        }
    }

    public static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative, was " + value);
        }
    }

    public static void requireInRange(long value, long min, long max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("%s must be between %d and %d, was %d", name, min, max, value));
        }
    }

    public static UUID parseUUID(String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
