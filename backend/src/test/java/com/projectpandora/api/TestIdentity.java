package com.projectpandora.api;

import java.util.Locale;

/** Checksum-valid fixture numbers for isolated test databases. */
final class TestIdentity {
    private TestIdentity() {}

    static String card(int serial) {
        return card("19900501", serial);
    }

    static String card(String date, int serial) {
        String digits = "110105" + date + String.format(Locale.ROOT, "%03d", serial);
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        int sum = 0;
        for (int i = 0; i < weights.length; i++) sum += (digits.charAt(i) - '0') * weights[i];
        return digits + "10X98765432".charAt(sum % 11);
    }
}
