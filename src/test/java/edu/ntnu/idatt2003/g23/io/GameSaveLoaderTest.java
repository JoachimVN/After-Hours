package edu.ntnu.idatt2003.g23.io;

import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
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
    @DisplayName("load returns an array with a Player and Exchange")
    void loadReturnsPlayerAndExchange() throws IOException {
        Object[] result = GameSaveLoader.load(saveDir);
        assertNotNull(result);
        assertEquals(3, result.length);
        assertInstanceOf(Player.class, result[0]);
        assertInstanceOf(Exchange.class, result[1]);
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

    // ─── Private constructor ──────────────────────────────────────────────────

    @Test
    @DisplayName("Private constructor does not throw")
    void testPrivateConstructor() throws Exception {
        var constructor = GameSaveLoader.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertDoesNotThrow(() -> constructor.newInstance());
    }
}
