package net.momo.silo.util;

import java.text.DecimalFormat;

/** Formats large numbers with K/M/B suffixes. */
public final class NumberFormat {

    private static final DecimalFormat FORMATTER = new DecimalFormat("####0.#");

    private NumberFormat() {}

    public static String compact(long number) {
        if (number >= 1_000_000_000L) {
            return FORMATTER.format(number / 1_000_000_000.0) + "B";
        } else if (number >= 1_000_000L) {
            double val = number / 1_000_000.0;
            if (number > 100_000_000L) val = Math.round(val);
            return FORMATTER.format(val) + "M";
        } else if (number >= 1_000L) {
            double val = number / 1_000.0;
            if (number > 100_000L) val = Math.round(val);
            return FORMATTER.format(val) + "K";
        }
        return String.valueOf(number);
    }
}
