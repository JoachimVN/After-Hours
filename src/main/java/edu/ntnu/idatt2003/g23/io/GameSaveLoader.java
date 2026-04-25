package edu.ntnu.idatt2003.g23.io;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.PlayerStatus;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.model.transaction.Purchase;
import edu.ntnu.idatt2003.g23.model.transaction.Sale;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ntnu.idatt2003.g23.ui.util.AvatarUtil;

/**
 * Lists and loads game saves from {@link GameSaveExporter#SAVES_DIR}.
 */
public final class GameSaveLoader {

  private static final Gson GSON = new Gson();

  private GameSaveLoader() {
  }

  // ── Public record exposed to callers ──────────────────────────────────────

  /**
   * Metadata shown in the save-selection UI without loading the full game.
   */
  public record SaveMeta(
      Path saveDir,
      String displayName,
      String profileAvatar,
      String exchangeName,
      String savedAt,
      int week,
      String money,
      String netWorthHint,
      int portfolioSize,
      int totalShares,
      String status,
      boolean autosave) {

    public String id() {
      return saveDir.getFileName().toString();
    }
  }

  // ── Listing ───────────────────────────────────────────────────────────────

  /**
   * Returns all saves sorted newest-first.
   */
  public static List<SaveMeta> listSaves() throws IOException {
    List<SaveMeta> result = new ArrayList<>();

    // Manual saves
    Path dir = GameSaveExporter.SAVES_DIR;
    if (Files.exists(dir)) {
      try (var stream = Files.list(dir)) {
        stream.filter(Files::isDirectory).forEach(saveDir -> {
          Path json = saveDir.resolve("save.json");
          if (!Files.exists(json)) {
            return;
          }
          try {
            result.add(readMeta(saveDir, json));
          } catch (IOException ignored) {
          }
        });
      }
    }

    // Autosaves (listed separately — dedicated folder)
    Path autoDir = GameSaveExporter.AUTOSAVE_DIR;
    if (Files.exists(autoDir)) {
      Map<String, SaveMeta> newestBySlot = new HashMap<>();
      Map<String, Long> newestTimeBySlot = new HashMap<>();
      List<Path> staleAutosaveDirs = new ArrayList<>();
      try (var stream = Files.list(autoDir)) {
        stream.filter(Files::isDirectory).forEach(saveDir -> {
          Path json = saveDir.resolve("save.json");
          if (!Files.exists(json)) {
            return;
          }
          try {
            SaveMeta meta = readMeta(saveDir, json);
            String slot = normalizeAutosaveSlot(saveDir.getFileName().toString());
            long modified = Files.getLastModifiedTime(json).toMillis();

            if (!newestBySlot.containsKey(slot) || modified > newestTimeBySlot.get(slot)) {
              SaveMeta previous = newestBySlot.put(slot, meta);
              newestTimeBySlot.put(slot, modified);
              if (previous != null) {
                staleAutosaveDirs.add(previous.saveDir());
              }
            } else {
              staleAutosaveDirs.add(saveDir);
            }
          } catch (IOException ignored) {
          }
        });
      }
      result.addAll(newestBySlot.values());
      for (Path staleDir : staleAutosaveDirs) {
        try {
          deleteSave(staleDir);
        } catch (IOException ignored) {
        }
      }
    }

    result.sort(Comparator.comparing(SaveMeta::id).reversed());
    return result;
  }

  /**
   * Renames a save folder (and updates the displayName in save.json is NOT
   * required — the folder name is just the ID; displayName comes from the
   * JSON playerName).
   */
  public static Path renameSave(Path saveDir, String newFolderName) throws IOException {
    return renameSave(saveDir, newFolderName, null);
  }

  /**
   * Renames a save folder and optionally updates a UI display label in
   * {@code save.json}. When {@code newDisplayName} is blank or null, the
   * existing JSON label is preserved.
   */
  public static Path renameSave(Path saveDir, String newFolderName, String newDisplayName)
      throws IOException {
    Path parent = saveDir.getParent();
    Path target = parent.resolve(newFolderName);
    Path moved = Files.move(saveDir, target);

    if (newDisplayName != null && !newDisplayName.isBlank()) {
      Path jsonPath = moved.resolve("save.json");
      if (Files.exists(jsonPath)) {
        JsonObject obj =
            GSON.fromJson(Files.readString(jsonPath, StandardCharsets.UTF_8), JsonObject.class);
        obj.addProperty("displayName", newDisplayName.strip());
        Files.writeString(jsonPath, GSON.toJson(obj), StandardCharsets.UTF_8);
      }
    }

    return moved;
  }

  /**
   * Deletes a save folder and all its contents.
   */
  public static void deleteSave(Path saveDir) throws IOException {
    try (var stream = Files.walk(saveDir)) {
      var paths = stream.sorted(Comparator.reverseOrder()).toList();
      for (Path p : paths) {
        Files.delete(p);
      }
    }
  }

  // ── Loading ───────────────────────────────────────────────────────────────

  /**
   * Fully loads a save into a live {@link Player} and {@link Exchange}.
   *
   * @param saveDir the save folder to load from
   * @return a two-element array: {@code [player, exchange]}
   * @throws IOException           if reading fails
   * @throws IllegalStateException if the save data is corrupt
   */
  public static Object[] load(Path saveDir) throws IOException {
    Path jsonPath = saveDir.resolve("save.json");
    Path csvPath = saveDir.resolve("stocks.csv");

    String raw = Files.readString(jsonPath, StandardCharsets.UTF_8);
    JsonObject obj = GSON.fromJson(raw, JsonObject.class);

    String playerName = obj.get("playerName").getAsString();
    BigDecimal starting = new BigDecimal(obj.get("startingMoney").getAsString());
    BigDecimal money = new BigDecimal(obj.get("money").getAsString());
    String statusStr = obj.get("status").getAsString();
    int week = obj.get("week").getAsInt();
    String exchangeName = obj.get("exchangeName").getAsString();

    // ── Restore stocks (full price history) from CSV ───────────────────
    CsvParseResult csvResult = StockCsvLoader.parseWithErrors(
        Files.newBufferedReader(csvPath, StandardCharsets.UTF_8));
    List<Stock> stocks = csvResult.getRows().stream()
        .filter(r -> !r.hasError())
        .map(StockCsvLoader::rowToStock)
        .toList();

    if (stocks.isEmpty()) {
      throw new IllegalStateException("Save file contains no valid stocks");
    }

    // ── Build exchange ─────────────────────────────────────────────────
    Exchange exchange = new Exchange(exchangeName, stocks);
    exchange.setWeek(week);

    // ── Build player ───────────────────────────────────────────────────
    Player player = new Player(playerName, starting);
    player.setMoney(money);
    String savedProfileAvatar = null;
    if (obj.has("profileAvatar") && !obj.get("profileAvatar").isJsonNull()) {
      savedProfileAvatar = obj.get("profileAvatar").getAsString();
      player.setProfileAvatar(savedProfileAvatar);
    }
    int derivedLegacyPhase = deriveLegacyChickPhase(savedProfileAvatar);
    if (derivedLegacyPhase > 0) {
      player.setWeeksUsingChickAvatar(derivedLegacyPhase * 10);
    }
    // Restore chicks avatar progression if present
    if (obj.has("weeksUsingChickAvatar") && !obj.get("weeksUsingChickAvatar").isJsonNull()) {
      player.setWeeksUsingChickAvatar(obj.get("weeksUsingChickAvatar").getAsInt());
    }
    try {
      player.setStatus(PlayerStatus.valueOf(statusStr));
    } catch (IllegalArgumentException ignored) {
      // unknown status → keep default NOVICE
    }

    // ── Restore portfolio ─────────────────────────────────────────────
    JsonArray portfolio = obj.getAsJsonArray("portfolio");
    if (portfolio != null) {
      for (JsonElement el : portfolio) {
        JsonObject s = el.getAsJsonObject();
        String symbol = s.get("symbol").getAsString();
        BigDecimal quantity = new BigDecimal(s.get("quantity").getAsString());
        BigDecimal purchase = new BigDecimal(s.get("purchasePrice").getAsString());

        // Find the matching Stock object from the exchange
        if (!exchange.hasStock(symbol)) {
          continue;
        }
        Stock stock = exchange.getStock(symbol);
        player.getPortfolio().addShare(new Share(stock, quantity, purchase));
      }
    }

    // ── Restore transaction history ────────────────────────────────────
    JsonArray transactions = obj.getAsJsonArray("transactions");
    if (transactions != null) {
      for (JsonElement el : transactions) {
        JsonObject t = el.getAsJsonObject();
        String type = t.get("type").getAsString();
        String symbol = t.get("symbol").getAsString();
        BigDecimal quantity = new BigDecimal(t.get("quantity").getAsString());
        BigDecimal price = new BigDecimal(t.get("purchasePrice").getAsString());
        int txWeek = t.get("week").getAsInt();

        if (!exchange.hasStock(symbol)) {
          continue;
        }
        Stock stock = exchange.getStock(symbol);
        Share share = new Share(stock, quantity, price);

        if ("BUY".equals(type)) {
          player.getTransactionArchive().add(new Purchase(share, txWeek));
        } else {
          player.getTransactionArchive().add(new Sale(share, txWeek));
        }
      }
    }

    JsonArray weeklySnapshots = obj.getAsJsonArray("weeklySnapshots");
    if (weeklySnapshots != null) {
      List<Player.WeeklySnapshot> snapshots = new ArrayList<>();
      for (JsonElement el : weeklySnapshots) {
        JsonObject s = el.getAsJsonObject();
        int snapWeek = s.get("week").getAsInt();
        BigDecimal cashValue = new BigDecimal(s.get("cash").getAsString());
        BigDecimal portfolioValue = new BigDecimal(s.get("portfolioValue").getAsString());
        BigDecimal netWorthValue = new BigDecimal(s.get("netWorth").getAsString());
        snapshots.add(
            new Player.WeeklySnapshot(snapWeek, cashValue, portfolioValue, netWorthValue));
      }
      player.setWeeklySnapshots(snapshots);
    }
    if (player.getWeeklySnapshots().isEmpty()) {
      player.recordWeeklySnapshot(Math.max(1, exchange.getWeek()));
    }

    // ── Restore UI state (optional — absent in saves from older versions) ──
    GameUiState uiState = null;
    if (obj.has("uiState") && obj.get("uiState").isJsonObject()) {
      JsonObject us = obj.getAsJsonObject("uiState");
      List<String> favs = new ArrayList<>();
      if (us.has("favorites")) {
        for (JsonElement el : us.getAsJsonArray("favorites")) {
          favs.add(el.getAsString());
        }
      }
      List<String> filters = new ArrayList<>();
      if (us.has("activeFilters")) {
        for (JsonElement el : us.getAsJsonArray("activeFilters")) {
          filters.add(el.getAsString());
        }
      }
      List<String> chips = new ArrayList<>();
      if (us.has("filterChipOrder")) {
        for (JsonElement el : us.getAsJsonArray("filterChipOrder")) {
          chips.add(el.getAsString());
        }
      }
      String sort = us.has("stockSort") ? us.get("stockSort").getAsString() : "NAME";
      String sel = us.has("selectedSymbol") && !us.get("selectedSymbol").isJsonNull()
          ? us.get("selectedSymbol").getAsString() : null;
      uiState = new GameUiState(favs, filters, chips, sort, sel);
    }

    return new Object[] {player, exchange, uiState};
  }

  // ── Internal ──────────────────────────────────────────────────────────────

  private static SaveMeta readMeta(Path saveDir, Path jsonPath) throws IOException {
    String raw = Files.readString(jsonPath, StandardCharsets.UTF_8);
    JsonObject obj = GSON.fromJson(raw, JsonObject.class);

    String playerName = obj.get("playerName").getAsString();
    String displayName = obj.has("displayName") && !obj.get("displayName").isJsonNull()
        ? obj.get("displayName").getAsString().strip()
        : playerName;
    if (displayName.isBlank()) {
      displayName = playerName;
    }
    String profileAvatar = obj.has("profileAvatar") && !obj.get("profileAvatar").isJsonNull()
        ? obj.get("profileAvatar").getAsString()
        : "\uD83E\uDDD1";
    int weeksUsingChick =
        obj.has("weeksUsingChickAvatar") && !obj.get("weeksUsingChickAvatar").isJsonNull()
            ? obj.get("weeksUsingChickAvatar").getAsInt()
            : deriveLegacyChickPhase(profileAvatar) * 10;
    int chickPhaseUnlocked =
        weeksUsingChick >= 30 ? 3 : weeksUsingChick >= 20 ? 2 : weeksUsingChick >= 10 ? 1 : 0;
    profileAvatar = AvatarUtil.getDisplayAvatarStem(profileAvatar, chickPhaseUnlocked);
    String exchangeName = obj.get("exchangeName").getAsString();
    String savedAt = obj.get("savedAt").getAsString();
    int week = obj.get("week").getAsInt();
    String money = obj.get("money").getAsString();
    String netWorth = obj.has("netWorth") ? obj.get("netWorth").getAsString() : money;
    boolean isAutosave = obj.has("autosave") && obj.get("autosave").getAsBoolean();
    String status = obj.has("status") ? obj.get("status").getAsString() : "NOVICE";

    JsonArray portfolio = obj.getAsJsonArray("portfolio");
    int portfolioSize = portfolio != null ? portfolio.size() : 0;
    int totalShares = 0;
    if (portfolio != null) {
      for (JsonElement el : portfolio) {
        JsonObject s = el.getAsJsonObject();
        if (s.has("quantity")) {
          try {
            totalShares += new java.math.BigDecimal(s.get("quantity").getAsString()).intValue();
          } catch (NumberFormatException ignored) {
          }
        }
      }
    }

    return new SaveMeta(saveDir, displayName, profileAvatar, exchangeName, savedAt,
        week, money, netWorth, portfolioSize, totalShares, status, isAutosave);
  }

  private static String normalizeAutosaveSlot(String folderName) {
    String normalized = folderName;
    while (normalized.startsWith("autosave_")) {
      normalized = normalized.substring("autosave_".length());
    }
    return normalized;
  }

  private static int deriveLegacyChickPhase(String avatar) {
    if (avatar == null) {
      return 0;
    }
    return switch (avatar) {
      case "cracking-egg" -> 1;
      case "hatching-chick" -> 2;
      case "chick" -> 3;
      default -> 0;
    };
  }
}
