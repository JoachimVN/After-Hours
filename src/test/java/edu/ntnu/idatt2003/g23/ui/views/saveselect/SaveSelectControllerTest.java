package edu.ntnu.idatt2003.g23.ui.views.saveselect;

import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Stock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SaveSelectControllerTest {

  // ── handleBack ────────────────────────────────────────────────────────────

  @Test
  void handleBack_firesCallback() {
    AtomicBoolean fired = new AtomicBoolean(false);
    SaveSelectController ctrl = noSessionController(() -> fired.set(true), () -> {
    });
    ctrl.handleBack();
    assertTrue(fired.get());
  }

  // ── handleNewGame ─────────────────────────────────────────────────────────

  @Test
  void handleNewGame_firesCallback() {
    AtomicBoolean fired = new AtomicBoolean(false);
    SaveSelectController ctrl = noSessionController(() -> {
    }, () -> fired.set(true));
    ctrl.handleNewGame();
    assertTrue(fired.get());
  }

  // ── hasSession ────────────────────────────────────────────────────────────

  @Test
  void hasSession_withNullPlayer_isFalse() {
    SaveSelectController ctrl = noSessionController(() -> {
    }, () -> {
    });
    assertFalse(ctrl.hasSession());
  }

  @Test
  void hasSession_withPlayer_isTrue() {
    Player p = new Player("Test", new BigDecimal("1000"));
    Exchange e = new Exchange("Market",
        List.of(new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("100")))));
    SaveSelectController ctrl = withSessionController(p, e);
    assertTrue(ctrl.hasSession());
  }

  // ── resumeSession ─────────────────────────────────────────────────────────

  @Test
  void resumeSession_firesOnLoadWithCorrectPlayer() {
    Player p = new Player("Alice", new BigDecimal("500"));
    Exchange e = new Exchange("Market",
        List.of(new Stock("AAPL", "Apple Inc.", List.of(new BigDecimal("100")))));
    AtomicReference<Object[]> captured = new AtomicReference<>();
    SaveSelectController ctrl = new SaveSelectController(
        () -> {
        }, () -> {
    }, captured::set, null, p, e, null, null, false, false);
    ctrl.resumeSession();
    assertNotNull(captured.get());
    assertSame(p, captured.get()[0]);
  }

  // ── loadSaveList ─────────────────────────────────────────────────────────

  @Test
  void loadSaveList_withNoSavesOnDisk_returnsEmptyOrList() {
    SaveSelectController ctrl = noSessionController(() -> {
    }, () -> {
    });
    // Should not throw; may return empty list if no saves exist
    assertNotNull(ctrl.loadSaveList());
  }

  // ── loadSave — invalid path ───────────────────────────────────────────────

  @Test
  void loadSave_nonExistentPath_returnsErrorMessage(@TempDir Path tempDir) {
    SaveSelectController ctrl = noSessionController(() -> {
    }, () -> {
    });
    Path bogus = tempDir.resolve("does_not_exist");
    String error = ctrl.loadSave(bogus);
    assertNotNull(error, "Should return an error message for a non-existent save directory");
  }

  // ── deleteSave — invalid path ─────────────────────────────────────────────

  @Test
  void deleteSave_nonExistentPath_returnsErrorMessage(@TempDir Path tempDir) {
    SaveSelectController ctrl = noSessionController(() -> {
    }, () -> {
    });
    Path bogus = tempDir.resolve("bogus_save");
    String error = ctrl.deleteSave(bogus);
    assertNotNull(error, "Should return an error message when save directory does not exist");
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private static SaveSelectController noSessionController(Runnable onBack, Runnable onNewGame) {
    return new SaveSelectController(onNewGame, onBack, arr -> {
    }, null, null, null, null, null, false, false);
  }

  private static SaveSelectController withSessionController(Player player, Exchange exchange) {
    return new SaveSelectController(() -> {
    }, () -> {
    }, arr -> {
    }, null, player, exchange, null, null, false, false);
  }
}
