package edu.ntnu.idatt2003.g23.model;

import edu.ntnu.idatt2003.g23.ModelTestFixtures;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ShareTest {

    @Test
    @DisplayName("Constructor stores fields correctly")
    void testConstructorStoresValues() {
        Stock stock = ModelTestFixtures.stock();
        Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("150.00"));

        assertEquals(stock, share.getStock());
        assertEquals(new BigDecimal("10"), share.getQuantity());
        assertEquals(new BigDecimal("150.00"), share.getPurchasePrice());
    }

    @Nested
    @DisplayName("Constructor validation tests")
    class ConstructorValidationTests {

        @Test
        @DisplayName("Constructor throws on null stock")
        void testConstructorThrowsOnNullStock() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Share(null, new BigDecimal("5"), new BigDecimal("100")));
        }

        @Test
        @DisplayName("Constructor throws on null quantity")
        void testConstructorThrowsOnNullQuantity() {
            Stock stock = ModelTestFixtures.stock();
            assertThrows(IllegalArgumentException.class, () -> new Share(stock, null, new BigDecimal("100")));
        }

        @Test
        @DisplayName("Constructor throws on zero quantity")
        void testConstructorThrowsOnZeroQuantity() {
            Stock stock = ModelTestFixtures.stock();
            assertThrows(IllegalArgumentException.class, () -> new Share(stock, BigDecimal.ZERO, new BigDecimal("100")));
        }

        @Test
        @DisplayName("Constructor throws on negative quantity")
        void testConstructorThrowsOnNegativeQuantity() {
            Stock stock = ModelTestFixtures.stock();
            assertThrows(IllegalArgumentException.class, () -> new Share(stock, new BigDecimal("-5"), new BigDecimal("100")));
        }

        @Test
        @DisplayName("Constructor throws on null purchase price")
        void testConstructorThrowsOnNullPurchasePrice() {
            Stock stock = ModelTestFixtures.stock();
            assertThrows(IllegalArgumentException.class, () -> new Share(stock, new BigDecimal("10"), null));
        }

        @Test
        @DisplayName("Constructor throws on zero purchase price")
        void testConstructorThrowsOnZeroPurchasePrice() {
            Stock stock = ModelTestFixtures.stock();
            assertThrows(IllegalArgumentException.class, () -> new Share(stock, new BigDecimal("10"), BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Constructor throws on negative purchase price")
        void testConstructorThrowsOnNegativePurchasePrice() {
            Stock stock = ModelTestFixtures.stock();
            assertThrows(IllegalArgumentException.class, () -> new Share(stock, new BigDecimal("10"), new BigDecimal("-1")));
        }
    }
}
