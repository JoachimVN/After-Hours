package edu.ntnu.idatt2003.g23.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NumberParserTest {

    // ── Basic integers ────────────────────────────────────────────────────────

    @Test
    void parse_integerString_returnsBigDecimal() {
        assertEquals(0, new BigDecimal("42").compareTo(NumberParser.parse("42")));
    }

    @Test
    void parse_decimalString_returnsBigDecimal() {
        assertEquals(0, new BigDecimal("3.14").compareTo(NumberParser.parse("3.14")));
    }

    @Test
    void parse_emptyString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberParser.parse(""));
    }

    @Test
    void parse_whitespaceOnly_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberParser.parse("   "));
    }

    @Test
    void parse_nonNumeric_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberParser.parse("abc"));
    }

    // ── K / M / B suffixes ────────────────────────────────────────────────────

    @Test
    void parse_kSuffix_multipliesByThousand() {
        assertEquals(0, new BigDecimal("5000").compareTo(NumberParser.parse("5K")));
    }

    @Test
    void parse_kLowerSuffix_multipliesByThousand() {
        assertEquals(0, new BigDecimal("5000").compareTo(NumberParser.parse("5k")));
    }

    @Test
    void parse_mSuffix_multipliesByMillion() {
        assertEquals(0, new BigDecimal("2000000").compareTo(NumberParser.parse("2M")));
    }

    @Test
    void parse_mLowerSuffix_multipliesByMillion() {
        assertEquals(0, new BigDecimal("2000000").compareTo(NumberParser.parse("2m")));
    }

    @Test
    void parse_bSuffix_multipliesByBillion() {
        assertEquals(0, new BigDecimal("1000000000").compareTo(NumberParser.parse("1B")));
    }

    @Test
    void parse_bLowerSuffix_multipliesByBillion() {
        assertEquals(0, new BigDecimal("1000000000").compareTo(NumberParser.parse("1b")));
    }

    @Test
    void parse_decimalWithKSuffix_multipliesCorrectly() {
        // "1.5K" → 1500
        assertEquals(0, new BigDecimal("1500").compareTo(NumberParser.parse("1.5K")));
    }

    // ── Comma handling ────────────────────────────────────────────────────────

    @Test
    void parse_singleCommaNotThreeDigitsAfter_treatedAsDecimalSeparator() {
        // "1,5" → 1.5 (one comma, after-comma length = 1, not 3)
        assertEquals(0, new BigDecimal("1.5").compareTo(NumberParser.parse("1,5")));
    }

    @Test
    void parse_singleCommaExactlyThreeDigitsAfter_treatedAsThousandsSeparator() {
        // "1,300" → 1300 (one comma, after-comma length = 3 → thousands)
        assertEquals(0, new BigDecimal("1300").compareTo(NumberParser.parse("1,300")));
    }

    @Test
    void parse_multipleCommas_treatedAsThousandsSeparators() {
        // "1,000,000" → 1000000
        assertEquals(0, new BigDecimal("1000000").compareTo(NumberParser.parse("1,000,000")));
    }

    @Test
    void parse_commaWithKSuffix_twoDigitsAfterComma_treatedAsDecimal() {
        // "1,30K" → 1.30 × 1000 = 1300
        assertEquals(0, new BigDecimal("1300.0").compareTo(NumberParser.parse("1,30K")));
    }

    @Test
    void parse_commaWithKSuffix_threeDigitsAfterComma_treatedAsThousands() {
        // "1,300K" → 1300 × 1000 = 1300000
        assertEquals(0, new BigDecimal("1300000").compareTo(NumberParser.parse("1,300K")));
    }

    // ── Whitespace trimming ────────────────────────────────────────────────────

    @Test
    void parse_leadingAndTrailingWhitespace_trimmed() {
        assertEquals(0, new BigDecimal("100").compareTo(NumberParser.parse("  100  ")));
    }

    @Test
    void parse_whitespaceAroundSuffixed_trimmed() {
        assertEquals(0, new BigDecimal("2000").compareTo(NumberParser.parse("  2K  ")));
    }

    // ── Zero ─────────────────────────────────────────────────────────────────

    @Test
    void parse_zero_returnsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(NumberParser.parse("0")));
    }
}
