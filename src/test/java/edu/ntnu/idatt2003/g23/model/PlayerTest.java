package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import edu.ntnu.idatt2003.g23.model.transaction.Purchase;

class PlayerTest {

    @Test
    @DisplayName("Constructor creates player with valid name and starting money")
    void testConstructorValidInputs() {
        BigDecimal startingMoney = new BigDecimal("1000.00");
        Player player = new Player("Alice", startingMoney);

        assertEquals("Alice", player.getName());
        assertEquals(startingMoney, player.getMoney());
        assertEquals(startingMoney, player.getStartingMoney());
        assertNotNull(player.getPortfolio());
        assertNotNull(player.getTransactionArchive());
    }

    @Test
    @DisplayName("Constructor accepts zero starting money")
    void testConstructorZeroStartingMoney() {
        BigDecimal startingMoney = BigDecimal.ZERO;
        Player player = new Player("Bob", startingMoney);

        assertEquals(BigDecimal.ZERO, player.getMoney());
        assertEquals(BigDecimal.ZERO, player.getStartingMoney());
    }

    @Test
    @DisplayName("getName returns correct name")
    void testGetName() {
        Player player = new Player("Charlie", new BigDecimal("500.00"));
        assertEquals("Charlie", player.getName());
    }

    @Test
    @DisplayName("getMoney returns current money")
    void testGetMoney() {
        Player player = new Player("David", new BigDecimal("200.00"));
        assertEquals(new BigDecimal("200.00"), player.getMoney());
    }

    @Test
    @DisplayName("getStartingMoney returns starting money")
    void testGetStartingMoney() {
        BigDecimal startingMoney = new BigDecimal("1500.00");
        Player player = new Player("Eve", startingMoney);
        assertEquals(startingMoney, player.getStartingMoney());
    }

    @Test
    @DisplayName("addMoney increases player's money correctly")
    void testAddMoney() {
        Player player = new Player("Frank", new BigDecimal("100.00"));
        BigDecimal amountToAdd = new BigDecimal("50.00");

        player.addMoney(amountToAdd);

        assertEquals(new BigDecimal("150.00"), player.getMoney());
    }

    @Test
    @DisplayName("addMoney throws on null amount")
    void testAddMoneyThrowsOnNull() {
        Player player = new Player("Grace", new BigDecimal("100.00"));

        assertThrows(IllegalArgumentException.class, () -> player.addMoney(null));
    }

    @Test
    @DisplayName("addMoney throws on negative amount")
    void testAddMoneyThrowsOnNegative() {
        Player player = new Player("Henry", new BigDecimal("100.00"));
        BigDecimal negativeAmount = new BigDecimal("-10.00");

        assertThrows(IllegalArgumentException.class, () -> player.addMoney(negativeAmount));
    }

    @Test
    @DisplayName("addMoney allows zero amount")
    void testAddMoneyAllowsZero() {
        Player player = new Player("Ian", new BigDecimal("100.00"));
        BigDecimal zeroAmount = BigDecimal.ZERO;

        player.addMoney(zeroAmount);

        assertEquals(new BigDecimal("100.00"), player.getMoney());
    }

    @Test
    @DisplayName("withdrawMoney decreases player's money correctly")
    void testWithdrawMoney() {
        Player player = new Player("Jack", new BigDecimal("100.00"));
        BigDecimal amountToWithdraw = new BigDecimal("30.00");

        player.withdrawMoney(amountToWithdraw);

        assertEquals(new BigDecimal("70.00"), player.getMoney());
    }

    @Test
    @DisplayName("withdrawMoney throws on null amount")
    void testWithdrawMoneyThrowsOnNull() {
        Player player = new Player("Kate", new BigDecimal("100.00"));

        assertThrows(IllegalArgumentException.class, () -> player.withdrawMoney(null));
    }

    @Test
    @DisplayName("withdrawMoney throws on negative amount")
    void testWithdrawMoneyThrowsOnNegative() {
        Player player = new Player("Liam", new BigDecimal("100.00"));
        BigDecimal negativeAmount = new BigDecimal("-10.00");

        assertThrows(IllegalArgumentException.class, () -> player.withdrawMoney(negativeAmount));
    }

    @Test
    @DisplayName("withdrawMoney allows zero amount")
    void testWithdrawMoneyAllowsZero() {
        Player player = new Player("Mia", new BigDecimal("100.00"));
        BigDecimal zeroAmount = BigDecimal.ZERO;

        player.withdrawMoney(zeroAmount);

        assertEquals(new BigDecimal("100.00"), player.getMoney());
    }

    @Test
    @DisplayName("withdrawMoney throws when insufficient funds")
    void testWithdrawMoneyThrowsOnInsufficientFunds() {
        Player player = new Player("Noah", new BigDecimal("50.00"));
        BigDecimal largeAmount = new BigDecimal("100.00");

        assertThrows(IllegalStateException.class, () -> player.withdrawMoney(largeAmount));
    }

    @Test
    @DisplayName("withdrawMoney allows withdrawing exact amount")
    void testWithdrawMoneyExactAmount() {
        Player player = new Player("Olivia", new BigDecimal("75.00"));
        BigDecimal exactAmount = new BigDecimal("75.00");

        player.withdrawMoney(exactAmount);

        assertTrue(player.getMoney().compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    @DisplayName("getPortfolio returns portfolio")
    void testGetPortfolio() {
        Player player = new Player("Peter", new BigDecimal("200.00"));
        assertNotNull(player.getPortfolio());
    }

    @Test
    @DisplayName("getTransactionArchive returns transaction archive")
    void testGetTransactionArchive() {
        Player player = new Player("Quinn", new BigDecimal("300.00"));
        assertNotNull(player.getTransactionArchive());
    }

    @Test
    @DisplayName("getNetWorth includes both cash and portfolio net worth")
    void testGetNetWorth() {
        Player player = new Player("Sara", new BigDecimal("1000.00"));

        Stock apple = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("120")));
        Share appleShare = new Share(apple, new BigDecimal("10"), new BigDecimal("100"));
        player.getPortfolio().addShare(appleShare);

        assertEquals(new BigDecimal("2131.600"), player.getNetWorth());
    }

    @Test
    @DisplayName("getNetWorth equals cash when portfolio is empty")
    void testGetNetWorthEmptyPortfolio() {
        Player player = new Player("Tom", new BigDecimal("750.00"));

        assertEquals(new BigDecimal("750.00"), player.getNetWorth());
    }

    @Test
    @DisplayName("New player has NOVICE status by default")
    void testDefaultStatusIsNovice() {
        Player player = new Player("Alice", new BigDecimal("1000"));

        assertEquals(PlayerStatus.NOVICE, player.getStatus());
    }

    @Test
    @DisplayName("getWeeksTraded returns count of distinct weeks traded")
    void testGetWeeksTraded() {
        Player player = new Player("Uma", new BigDecimal("1000.00"));

        // Initially no trades
        assertEquals(0, player.getWeeksTraded());
    }

    @Nested
    @DisplayName("calculateStatus Tests")
    class CalculateStatusTests {

        @Test
        @DisplayName("calculateStatus sets NOVICE when weeks == 0")
        void testCalculateStatusNoviceNoWeeks() {
            Player player = new Player("Victoria", new BigDecimal("1000.00"));
            player.calculateStatus();

            assertEquals(PlayerStatus.NOVICE, player.getStatus());
        }

        @Test
        @DisplayName("calculateStatus sets NOVICE with zero starting money")
        void testCalculateStatusNoviceZeroStartingMoney() {
            Player zeroPlayer = new Player("Walter", BigDecimal.ZERO);
            zeroPlayer.calculateStatus();

            assertEquals(PlayerStatus.NOVICE, zeroPlayer.getStatus());
        }

        @Test
        @DisplayName("calculateStatus sets NOVICE with low growth and weeks")
        void testCalculateStatusNoviceLowGrowth() {
            // Start with 1000, end with 1100 (growth = 1.1)
            Player player = new Player("Victoria", new BigDecimal("1000.00"));
            player.addMoney(new BigDecimal("100.00"));
            player.calculateStatus();

            assertEquals(PlayerStatus.NOVICE, player.getStatus());
        }

        @Test
        @DisplayName("getNetWorth correctly includes multiple shares")
        void testGetNetWorthMultipleShares() {
            Player player = new Player("Ben", new BigDecimal("1000.00"));

            Stock aapl = new Stock("AAPL", "Apple", List.of(new BigDecimal("150")));
            Stock googl = new Stock("GOOGL", "Google", List.of(new BigDecimal("200")));

            Share aaplShare = new Share(aapl, new BigDecimal("5"), new BigDecimal("150"));
            Share googlShare = new Share(googl, new BigDecimal("3"), new BigDecimal("200"));

            player.getPortfolio().addShare(aaplShare);
            player.getPortfolio().addShare(googlShare);

            // Net worth = cash + portfolio value after commission and tax
            // AAPL: 750 - 7.50 commission, no profit → tax = 0 → 742.50
            // GOOGL: 600 - 6.00 commission, no profit → tax = 0 → 594.00
            // = 1000 + 742.50 + 594.00 = 2336.50
            assertEquals(new BigDecimal("2336.50"), player.getNetWorth());
        }

        @Test
        @DisplayName("getNetWorth updates after money operations")
        void testGetNetWorthAfterMoneyOperations() {
            Player player = new Player("Cathy", new BigDecimal("500.00"));

            Stock stock = new Stock("TEST", "Test Corp", List.of(new BigDecimal("100")));
            Share share = new Share(stock, new BigDecimal("2"), new BigDecimal("100"));
            player.getPortfolio().addShare(share);

            player.addMoney(new BigDecimal("250.00"));

            // Net worth = 750 (cash) + portfolio value after commission and tax
            // TEST: 200 - 2.00 commission, no profit → tax = 0 → 198.00
            // = 750 + 198.00 = 948.00
            assertEquals(new BigDecimal("948.00"), player.getNetWorth());
        }

        @Test
        @DisplayName("calculateStatus sets INVESTOR with 10+ weeks and 1.2x growth")
        void testCalculateStatusInvestor() {
            Player player = new Player("Alice", new BigDecimal("1000.00"));
            Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
            Share share = new Share(stock, new BigDecimal("1"), new BigDecimal("140"));
            for (int i = 1; i <= 10; i++) {
                player.getTransactionArchive().add(new Purchase(share, i));
            }
            player.addMoney(new BigDecimal("200.00")); // net worth = 1200, growth = 1.2

            player.calculateStatus();

            assertEquals(PlayerStatus.INVESTOR, player.getStatus());
        }

        @Test
        @DisplayName("calculateStatus sets SPECULATOR with 20+ weeks and 2x growth")
        void testCalculateStatusSpeculator() {
            Player player = new Player("Alice", new BigDecimal("1000.00"));
            Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
            Share share = new Share(stock, new BigDecimal("1"), new BigDecimal("140"));
            for (int i = 1; i <= 20; i++) {
                player.getTransactionArchive().add(new Purchase(share, i));
            }
            player.addMoney(new BigDecimal("1000.00")); // net worth = 2000, growth = 2.0

            player.calculateStatus();

            assertEquals(PlayerStatus.SPECULATOR, player.getStatus());
        }

        @Test
        @DisplayName("calculateStatus stays NOVICE when 20+ weeks but growth < 2x")
        void testCalculateStatusNotSpeculatorLowGrowth() {
            Player player = new Player("Alice", new BigDecimal("1000.00"));
            Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
            Share share = new Share(stock, new BigDecimal("1"), new BigDecimal("140"));
            for (int i = 1; i <= 20; i++) {
                player.getTransactionArchive().add(new Purchase(share, i));
            }
            // growth = 1.0, does not qualify for SPECULATOR or INVESTOR

            player.calculateStatus();

            assertEquals(PlayerStatus.NOVICE, player.getStatus());
        }

        @Test
        @DisplayName("calculateStatus stays NOVICE when 10+ weeks but growth < 1.2x")
        void testCalculateStatusNotInvestorLowGrowth() {
            Player player = new Player("Alice", new BigDecimal("1000.00"));
            Stock stock = new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("150")));
            Share share = new Share(stock, new BigDecimal("1"), new BigDecimal("140"));
            for (int i = 1; i <= 10; i++) {
                player.getTransactionArchive().add(new Purchase(share, i));
            }
            // growth = 1.0, does not qualify for INVESTOR

            player.calculateStatus();

            assertEquals(PlayerStatus.NOVICE, player.getStatus());
        }
    }

    @Nested
    @DisplayName("Constructor validation tests")
    class ConstructorValidationTests {

        @Test
        @DisplayName("Constructor throws on null name")
        void testConstructorThrowsOnNullName() {
            assertThrows(IllegalArgumentException.class,
                () -> new Player(null, new BigDecimal("1000")));
        }

        @Test
        @DisplayName("Constructor throws on blank name")
        void testConstructorThrowsOnBlankName() {
            assertThrows(IllegalArgumentException.class,
                () -> new Player("   ", new BigDecimal("1000")));
        }

        @Test
        @DisplayName("Constructor throws on null starting money")
        void testConstructorThrowsOnNullStartingMoney() {
            assertThrows(IllegalArgumentException.class,
                () -> new Player("Alice", null));
        }

        @Test
        @DisplayName("Constructor throws on negative starting money")
        void testConstructorThrowsOnNegativeStartingMoney() {
            assertThrows(IllegalArgumentException.class,
                () -> new Player("Alice", new BigDecimal("-1")));
        }
    }
}