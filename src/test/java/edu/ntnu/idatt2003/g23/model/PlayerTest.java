package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("money operations maintain precision with BigDecimal")
    void testBigDecimalPrecision() {
        Player player = new Player("Ryan", new BigDecimal("100.50"));
        BigDecimal addAmount = new BigDecimal("0.25");
        BigDecimal withdrawAmount = new BigDecimal("50.75");

        player.addMoney(addAmount);
        assertEquals(new BigDecimal("100.75"), player.getMoney());

        player.withdrawMoney(withdrawAmount);
        assertEquals(new BigDecimal("50.00"), player.getMoney());
    }
}
