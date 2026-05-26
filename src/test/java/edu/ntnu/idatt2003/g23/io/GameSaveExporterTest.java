package edu.ntnu.idatt2003.g23.io;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
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

class GameSaveExporterTest {

  @TempDir
  Path tempDir;

  private Player player;
  private Exchange exchange;

  @BeforeEach
  void setUp() {
    Stock stock = new Stock("AAPL", "Apple Inc.",
        new ArrayList<>(List.of(new BigDecimal("150"), new BigDecimal("160"))));
    player = new Player("TestPlayer", new BigDecimal("1000"));
    exchange = new Exchange("TestExchange", List.of(stock));
  }

  // ─── overwrite ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("overwrite creates save.json in the target directory")
  void overwriteCreatesSaveJson() throws IOException {
    GameSaveExporter.overwrite(tempDir, player, exchange);
    assertTrue(Files.exists(tempDir.resolve("save.json")));
  }

  @Test
  @DisplayName("overwrite creates stocks.csv in the target directory")
  void overwriteCreatesStocksCsv() throws IOException {
    GameSaveExporter.overwrite(tempDir, player, exchange);
    assertTrue(Files.exists(tempDir.resolve("stocks.csv")));
  }

  @Test
  @DisplayName("save.json contains player name")
  void saveJsonContainsPlayerName() throws IOException {
    GameSaveExporter.overwrite(tempDir, player, exchange);
    String content = Files.readString(tempDir.resolve("save.json"), StandardCharsets.UTF_8);
    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
    assertEquals("TestPlayer", json.get("playerName").getAsString());
  }

  @Test
  @DisplayName("save.json contains starting money")
  void saveJsonContainsStartingMoney() throws IOException {
    GameSaveExporter.overwrite(tempDir, player, exchange);
    String content = Files.readString(tempDir.resolve("save.json"), StandardCharsets.UTF_8);
    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
    assertEquals(0,
        new BigDecimal("1000").compareTo(new BigDecimal(json.get("startingMoney").getAsString())));
  }

  @Test
  @DisplayName("save.json contains exchange name")
  void saveJsonContainsExchangeName() throws IOException {
    GameSaveExporter.overwrite(tempDir, player, exchange);
    String content = Files.readString(tempDir.resolve("save.json"), StandardCharsets.UTF_8);
    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
    assertEquals("TestExchange", json.get("exchangeName").getAsString());
  }

  @Test
  @DisplayName("save.json net worth equals player.getNetWorth()")
  void saveJsonNetWorthMatchesPlayerNetWorth() throws IOException {
    GameSaveExporter.overwrite(tempDir, player, exchange);
    String content = Files.readString(tempDir.resolve("save.json"), StandardCharsets.UTF_8);
    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
    BigDecimal savedNetWorth = new BigDecimal(json.get("netWorth").getAsString());
    assertEquals(0, player.getNetWorth().compareTo(savedNetWorth));
  }

  @Test
  @DisplayName("save.json persists chick avatar progression fields")
  void saveJsonPersistsChickAvatarProgression() throws IOException {
    player.setProfileAvatar("hatching-chick");
    player.setWeeksUsingChickAvatar(24);

    GameSaveExporter.overwrite(tempDir, player, exchange);

    String content = Files.readString(tempDir.resolve("save.json"), StandardCharsets.UTF_8);
    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
    assertEquals("egg", json.get("profileAvatar").getAsString());
    assertEquals(24, json.get("weeksUsingChickAvatar").getAsInt());
  }

  @Test
  @DisplayName("overwrite with null uiState does not write uiState field")
  void overwriteNullUiStateOmitsUiState() throws IOException {
    GameSaveExporter.overwrite(tempDir, player, exchange, null);
    String content = Files.readString(tempDir.resolve("save.json"), StandardCharsets.UTF_8);
    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
    // uiState should be absent or null in JSON
    assertTrue(!json.has("uiState") || json.get("uiState").isJsonNull());
  }

  @Test
  @DisplayName("overwrite replaces previous save content")
  void overwriteReplacesPreviousContent() throws IOException {
    GameSaveExporter.overwrite(tempDir, player, exchange);
    player.addMoney(new BigDecimal("500"));
    GameSaveExporter.overwrite(tempDir, player, exchange);

    String content = Files.readString(tempDir.resolve("save.json"), StandardCharsets.UTF_8);
    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
    BigDecimal savedMoney = new BigDecimal(json.get("money").getAsString());
    assertEquals(0, new BigDecimal("1500").compareTo(savedMoney));
  }

  // ─── autosave ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("autosave creates save.json in AUTOSAVE_DIR/<slot>")
  void autosaveCreatesSaveJson() throws IOException {
    Path autosaveDir = GameSaveExporter.autosave(player, exchange, null, "test_slot_abc");
    assertTrue(Files.exists(autosaveDir.resolve("save.json")));
    // cleanup
    Files.walk(autosaveDir).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
      try {
        Files.delete(p);
      } catch (IOException ignored) {
      }
    });
  }

  @Test
  @DisplayName("Two different slot IDs produce two separate autosave directories")
  void autosaveDifferentSlotsAreSeparate() throws IOException {
    Path dir1 = GameSaveExporter.autosave(player, exchange, null, "slot_one");
    Path dir2 = GameSaveExporter.autosave(player, exchange, null, "slot_two");

    assertNotEquals(dir1, dir2);
    assertTrue(Files.exists(dir1));
    assertTrue(Files.exists(dir2));

    // cleanup
    for (Path d : new Path[] {dir1, dir2}) {
      Files.walk(d).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.delete(p);
        } catch (IOException ignored) {
        }
      });
    }
  }

  @Test
  @DisplayName("Autosaving with the same slot ID overwrites (does not create new folder)")
  void autosaveSameSlotOverwrites() throws IOException {
    GameSaveExporter.autosave(player, exchange, null, "my_slot");
    player.addMoney(new BigDecimal("200"));
    Path dir = GameSaveExporter.autosave(player, exchange, null, "my_slot");

    String content =
        Files.readString(dir.resolve("save.json"), java.nio.charset.StandardCharsets.UTF_8);
    com.google.gson.JsonObject json =
        com.google.gson.JsonParser.parseString(content).getAsJsonObject();
    assertEquals(0,
        new BigDecimal("1200").compareTo(new BigDecimal(json.get("money").getAsString())));

    // cleanup
    Files.walk(dir).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
      try {
        Files.delete(p);
      } catch (IOException ignored) {
      }
    });
  }

  @Test
  @DisplayName("autosave normalizes empty slot IDs to default slot folder")
  void autosaveEmptySlotUsesDefaultFolderName() throws IOException {
    Path dir = GameSaveExporter.autosave(player, exchange, null, "");
    assertEquals("autosave_slot", dir.getFileName().toString());

    Files.walk(dir).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
      try {
        Files.delete(p);
      } catch (IOException ignored) {
      }
    });
  }

  @Test
  @DisplayName("autosave strips duplicated autosave_ prefixes from slot IDs")
  void autosaveNormalizesPrefixedSlotId() throws IOException {
    Path dir = GameSaveExporter.autosave(player, exchange, null, "autosave_autosave_demo");
    assertEquals("autosave_demo", dir.getFileName().toString());

    Files.walk(dir).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
      try {
        Files.delete(p);
      } catch (IOException ignored) {
      }
    });
  }

  @Test
  @DisplayName("markSaveAsFlagged sets flagged=true and removes legacy flags")
  void markSaveAsFlaggedSetsUnifiedFlag() throws IOException {
    GameSaveExporter.overwrite(tempDir, player, exchange);
    Path jsonPath = tempDir.resolve("save.json");

    JsonObject before = JsonParser.parseString(
        Files.readString(jsonPath, StandardCharsets.UTF_8)).getAsJsonObject();
    before.addProperty("flaggedDevMode", true);
    before.addProperty("flaggedCsvEdited", true);
    Files.writeString(jsonPath, before.toString(), StandardCharsets.UTF_8);

    GameSaveExporter.markSaveAsFlagged(tempDir);

    JsonObject after = JsonParser.parseString(
        Files.readString(jsonPath, StandardCharsets.UTF_8)).getAsJsonObject();
    assertTrue(after.get("flagged").getAsBoolean());
    assertFalse(after.has("flaggedDevMode"));
    assertFalse(after.has("flaggedCsvEdited"));
  }

  @Test
  @DisplayName("markSaveAsFlagged throws when save.json is missing")
  void markSaveAsFlaggedThrowsWithoutSaveJson() {
    Path missing = tempDir.resolve("missing_save");
    assertThrows(IOException.class, () -> GameSaveExporter.markSaveAsFlagged(missing));
  }

  @Test
  @DisplayName("exportSaveDataFiles writes paired json and csv files")
  void exportSaveDataFilesCopiesBothFiles() throws IOException {
    GameSaveExporter.overwrite(tempDir, player, exchange);

    Path destinationBase = tempDir.resolve("exports").resolve("session_export.any");
    Path[] out = GameSaveExporter.exportSaveDataFiles(tempDir, destinationBase);

    assertEquals(2, out.length);
    assertTrue(Files.exists(out[0]));
    assertTrue(Files.exists(out[1]));
    assertTrue(out[0].toString().endsWith("session_export.json"));
    assertTrue(out[1].toString().endsWith("session_export.csv"));
  }

  @Test
  @DisplayName("exportSaveCsvFile can export latest price only")
  void exportSaveCsvFileLatestPriceOnlyWritesSinglePriceColumn() throws IOException {
    GameSaveExporter.overwrite(tempDir, player, exchange);

    Path destinationBase = tempDir.resolve("exports").resolve("latest_only.any");
    Path out = GameSaveExporter.exportSaveCsvFile(tempDir, destinationBase, true);

    String csv = Files.readString(out, StandardCharsets.UTF_8);
    assertTrue(csv.startsWith("symbol,company,price"));
    assertTrue(csv.contains("AAPL,Apple Inc.,160.00"));
    assertFalse(csv.contains(";"));
  }

  @Test
  @DisplayName("exportSaveDataFiles can export json plus latest-price csv")
  void exportSaveDataFilesLatestPriceOnlyWritesCurrentPriceCsv() throws IOException {
    GameSaveExporter.overwrite(tempDir, player, exchange);

    Path destinationBase = tempDir.resolve("exports").resolve("latest_bundle.any");
    Path[] out = GameSaveExporter.exportSaveDataFiles(tempDir, destinationBase, true);

    assertTrue(Files.exists(out[0]));
    assertTrue(Files.exists(out[1]));

    String csv = Files.readString(out[1], StandardCharsets.UTF_8);
    assertTrue(csv.startsWith("symbol,company,price"));
    assertTrue(csv.contains("AAPL,Apple Inc.,160.00"));
    assertFalse(csv.contains(";"));
  }

  @Test
  @DisplayName("exportSaveDataFiles validates null arguments")
  void exportSaveDataFilesRejectsNullArguments() {
    assertThrows(IllegalArgumentException.class,
        () -> GameSaveExporter.exportSaveDataFiles(null, tempDir.resolve("x")));
    assertThrows(IllegalArgumentException.class,
        () -> GameSaveExporter.exportSaveDataFiles(tempDir, null));
  }

  @Test
  @DisplayName("exportSaveDataFiles throws when required save files are missing")
  void exportSaveDataFilesThrowsWhenRequiredFilesMissing() throws IOException {
    Path incompleteSave = tempDir.resolve("incomplete-save");
    Files.createDirectories(incompleteSave);
    Files.writeString(incompleteSave.resolve("save.json"), "{}", StandardCharsets.UTF_8);

    assertThrows(IOException.class,
        () -> GameSaveExporter.exportSaveDataFiles(incompleteSave, tempDir.resolve("out")));
  }

  // ─── Private constructor ──────────────────────────────────────────────────

  @Test
  @DisplayName("Private constructor does not throw")
  void testPrivateConstructor() throws Exception {
    var constructor = GameSaveExporter.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    assertDoesNotThrow(() -> constructor.newInstance());
  }
}
