package edu.ntnu.idatt2003.g23.io;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import edu.ntnu.idatt2003.g23.io.GameUiState;
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
import java.util.List;

/**
 * Lists and loads game saves from {@link GameSaveExporter#SAVES_DIR}.
 */
public final class GameSaveLoader {

    private static final Gson GSON = new Gson();

    private GameSaveLoader() {}

    // ── Public record exposed to callers ──────────────────────────────────────

    /**
     * Metadata shown in the save-selection UI without loading the full game.
     */
    public record SaveMeta(
            Path   saveDir,
            String displayName,
            String exchangeName,
            String savedAt,
            int    week,
            String money,
            String netWorthHint,
            int    portfolioSize,
            int    totalShares,
            String status,
            boolean autosave) {

        public String id() { return saveDir.getFileName().toString(); }
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
                    if (!Files.exists(json)) return;
                    try { result.add(readMeta(saveDir, json)); } catch (IOException ignored) {}
                });
            }
        }

        // Autosaves (listed separately — dedicated folder)
        Path autoDir = GameSaveExporter.AUTOSAVE_DIR;
        if (Files.exists(autoDir)) {
            try (var stream = Files.list(autoDir)) {
                stream.filter(Files::isDirectory).forEach(saveDir -> {
                    Path json = saveDir.resolve("save.json");
                    if (!Files.exists(json)) return;
                    try { result.add(readMeta(saveDir, json)); } catch (IOException ignored) {}
                });
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
        Path parent = saveDir.getParent();
        Path target = parent.resolve(newFolderName);
        return Files.move(saveDir, target);
    }

    /**
     * Deletes a save folder and all its contents.
     */
    public static void deleteSave(Path saveDir) throws IOException {
        try (var stream = Files.walk(saveDir)) {
            var paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path p : paths) Files.delete(p);
        }
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    /**
     * Fully loads a save into a live {@link Player} and {@link Exchange}.
     *
     * @param saveDir the save folder to load from
     * @return a two-element array: {@code [player, exchange]}
     * @throws IOException if reading fails
     * @throws IllegalStateException if the save data is corrupt
     */
    public static Object[] load(Path saveDir) throws IOException {
        Path jsonPath = saveDir.resolve("save.json");
        Path csvPath  = saveDir.resolve("stocks.csv");

        String raw = Files.readString(jsonPath, StandardCharsets.UTF_8);
        JsonObject obj = GSON.fromJson(raw, JsonObject.class);

        String playerName   = obj.get("playerName").getAsString();
        BigDecimal starting = new BigDecimal(obj.get("startingMoney").getAsString());
        BigDecimal money    = new BigDecimal(obj.get("money").getAsString());
        String statusStr    = obj.get("status").getAsString();
        int week            = obj.get("week").getAsInt();
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
                String symbol       = s.get("symbol").getAsString();
                BigDecimal qty      = new BigDecimal(s.get("quantity").getAsString());
                BigDecimal purchase = new BigDecimal(s.get("purchasePrice").getAsString());

                // Find the matching Stock object from the exchange
                if (!exchange.hasStock(symbol)) continue;
                Stock stock = exchange.getStock(symbol);
                player.getPortfolio().addShare(new Share(stock, qty, purchase));
            }
        }

        // ── Restore transaction history ────────────────────────────────────
        JsonArray transactions = obj.getAsJsonArray("transactions");
        if (transactions != null) {
            for (JsonElement el : transactions) {
                JsonObject t = el.getAsJsonObject();
                String type     = t.get("type").getAsString();
                String symbol   = t.get("symbol").getAsString();
                BigDecimal qty  = new BigDecimal(t.get("quantity").getAsString());
                BigDecimal price = new BigDecimal(t.get("purchasePrice").getAsString());
                int txWeek      = t.get("week").getAsInt();

                if (!exchange.hasStock(symbol)) continue;
                Stock stock = exchange.getStock(symbol);
                Share share = new Share(stock, qty, price);

                if ("BUY".equals(type)) {
                    player.getTransactionArchive().add(new Purchase(share, txWeek));
                } else {
                    player.getTransactionArchive().add(new Sale(share, txWeek));
                }
            }
        }

        // ── Restore UI state (optional — absent in saves from older versions) ──
        GameUiState uiState = null;
        if (obj.has("uiState") && obj.get("uiState").isJsonObject()) {
            JsonObject us = obj.getAsJsonObject("uiState");
            List<String> favs = new ArrayList<>();
            if (us.has("favorites"))
                for (JsonElement el : us.getAsJsonArray("favorites")) favs.add(el.getAsString());
            List<String> filters = new ArrayList<>();
            if (us.has("activeFilters"))
                for (JsonElement el : us.getAsJsonArray("activeFilters")) filters.add(el.getAsString());
            List<String> chips = new ArrayList<>();
            if (us.has("filterChipOrder"))
                for (JsonElement el : us.getAsJsonArray("filterChipOrder")) chips.add(el.getAsString());
            String sort = us.has("stockSort") ? us.get("stockSort").getAsString() : "NAME";
            String sel  = us.has("selectedSymbol") && !us.get("selectedSymbol").isJsonNull()
                          ? us.get("selectedSymbol").getAsString() : null;
            uiState = new GameUiState(favs, filters, chips, sort, sel);
        }

        return new Object[]{player, exchange, uiState};
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static SaveMeta readMeta(Path saveDir, Path jsonPath) throws IOException {
        String raw = Files.readString(jsonPath, StandardCharsets.UTF_8);
        JsonObject obj = GSON.fromJson(raw, JsonObject.class);

        String playerName   = obj.get("playerName").getAsString();
        String exchangeName = obj.get("exchangeName").getAsString();
        String savedAt      = obj.get("savedAt").getAsString();
        int week            = obj.get("week").getAsInt();
        String money        = obj.get("money").getAsString();
        String netWorth     = obj.has("netWorth") ? obj.get("netWorth").getAsString() : money;
        boolean isAutosave  = obj.has("autosave") && obj.get("autosave").getAsBoolean();
        String status       = obj.has("status") ? obj.get("status").getAsString() : "NOVICE";

        JsonArray portfolio = obj.getAsJsonArray("portfolio");
        int portfolioSize = portfolio != null ? portfolio.size() : 0;
        int totalShares = 0;
        if (portfolio != null) {
            for (JsonElement el : portfolio) {
                JsonObject s = el.getAsJsonObject();
                if (s.has("quantity")) {
                    try { totalShares += new java.math.BigDecimal(s.get("quantity").getAsString()).intValue(); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }

        return new SaveMeta(saveDir, playerName, exchangeName, savedAt,
                week, money, netWorth, portfolioSize, totalShares, status, isAutosave);
    }
}
