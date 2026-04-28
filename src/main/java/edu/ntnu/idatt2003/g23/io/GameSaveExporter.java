package edu.ntnu.idatt2003.g23.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.transaction.Purchase;
import edu.ntnu.idatt2003.g23.model.transaction.Transaction;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Saves a game to a folder under {@code ~/.afterhours/saves/}.
 *
 * <p>Each save is a directory named {@code <playerName>_<timestamp>} and
 * contains:
 * <ul>
 *   <li>{@code save.json} — player state, portfolio, transactions, meta</li>
 *   <li>{@code stocks.csv} — full price history (semicolon format)</li>
 * </ul>
 */
public final class GameSaveExporter {

  public static final Path SAVES_DIR =
      Path.of(System.getProperty("user.home"), ".afterhours", "saves");

  /**
   * Dedicated directory for autosave slots (one per player+exchange).
   */
  public static final Path AUTOSAVE_DIR =
      Path.of(System.getProperty("user.home"), ".afterhours", "autosaves");

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final DateTimeFormatter FOLDER_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
  private static final DateTimeFormatter DISPLAY_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private GameSaveExporter() {
  }

  // ── DTOs written into save.json ───────────────────────────────────────────

  private record ShareDto(String symbol, String quantity, String purchasePrice) {
  }

  private record TransactionDto(String type, String symbol, String quantity,
                                String purchasePrice, int week) {
  }

  private record WeeklySnapshotDto(int week, String cash, String portfolioValue, String netWorth) {
  }

  private record UiStateJson(List<String> favorites, List<String> activeFilters,
                             List<String> filterChipOrder, String stockSort,
                             String selectedSymbol, double sidebarDivider,
                             double portfolioDivider) {
  }

  private record SaveJson(
      String playerName,
      String profileAvatar,
      String startingMoney,
      String money,
      String netWorth,
      String status,
      int week,
      String exchangeName,
      String savedAt,
      boolean autosave,
      List<ShareDto> portfolio,
      List<TransactionDto> transactions,
      List<WeeklySnapshotDto> weeklySnapshots,
      UiStateJson uiState,
      Integer weeksUsingChickAvatar) {
  }

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Writes a new save folder for the given game state.
   *
   * @param player   the player to save
   * @param exchange the exchange to save
   * @return the path of the newly created save folder
   * @throws IOException if any file operation fails
   */
  public static Path save(Player player, Exchange exchange) throws IOException {
    return save(player, exchange, null);
  }

  public static Path save(Player player, Exchange exchange, GameUiState uiState)
      throws IOException {
    LocalDateTime now = LocalDateTime.now();
    String safeName = player.getName().replaceAll("[^A-Za-z0-9_\\-]", "_");
    String folderName = safeName + "_" + now.format(FOLDER_FMT);

    Path saveDir = SAVES_DIR.resolve(folderName);
    Files.createDirectories(saveDir);

    writeJson(saveDir, player, exchange, now, uiState, false);
    StockCsvExporter.writeHistory(saveDir.resolve("stocks.csv"),
        exchange.getStocks());

    return saveDir;
  }

  /**
   * Saves to the autosave slot identified by {@code slotId}. Each game instance
   * should supply a distinct slot ID so that different sessions never overwrite
   * each other's autosave (e.g. two "Alice" games started independently each
   * keep their own slot).
   *
   * @param slotId unique identifier for this game instance (caller-supplied)
   */
  public static Path autosave(Player player, Exchange exchange, GameUiState uiState,
                              String slotId) throws IOException {
    String safeSlot = normalizeAutosaveSlotId(slotId);
    String folderName = "autosave_" + safeSlot;
    Path saveDir = AUTOSAVE_DIR.resolve(folderName);
    cleanupDuplicateAutosaveSlots(safeSlot, saveDir);
    Files.createDirectories(saveDir);
    writeJson(saveDir, player, exchange, LocalDateTime.now(), uiState, true);
    StockCsvExporter.writeHistory(saveDir.resolve("stocks.csv"), exchange.getStocks());
    return saveDir;
  }

  /**
   * Overwrites an existing save folder's contents (used when the player
   * loaded a save and wants to save over it).
   *
   * @param saveDir  the existing save directory to overwrite
   * @param player   the current player state
   * @param exchange the current exchange state
   * @throws IOException if any file operation fails
   */
  public static void overwrite(Path saveDir, Player player, Exchange exchange)
      throws IOException {
    overwrite(saveDir, player, exchange, null);
  }

  public static void overwrite(Path saveDir, Player player, Exchange exchange,
                               GameUiState uiState) throws IOException {
    Files.createDirectories(saveDir);
    writeJson(saveDir, player, exchange, LocalDateTime.now(), uiState, false);
    StockCsvExporter.writeHistory(saveDir.resolve("stocks.csv"),
        exchange.getStocks());
  }

  /**
   * Exports a selected save to two files (JSON + CSV), following the same
   * structure used internally by save folders.
   *
   * @param saveDir         the save folder containing save.json and stocks.csv
   * @param destinationBase chosen file path used as base name for output files
   * @return array where index 0 is json destination and index 1 is csv destination
   */
  public static Path[] exportSaveDataFiles(Path saveDir, Path destinationBase) throws IOException {
    if (saveDir == null) {
      throw new IllegalArgumentException("saveDir cannot be null");
    }
    if (destinationBase == null) {
      throw new IllegalArgumentException("destinationBase cannot be null");
    }

    Path saveJson = saveDir.resolve("save.json");
    Path stocksCsv = saveDir.resolve("stocks.csv");
    if (!Files.exists(saveJson)) {
      throw new IOException("Missing save.json in selected save");
    }
    if (!Files.exists(stocksCsv)) {
      throw new IOException("Missing stocks.csv in selected save");
    }

    Path parent = destinationBase.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    String fileName = destinationBase.getFileName().toString();
    String stem;
    int dot = fileName.lastIndexOf('.');
    if (dot > 0) {
      stem = fileName.substring(0, dot);
    } else {
      stem = fileName;
    }

    Path jsonDest = (parent == null ? Path.of(stem + ".json") : parent.resolve(stem + ".json"));
    Path csvDest = (parent == null ? Path.of(stem + ".csv") : parent.resolve(stem + ".csv"));

    Files.copy(saveJson, jsonDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    Files.copy(stocksCsv, csvDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    return new Path[] {jsonDest, csvDest};
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static void writeJson(Path saveDir, Player player, Exchange exchange,
                                LocalDateTime savedAt, GameUiState uiState,
                                boolean isAutosave) throws IOException {
    List<ShareDto> portfolio = new ArrayList<>();
    for (Share share : player.getPortfolio().getShares()) {
      portfolio.add(new ShareDto(
          share.getStock().getSymbol(),
          share.getQuantity().toPlainString(),
          share.getPurchasePrice().toPlainString()));
    }

    List<TransactionDto> transactions = new ArrayList<>();
    for (Transaction t : player.getTransactionArchive().getAll()) {
      String type = (t instanceof Purchase) ? "BUY" : "SELL";
      transactions.add(new TransactionDto(
          type,
          t.getShare().getStock().getSymbol(),
          t.getShare().getQuantity().toPlainString(),
          t.getShare().getPurchasePrice().toPlainString(),
          t.getWeek()));
    }

    List<WeeklySnapshotDto> weeklySnapshots = new ArrayList<>();
    for (Player.WeeklySnapshot s : player.getWeeklySnapshots()) {
      weeklySnapshots.add(new WeeklySnapshotDto(
          s.week(),
          s.cash().toPlainString(),
          s.portfolioValue().toPlainString(),
          s.netWorth().toPlainString()));
    }

    UiStateJson uiStateJson = uiState == null ? null : new UiStateJson(
        List.copyOf(uiState.favorites()),
        List.copyOf(uiState.activeFilters()),
        List.copyOf(uiState.filterChipOrder()),
        uiState.stockSort(),
        uiState.selectedSymbol(),
        uiState.sidebarDivider(),
        uiState.portfolioDivider());

    BigDecimal netWorth = player.getNetWorth();

    SaveJson json = new SaveJson(
        player.getName(),
        player.getProfileAvatar(),
        player.getStartingMoney().toPlainString(),
        player.getMoney().toPlainString(),
        netWorth.toPlainString(),
        player.getStatus().name(),
        exchange.getWeek(),
        exchange.getName(),
        savedAt.format(DISPLAY_FMT),
        isAutosave,
        portfolio,
        transactions,
        weeklySnapshots,
        uiStateJson,
        player.getWeeksUsingChickAvatar());

    Path jsonFile = saveDir.resolve("save.json");
    Files.writeString(jsonFile, GSON.toJson(json), StandardCharsets.UTF_8);
  }

  private static void cleanupDuplicateAutosaveSlots(String safeSlot, Path canonicalSlotDir)
      throws IOException {
    if (!Files.exists(AUTOSAVE_DIR)) {
      return;
    }
    try (var dirs = Files.list(AUTOSAVE_DIR)) {
      List<Path> duplicates = dirs
          .filter(Files::isDirectory)
          .filter(p -> {
            String folderName = p.getFileName().toString();
            String normalized = normalizeAutosaveFolderName(folderName);
            return normalized.equals(safeSlot);
          })
          .filter(p -> !p.equals(canonicalSlotDir))
          .toList();
      for (Path duplicate : duplicates) {
        deleteDirectoryRecursively(duplicate);
      }
    }
  }

  private static String normalizeAutosaveSlotId(String slotId) {
    String safe = slotId == null ? "" : slotId.replaceAll("[^A-Za-z0-9_\\-]", "_");
    while (safe.startsWith("autosave_")) {
      safe = safe.substring("autosave_".length());
    }
    if (safe.isBlank()) {
      return "slot";
    }
    return safe;
  }

  private static String normalizeAutosaveFolderName(String folderName) {
    String normalized = folderName;
    while (normalized.startsWith("autosave_")) {
      normalized = normalized.substring("autosave_".length());
    }
    return normalized;
  }

  private static void deleteDirectoryRecursively(Path directory) throws IOException {
    if (!Files.exists(directory)) {
      return;
    }
    try (var walk = Files.walk(directory)) {
      walk.sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              Files.deleteIfExists(path);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    } catch (RuntimeException e) {
      if (e.getCause() instanceof IOException io) {
        throw io;
      }
      throw e;
    }
  }
}
