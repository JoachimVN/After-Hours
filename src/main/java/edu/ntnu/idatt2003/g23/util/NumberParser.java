package edu.ntnu.idatt2003.g23.util;

import java.math.BigDecimal;

/**
 * Parses a user-supplied numeric string that may carry a magnitude suffix:
 * {@code K} / {@code k} → ×1 000, {@code M} / {@code m} → ×1 000 000,
 * {@code B} / {@code b} → ×1 000 000 000.
 * <p>
 * Comma handling: if there is exactly one comma, no period, and the digits after
 * the comma are <em>not</em> exactly 3, the comma is treated as a decimal separator
 * ({@code 1,3k} → 1 300, {@code 1,30k} → 1 300 — wait, 1.30 × 1000 = 1300).
 * If the group after the lone comma is exactly 3 digits, it is treated as a
 * thousands separator ({@code 1,300k} → 1 300 000). Multiple commas are always
 * thousands separators ({@code 1,000,000}).
 * </p>
 */
public final class NumberParser {

    private NumberParser() {}

    /**
     * Parses {@code text} and returns a {@link BigDecimal}.
     *
     * @throws NumberFormatException if {@code text} is empty or not a valid number
     */
    public static BigDecimal parse(String text) {
        String s = text.trim();
        if (s.isEmpty()) throw new NumberFormatException("empty");

        // Strip suffix before deciding comma handling
        char lastChar = Character.toUpperCase(s.charAt(s.length() - 1));
        BigDecimal multiplier = switch (lastChar) {
            case 'K' -> BigDecimal.valueOf(1_000);
            case 'M' -> BigDecimal.valueOf(1_000_000);
            case 'B' -> BigDecimal.valueOf(1_000_000_000);
            default  -> BigDecimal.ONE;
        };
        String digits = multiplier.equals(BigDecimal.ONE) ? s : s.substring(0, s.length() - 1);

        // Comma handling:
        // - exactly 1 comma, no period, and the part after the comma is NOT exactly
        //   3 digits → decimal separator (e.g. "1,3" → 1.3, "1,30" → 1.30)
        // - everything else (multiple commas, or 3-digit group after comma) →
        //   thousands separator, strip commas (e.g. "1,300", "1,000,000")
        long commaCount = digits.chars().filter(c -> c == ',').count();
        if (commaCount == 1 && !digits.contains(".")) {
            int commaIdx = digits.indexOf(',');
            String afterComma = digits.substring(commaIdx + 1);
            if (afterComma.length() != 3) {
                digits = digits.replace(",", ".");
            } else {
                digits = digits.replace(",", "");
            }
        } else {
            digits = digits.replace(",", "");
        }

        return new BigDecimal(digits).multiply(multiplier);
    }
}
