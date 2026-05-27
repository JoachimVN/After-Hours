package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import edu.ntnu.idatt2003.g23.model.transaction.calculator.SaleCalculator;

// Represents a portfolio containing a list of shares. Provides methods to add and remove shares, retrieve all shares, and get shares by stock symbol.
public class Portfolio {
  private final List<Share> shares;

  // Constructor for creating a new Portfolio instance with the specified list of shares.
  public Portfolio() {
    this.shares = new ArrayList<>();
  }

  /**
   * Adds a share to the portfolio
   *
   * @param share the share to add
   * @return true if the share was added successfully, false otherwise
   * @throws IllegalArgumentException if the share is null
   */
  public boolean addShare(Share share) {
    if (share == null) {
      throw new IllegalArgumentException("Share cannot be null");
    }
    shares.add(share);
    return true;
  }

  /**
   * Removes a share from the portfolio
   *
   * @param share the share to remove
   * @return true if the share was removed successfully, false otherwise
   * @throws IllegalArgumentException if the share is null
   */
  public boolean removeShare(Share share) {
    if (share == null) {
      throw new IllegalArgumentException("Share cannot be null");
    }
    shares.remove(share);
    return true;
  }

  /**
   * Retrieves all shares in the portfolio
   *
   * @return a list of all shares in the portfolio
   */
  public List<Share> getShares() {
    return shares;
  }

  /**
   * Retrieves shares in the portfolio by their stock symbol
   *
   * @param symbol the stock symbol to search for
   * @return a list of shares with the specified stock symbol
   * @throws IllegalArgumentException if the symbol is null
   */
  public List<Share> getShareBySymbol(String symbol) {
    if (symbol == null) {
      throw new IllegalArgumentException("Symbol cannot be null");
    }
    return shares.stream()
        .filter(share -> share.getStock().getSymbol().equals(symbol))
        .toList();
  }

  /**
   * Calculates the total net worth of the portfolio
   *
   * @return the total net worth of the portfolio
   * @throws IllegalStateException if any share in the portfolio has invalid data (e.g., null stock, quantity, or purchase price)
   */
  public BigDecimal getNetWorth() {
    // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
    return shares.stream()
        .map(share -> new SaleCalculator(share).calculateTotal())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}