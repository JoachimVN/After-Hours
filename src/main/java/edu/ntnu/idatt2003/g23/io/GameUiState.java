package edu.ntnu.idatt2003.g23.io;

import java.util.List;

/**
 * Snapshot of the in-game UI state (filter chips, sort order, selected stock,
 * starred favourites). Written into {@code save.json} and restored on load.
 */
public record GameUiState(
    List<String> favorites,
    List<String> activeFilters,
    List<String> filterChipOrder,
    String stockSort,
    String selectedSymbol,
    double sidebarDivider,
    double portfolioDivider) {
}
