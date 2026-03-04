package edu.ntnu.idatt2003.g23.model;

import java.util.List;
import java.util.stream.Collectors;

// Represents a portfolio containing a list of shares. Provides methods to add and remove shares, retrieve all shares, and get shares by stock symbol.
public class Portfolio {
    private final List<Share> shares;

    // Constructor for creating a new Portfolio instance with the specified list of shares.
    Portfolio(List<Share> shares) {
        this.shares = shares;
    }

    /**
     * Adds a share to the portfolio
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
     * @return a list of all shares in the portfolio
     */
    public List<Share> getShares() {
        return shares;
    }

    /**
     * Retrieves shares in the portfolio by their stock symbol
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
                     .collect(Collectors.toList());
    }
}

