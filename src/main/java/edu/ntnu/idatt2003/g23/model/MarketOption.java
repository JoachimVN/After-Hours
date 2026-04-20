package edu.ntnu.idatt2003.g23.model;

/**
 * Represents a built-in market that can be selected at game setup.
 *
 * @param name        Display name shown in the UI (e.g. "S&P 500").
 * @param csvResource Classpath-relative path to the stock CSV file
 *                    (e.g. "data/stocks/sp500.csv").
 * @param description Short description shown below the market selector.
 */
public record MarketOption(String name, String csvResource, String description) {
}
