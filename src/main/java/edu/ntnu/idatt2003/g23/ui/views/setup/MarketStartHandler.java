package edu.ntnu.idatt2003.g23.ui.views.setup;

/**
 * Callback fired when the user starts a game with a built-in market.
 */
@FunctionalInterface
public interface MarketStartHandler {
    void start(String playerName, double startingCash, String csvResourcePath);
}
