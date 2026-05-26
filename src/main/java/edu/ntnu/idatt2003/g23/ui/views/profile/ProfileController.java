package edu.ntnu.idatt2003.g23.ui.views.profile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import edu.ntnu.idatt2003.g23.model.PlayerStatus;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.ui.views.game.GameController;

/**
 * Controller for ProfileView (MVC).
 */
public final class ProfileController {
  private static final int MINI_CHART_MAX_POINTS = 100;

  private final GameController gameController;
  private final Supplier<List<String>> favoriteSymbolsSupplier;
  private final Consumer<String> favoriteToggleConsumer;
  private final Predicate<String> favoritePredicate;

  public ProfileController(GameController gameController) {
    this(gameController, List::of);
  }

  public ProfileController(GameController gameController,
                           Supplier<List<String>> favoriteSymbolsSupplier) {
    this(gameController, favoriteSymbolsSupplier, s -> {}, s -> false);
  }

  public ProfileController(GameController gameController,
                           Supplier<List<String>> favoriteSymbolsSupplier,
                           Consumer<String> favoriteToggleConsumer,
                           Predicate<String> favoritePredicate) {
    if (gameController == null) {
      throw new IllegalArgumentException("gameController cannot be null");
    }
    if (favoriteSymbolsSupplier == null) {
      throw new IllegalArgumentException("favoriteSymbolsSupplier cannot be null");
    }
    if (favoriteToggleConsumer == null) {
      throw new IllegalArgumentException("favoriteToggleConsumer cannot be null");
    }
    if (favoritePredicate == null) {
      throw new IllegalArgumentException("favoritePredicate cannot be null");
    }
    this.gameController = gameController;
    this.favoriteSymbolsSupplier = favoriteSymbolsSupplier;
    this.favoriteToggleConsumer = favoriteToggleConsumer;
    this.favoritePredicate = favoritePredicate;
  }

  public record FavoriteStockView(String symbol, String company,
                                  BigDecimal currentPrice, BigDecimal changePct,
                                  boolean owned,
                                  List<BigDecimal> priceHistory) {
  }

  public record PortfolioPositionView(String symbol,
                                      String company,
                                      BigDecimal quantity,
                                      BigDecimal averagePrice,
                                      BigDecimal currentPrice,
                                      BigDecimal marketValue,
                                      BigDecimal pnl,
                                      BigDecimal pnlPct,
                                      List<BigDecimal> priceHistory) {
  }

  public record StatusRequirementInfo(String statusName,
                                      int weeksCurrent,
                                      int weeksRequired,
                                      BigDecimal growthCurrent,
                                      BigDecimal growthRequired,
                                      BigDecimal totalProgressRatio) {
  }

  public String getPlayerName() {
    return gameController.getPlayerName();
  }

  public PlayerStatus getPlayerStatus() {
    return gameController.getPlayerStatus();
  }

  public int getCurrentWeek() {
    return gameController.getCurrentWeek();
  }

  public BigDecimal getPlayerStartingMoney() {
    return gameController.getPlayerStartingMoney();
  }

  public BigDecimal getPlayerCash() {
    return gameController.getPlayerCash();
  }

  public BigDecimal getPortfolioNetWorth() {
    return gameController.getPortfolioNetWorth();
  }

  public BigDecimal getPlayerNetWorth() {
    return gameController.getPlayerNetWorth();
  }

  public BigDecimal getPlayerGrowthRatio() {
    return gameController.getPlayerGrowthRatio();
  }

  public int getTransactionCount() {
    return gameController.getTransactionCount();
  }

  public int getPlayerWeeksTraded() {
    return gameController.getPlayerWeeksTraded();
  }

  public BigDecimal getPlayerStatusProgress() {
    return gameController.getPlayerStatusProgress();
  }

  public int getPlayerWeeksTargetForNextStatus() {
    return gameController.getPlayerWeeksTargetForNextStatus();
  }

  public BigDecimal getPlayerWeeksProgress() {
    return gameController.getPlayerWeeksProgress();
  }

  public BigDecimal getPlayerGrowthTargetForNextStatus() {
    return gameController.getPlayerGrowthTargetForNextStatus();
  }

  public BigDecimal getPlayerNetWorthProgress() {
    return gameController.getPlayerNetWorthProgress();
  }

  public List<Share> getPortfolioShares() {
    return gameController.getPortfolioShares();
  }

  public List<String> getFavoriteSymbols() {
    return List.copyOf(favoriteSymbolsSupplier.get());
  }

  public boolean isFavorite(String symbol) {
    return favoritePredicate.test(symbol);
  }

  public void toggleFavorite(String symbol) {
    favoriteToggleConsumer.accept(symbol);
  }

  public List<FavoriteStockView> getFavoriteStocks() {
    Set<String> favorites = getFavoriteSymbols().stream()
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.toSet());
    if (favorites.isEmpty()) {
      return List.of();
    }

    return gameController.getStocks().stream()
        .filter(stock -> favorites.contains(stock.getSymbol()))
        .sorted(java.util.Comparator.comparing(Stock::getSymbol))
        .map(stock -> {
          List<BigDecimal> history = compactHistory(stock.getHistoricalPrices());
          BigDecimal currentPrice = stock.getSalesPrice();
          BigDecimal changePct = percentageChangeFromInitial(currentPrice, history);
          return new FavoriteStockView(
              stock.getSymbol(),
              stock.getCompany(),
              currentPrice,
              changePct,
              gameController.isOwned(stock.getSymbol()),
              history);
        })
        .toList();
  }

      public List<PortfolioPositionView> getPortfolioPositions() {
      return gameController.getPortfolioShares().stream()
        .sorted(java.util.Comparator.comparing(share -> share.getStock().getSymbol()))
        .map(share -> {
          BigDecimal quantity = share.getQuantity();
          BigDecimal averagePrice = share.getPurchasePrice();
          BigDecimal currentPrice = share.getStock().getSalesPrice();
          BigDecimal marketValue = currentPrice.multiply(quantity);
          BigDecimal costBasis = averagePrice.multiply(quantity);
          BigDecimal pnl = marketValue.subtract(costBasis);
          BigDecimal pnlPct = costBasis.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : pnl.divide(costBasis, 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100));

          return new PortfolioPositionView(
            share.getStock().getSymbol(),
            share.getStock().getCompany(),
            quantity,
            averagePrice,
            currentPrice,
            marketValue,
            pnl,
            pnlPct,
            compactHistory(share.getStock().getHistoricalPrices()));
        })
        .toList();
      }

  public StatusRequirementInfo getStatusRequirement(PlayerStatus status) {
    int weeksNow = Math.max(0, getPlayerWeeksTraded());
    int weeksCurrent = weeksNow;
    int weeksRequired = gameController.getPlayerWeeksTargetForStatus(status);
    BigDecimal growthCurrent = getPlayerGrowthRatio().setScale(2, RoundingMode.HALF_UP);
    BigDecimal growthRequired = gameController.getPlayerGrowthTargetForStatus(status);
    BigDecimal totalRatio = gameController.getPlayerStatusProgress(status);

    return new StatusRequirementInfo(
        formatStatusName(status),
        weeksCurrent,
        weeksRequired,
        growthCurrent,
        growthRequired,
        totalRatio);
  }

  public List<GameController.ReplayPoint> getReplaySeries() {
    return gameController.getReplaySeries();
  }

  public String getDisplayedPlayerAvatar() {
    return gameController.getPlayerAvatar();
  }

  public boolean isChickAvatarEquipped() {
    return gameController.isChickAvatarEquipped();
  }

  public int getChickPhaseUnlocked() {
    return gameController.getChickPhaseUnlocked();
  }

  public int getWeeksUsingChickAvatar() {
    return gameController.getWeeksUsingChickAvatar();
  }

  private static String formatStatusName(PlayerStatus status) {
    String lower = status.name().toLowerCase(Locale.ROOT);
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }

  private static List<BigDecimal> compactHistory(List<BigDecimal> history) {
    if (history == null || history.isEmpty()) {
      return List.of();
    }
    if (history.size() <= MINI_CHART_MAX_POINTS) {
      return List.copyOf(history);
    }
    return List.copyOf(history.subList(history.size() - MINI_CHART_MAX_POINTS, history.size()));
  }

  private static BigDecimal percentageChangeFromInitial(BigDecimal currentPrice,
                                                        List<BigDecimal> history) {
    if (currentPrice == null) {
      return BigDecimal.ZERO;
    }

    BigDecimal initialPrice = (history == null || history.isEmpty())
        ? currentPrice
        : history.get(0);

    if (initialPrice == null || initialPrice.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }

    BigDecimal delta = currentPrice.subtract(initialPrice);
    if (delta.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }

    return delta.divide(initialPrice, 6, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));
  }
}
