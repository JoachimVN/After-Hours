package edu.ntnu.idatt2003.g23;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;

/**
 * Common test fixtures shared across model test classes.
 */
public final class ModelTestFixtures {

    private ModelTestFixtures() {}

    public static Stock stock() {
        return new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("150"))));
    }

    public static Share share() {
        return new Share(stock(), new BigDecimal("10"), new BigDecimal("140"));
    }

    public static Player player() {
        return new Player("TestPlayer", new BigDecimal("10000"));
    }
}
