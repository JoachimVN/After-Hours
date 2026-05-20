package edu.ntnu.idatt2003.g23.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.PlayerStatus;
import edu.ntnu.idatt2003.g23.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameSaveLoaderTest {

  @TempDir
  Path tempDir;

  private Player player;
  private Exchange exchange;
  private Path saveDir;

  @BeforeEach
  void setUp() throws IOException {
    Stock stock = new Stock("AAPL", "Apple Inc.", new ArrayList<>(List.of(new BigDecimal("150"))));
    player = new Player("LoadTest", new BigDecimal("2000"));
    exchange = new Exchange("TestExchange", List.of(stock));

    // Create a save in a subdirectory of tempDir so we can test load/rename/delete
    saveDir = tempDir.resolve("test_save");
    Files.createDirectories(saveDir);
    GameSaveExporter.overwrite(saveDir, player, exchange);
  }

  // ─── load ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("load returns player, exchange, ui state, and flagged state")
  void loadReturnsPlayerAndExchange() throws IOException {
    Object[] result = GameSaveLoader.load(saveDir);
    assertNotNull(result);
    assertEquals(4, result.length);
    assertInstanceOf(Player.class, result[0]);
    assertInstanceOf(Exchange.class, result[1]);
    assertNull(result[2]);
    assertEquals(Boolean.FALSE, result[3]);
  }

  @Test
  @DisplayName("load restores player name correctly")
  void loadRestoresPlayerName() throws IOException {
    Object[] result = GameSaveLoader.load(saveDir);
    Player loaded = (Player) result[0];
    assertEquals("LoadTest", loaded.getName());
  }

  @Test
  @DisplayName("load restores player money correctly")
  void loadRestoresPlayerMoney() throws IOException {
    Object[] result = GameSaveLoader.load(saveDir);
    Player loaded = (Player) result[0];
    assertEquals(0, new BigDecimal("2000").compareTo(loaded.getMoney()));
  }

  @Test
  @DisplayName("load restores exchange name correctly")
  void loadRestoresExchangeName() throws IOException {
    Object[] result = GameSaveLoader.load(saveDir);
    Exchange loaded = (Exchange) result[1];
    assertEquals("TestExchange", loaded.getName());
  }

  @Test
  @DisplayName("load restores chick avatar selection and progression")
  void loadRestoresChickAvatarProgression() throws IOException {
    player.setProfileAvatar("cracking-egg");
    player.setWeeksUsingChickAvatar(17);
    GameSaveExporter.overwrite(saveDir, player, exchange);

    Object[] result = GameSaveLoader.load(saveDir);
    Player loaded = (Player) result[0];

    assertEquals("egg", loaded.getProfileAvatar());
    assertEquals("cracking-egg", loaded.getDisplayedProfileAvatar());
    assertEquals(17, loaded.getWeeksUsingChickAvatar());
    assertEquals(1, loaded.getChickPhaseUnlocked());
  }

  @Test
  @DisplayName("load throws IOException when save.json is missing")
  void loadThrowsWhenSaveJsonMissing() throws IOException {
    Path emptyDir = tempDir.resolve("no_save");
    Files.createDirectories(emptyDir);
    assertThrows(IOException.class, () -> GameSaveLoader.load(emptyDir));
  }

  // ─── deleteSave ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("deleteSave removes the directory")
  void deleteSaveRemovesDirectory() throws IOException {
    assertTrue(Files.exists(saveDir));
    GameSaveLoader.deleteSave(saveDir);
    assertFalse(Files.exists(saveDir));
  }

  @Test
  @DisplayName("deleteSave throws IOException for non-existent path")
  void deleteSaveThrowsForNonExistentPath() {
    Path nonExistent = tempDir.resolve("does_not_exist");
    assertThrows(IOException.class, () -> GameSaveLoader.deleteSave(nonExistent));
  }

  // ─── renameSave ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("renameSave moves directory to new name")
  void renameSaveMovesDirectory() throws IOException {
    Path renamed = GameSaveLoader.renameSave(saveDir, "renamed_save");
    assertFalse(Files.exists(saveDir));
    assertTrue(Files.exists(renamed));
  }

  @Test
  @DisplayName("renameSave returns path with new folder name")
  void renameSaveReturnsCorrectPath() throws IOException {
    Path renamed = GameSaveLoader.renameSave(saveDir, "new_folder_name");
    assertEquals("new_folder_name", renamed.getFileName().toString());
  }

  @Test
  @DisplayName("renameSave with display name updates playerName/displayName in JSON")
  void renameSaveWithDisplayNameUpdatesJson() throws IOException {
    Path renamed = GameSaveLoader.renameSave(saveDir, "renamed_with_display", "Visible Name");
    JsonObject json = JsonParser.parseString(
        Files.readString(renamed.resolve("save.json"), StandardCharsets.UTF_8)).getAsJsonObject();

    assertEquals("Visible Name", json.get("displayName").getAsString());
    assertEquals("Visible Name", json.get("playerName").getAsString());
  }

  @Test
  @DisplayName("load reads UI state and legacy flagged fields")
  void loadReadsUiStateAndLegacyFlagFields() throws IOException {
    JsonObject json = JsonParser.parseString(
        Files.readString(saveDir.resolve("save.json"), StandardCharsets.UTF_8)).getAsJsonObject();

    JsonObject uiState = new JsonObject();
    JsonArray favorites = new JsonArray();
    favorites.add("AAPL");
    JsonArray activeFilters = new JsonArray();
    activeFilters.add("OWNED");
    JsonArray chipOrder = new JsonArray();
    chipOrder.add("ALL");
    uiState.add("favorites", favorites);
    uiState.add("activeFilters", activeFilters);
    uiState.add("filterChipOrder", chipOrder);
    uiState.addProperty("stockSort", "PRICE");
    uiState.addProperty("selectedSymbol", "AAPL");
    uiState.addProperty("sidebarDivider", 0.2);
    uiState.addProperty("portfolioDivider", 0.7);

    json.add("uiState", uiState);
    json.addProperty("flaggedDevMode", true);
    Files.writeString(saveDir.resolve("save.json"), json.toString(), StandardCharsets.UTF_8);

    Object[] result = GameSaveLoader.load(saveDir);
    assertInstanceOf(GameUiState.class, result[2]);
    GameUiState restored = (GameUiState) result[2];
    assertEquals(List.of("AAPL"), restored.favorites());
    assertEquals("PRICE", restored.stockSort());
    assertEquals(Boolean.TRUE, result[3]);
  }

  @Test
  @DisplayName("load falls back to NOVICE when status is invalid")
  void loadWithInvalidStatusFallsBackToNovice() throws IOException {
    JsonObject json = JsonParser.parseString(
        Files.readString(saveDir.resolve("save.json"), StandardCharsets.UTF_8)).getAsJsonObject();
    json.addProperty("status", "NOT_A_REAL_STATUS");
    Files.writeString(saveDir.resolve("save.json"), json.toString(), StandardCharsets.UTF_8);

    Object[] result = GameSaveLoader.load(saveDir);
    Player loaded = (Player) result[0];
    assertEquals(PlayerStatus.NOVICE, loaded.getStatus());
  }

  @Test
  @DisplayName("load skips unknown symbols in portfolio and transactions")
  void loadSkipsUnknownSymbolsInPortfolioAndTransactions() throws IOException {
    JsonObject json = JsonParser.parseString(
        Files.readString(saveDir.resolve("save.json"), StandardCharsets.UTF_8)).getAsJsonObject();

    JsonArray portfolio = new JsonArray();
    JsonObject unknownShare = new JsonObject();
    unknownShare.addProperty("symbol", "MISSING");
    unknownShare.addProperty("quantity", "5");
    unknownShare.addProperty("purchasePrice", "10");
    portfolio.add(unknownShare);
    json.add("portfolio", portfolio);

    JsonArray tx = new JsonArray();
    JsonObject unknownTx = new JsonObject();
    unknownTx.addProperty("type", "BUY");
    unknownTx.addProperty("symbol", "MISSING");
    unknownTx.addProperty("quantity", "2");
    unknownTx.addProperty("purchasePrice", "10");
    unknownTx.addProperty("week", 1);
    tx.add(unknownTx);
    json.add("transactions", tx);

    Files.writeString(saveDir.resolve("save.json"), json.toString(), StandardCharsets.UTF_8);

    Object[] result = GameSaveLoader.load(saveDir);
    Player loaded = (Player) result[0];
    assertTrue(loaded.getPortfolio().getShares().isEmpty());
    assertTrue(loaded.getTransactionArchive().getAll().isEmpty());
  }

  @Test
  @DisplayName("load records default weekly snapshot when snapshots are absent")
  void loadRecordsDefaultSnapshotWhenMissing() throws IOException {
    JsonObject json = JsonParser.parseString(
        Files.readString(saveDir.resolve("save.json"), StandardCharsets.UTF_8)).getAsJsonObject();
    json.remove("weeklySnapshots");
    Files.writeString(saveDir.resolve("save.json"), json.toString(), StandardCharsets.UTF_8);

    Object[] result = GameSaveLoader.load(saveDir);
    Player loaded = (Player) result[0];
    assertFalse(loaded.getWeeklySnapshots().isEmpty());
    assertEquals(1, loaded.getWeeklySnapshots().getFirst().week());
  }

  // ─── Private constructor ──────────────────────────────────────────────────

  @Test
  @DisplayName("Private constructor does not throw")
  void testPrivateConstructor() throws Exception {
    var constructor = GameSaveLoader.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    assertDoesNotThrow(() -> constructor.newInstance());
  }
}
