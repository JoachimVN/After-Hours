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

    /** Dedicated directory for autosave slots (one per player+exchange). */
    public static final Path AUTOSAVE_DIR =
            Path.of(System.getProperty("user.home"), ".afterhours", "autosaves");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FOLDER_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private GameSaveExporter() {}

    // ── DTOs written into save.json ───────────────────────────────────────────

    private record ShareDto(String symbol, String quantity, String purchasePrice) {}
    private record TransactionDto(String type, String symbol, String quantity,
                                  String purchasePrice, int week) {}
    private record UiStateJson(List<String> favorites, List<String> activeFilters,
                               List<String> filterChipOrder, String stockSort,
                               String selectedSymbol) {}
    private record SaveJson(
            String playerName,
            String startingMoney,
            String money,
            String netWorth,
            String status,
            int    week,
            String exchangeName,
            String savedAt,
            boolean autosave,
            List<ShareDto>       portfolio,
            List<TransactionDto> transactions,
            UiStateJson          uiState) {}

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

    public static Path save(Player player, Exchange exchange, GameUiState uiState) throws IOException {
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
        String safeSlot = slotId.replaceAll("[^A-Za-z0-9_\\-]", "_");
        String folderName = "autosave_" + safeSlot;
        Path saveDir = AUTOSAVE_DIR.resolve(folderName);
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

        UiStateJson uiStateJson = uiState == null ? null : new UiStateJson(
                List.copyOf(uiState.favorites()),
                List.copyOf(uiState.activeFilters()),
                List.copyOf(uiState.filterChipOrder()),
                uiState.stockSort(),
                uiState.selectedSymbol());

        BigDecimal netWorth = player.getNetWorth();

        SaveJson json = new SaveJson(
                player.getName(),
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
                uiStateJson);

        Path jsonFile = saveDir.resolve("save.json");
        Files.writeString(jsonFile, GSON.toJson(json), StandardCharsets.UTF_8);
    }
}
