package edu.ntnu.idatt2003.g23.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.ntnu.idatt2003.g23.io.GameUiState;
import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Stock;

class GameSessionServiceTest {

  @Test
  void clearForNewGame_resetsAllSessionState() {
    GameSessionService session = new GameSessionService();
    session.setFlagged(true);
    session.setProfileAvatar("boy");
    session.setAutosaveId("abc");
    session.setSavePath(Path.of("saves", "slot"));
    session.setUiState(new GameUiState(List.of("AAPL"), List.of(), List.of(), "name", "AAPL", 0.5, 0.5));

    session.clearForNewGame();

    assertNull(session.getPlayer());
    assertNull(session.getExchange());
    assertNull(session.getSavePath());
    assertNull(session.getUiState());
    assertFalse(session.isFlagged());
    assertEquals("bust-in-silhouette", session.getProfileAvatar());
    assertNull(session.getAutosaveId());
  }

  @Test
  void startSession_withSavePath_setsCoreFieldsAndNormalizesAutosaveId() {
    GameSessionService session = new GameSessionService();
    session.setFlagged(true);

    Player player = new Player("Player One", new BigDecimal("1000"));
    Exchange exchange = sampleExchange();
    Path savePath = Path.of("saves", "autosave_main-slot");

    session.startSession(null, null, null, player, exchange, savePath);

    assertSame(player, session.getPlayer());
    assertSame(exchange, session.getExchange());
    assertEquals(savePath, session.getSavePath());
    assertEquals(player.getProfileAvatar(), session.getProfileAvatar());
    assertEquals("main-slot", session.getAutosaveId());
    assertTrue(session.isFlagged());
  }

  @Test
  void startSession_resumeSameInMemorySession_keepsPreviousAutosaveId() {
    GameSessionService session = new GameSessionService();
    session.setFlagged(true);

    Player player = new Player("Player One", new BigDecimal("1000"));
    Exchange exchange = sampleExchange();

    session.startSession(player, exchange, "persisted_slot", player, exchange, null);

    assertEquals("persisted_slot", session.getAutosaveId());
    assertTrue(session.isFlagged());
  }

  @Test
  void startSession_newUnsavedSession_resetsFlaggedAndGeneratesAutosaveId() {
    GameSessionService session = new GameSessionService();
    session.setFlagged(true);

    Player player = new Player("Player One", new BigDecimal("1000"));
    Exchange exchange = sampleExchange();

    session.startSession(null, null, null, player, exchange, null);

    assertFalse(session.isFlagged());
    assertNotNull(session.getAutosaveId());
    assertTrue(session.getAutosaveId().startsWith("Player_One_"));
  }

  @Test
  void normalizeAutosaveSlotId_stripsAutosavePrefixAndSanitizes() {
    String normalized = GameSessionService.normalizeAutosaveSlotId("autosave_autosave_slot 1!*?");
    assertEquals("slot_1___", normalized);
  }

  @Test
  void normalizeAutosaveSlotId_blankOrNull_returnsSlot() {
    assertEquals("slot", GameSessionService.normalizeAutosaveSlotId(""));
    assertEquals("slot", GameSessionService.normalizeAutosaveSlotId(null));
  }

  @Test
  void setProfileAvatar_ignoresNullAndBlankValues() {
    GameSessionService session = new GameSessionService();

    session.setProfileAvatar("boy");
    session.setProfileAvatar(" ");
    session.setProfileAvatar(null);

    assertEquals("boy", session.getProfileAvatar());
  }

  @Test
  void setUiState_roundTripsValue() {
    GameSessionService session = new GameSessionService();
    GameUiState uiState = new GameUiState(
        List.of("AAPL"),
        List.of("GAINERS"),
        List.of("GAINERS"),
        "price",
        "AAPL",
        0.25,
        0.75);

    session.setUiState(uiState);

    assertSame(uiState, session.getUiState());
  }

  private static Exchange sampleExchange() {
    Stock stock = new Stock("AAPL", "Apple", List.of(new BigDecimal("100"), new BigDecimal("101")));
    return new Exchange("Test", List.of(stock));
  }
}
