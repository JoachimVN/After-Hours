package edu.ntnu.idatt2003.g23.ui.views.setup;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SetupControllerTest {

  // ── parseCash ─────────────────────────────────────────────────────────────

  @Test
  void parseCash_validPositive_returnsValue() {
    assertEquals(5000.0, SetupController.parseCash("5000"), 1e-9);
  }

  @Test
  void parseCash_blank_returnsDefault() {
    assertEquals(10_000.0, SetupController.parseCash(""), 1e-9);
  }

  @Test
  void parseCash_negative_returnsDefault() {
    assertEquals(10_000.0, SetupController.parseCash("-100"), 1e-9);
  }

  @Test
  void parseCash_notANumber_returnsDefault() {
    assertEquals(10_000.0, SetupController.parseCash("abc"), 1e-9);
  }

  @Test
  void parseCash_zero_returnsDefault() {
    assertEquals(10_000.0, SetupController.parseCash("0"), 1e-9);
  }

  @Test
  void parseCash_decimalInput_returnsValue() {
    assertEquals(1234.56, SetupController.parseCash("1234.56"), 1e-6);
  }

  // ── isCashReady ───────────────────────────────────────────────────────────

  @Test
  void isCashReady_presetSelected_alwaysTrue() {
    SetupController ctrl = makeController();
    assertTrue(ctrl.isCashReady(true, ""));
    assertTrue(ctrl.isCashReady(true, "bad"));
  }

  @Test
  void isCashReady_noPreset_blankText_isFalse() {
    SetupController ctrl = makeController();
    assertFalse(ctrl.isCashReady(false, ""));
  }

  @Test
  void isCashReady_noPreset_validCash_isTrue() {
    SetupController ctrl = makeController();
    assertTrue(ctrl.isCashReady(false, "5000"));
  }

  @Test
  void isCashReady_noPreset_zeroCash_parsesToDefault_isTrue() {
    // "0" is invalid cash, parseCash returns the default 10_000 which is > 0,
    // so the controller treats the field as ready.
    SetupController ctrl = makeController();
    assertTrue(ctrl.isCashReady(false, "0"));
  }

  // ── handleBack ────────────────────────────────────────────────────────────

  @Test
  void handleBack_firesCallback() {
    AtomicBoolean fired = new AtomicBoolean(false);
    SetupController ctrl = new SetupController(
        () -> fired.set(true),
        (name, cash, csv) -> {
        },
        (name, cash) -> {
        }
    );
    ctrl.handleBack();
    assertTrue(fired.get());
  }

  // ── handleStart with default market ──────────────────────────────────────

  @Test
  void handleStart_defaultStocks_firesOnStartDefault() {
    AtomicReference<String> capturedName = new AtomicReference<>();
    AtomicReference<Double> capturedCash = new AtomicReference<>();
    SetupController ctrl = new SetupController(
        () -> {
        },
        (name, cash, csv) -> {
          capturedName.set(name);
          capturedCash.set(cash);
        },
        (name, cash) -> {
        }
    );
    ctrl.setUseDefaultStocks(true);
    ctrl.selectMarket(0);
    ctrl.handleStart("Alice", "7500");

    assertEquals("Alice", capturedName.get());
    assertEquals(7500.0, capturedCash.get(), 1e-9);
  }

  @Test
  void handleStart_blankName_defaultsToPlayer() {
    AtomicReference<String> capturedName = new AtomicReference<>();
    SetupController ctrl = new SetupController(
        () -> {
        },
        (name, cash, csv) -> capturedName.set(name),
        (name, cash) -> {
        }
    );
    ctrl.setUseDefaultStocks(true);
    ctrl.handleStart("  ", "5000");
    assertEquals("Player", capturedName.get());
  }

  @Test
  void handleStart_csvMode_firesOnStartCsv() {
    AtomicBoolean csvFired = new AtomicBoolean(false);
    SetupController ctrl = new SetupController(
        () -> {
        },
        (name, cash, csv) -> {
        },
        (name, cash) -> csvFired.set(true)
    );
    ctrl.setUseDefaultStocks(false);
    ctrl.handleStart("Bob", "3000");
    assertTrue(csvFired.get());
  }

  // ── selectMarket ─────────────────────────────────────────────────────────

  @Test
  void selectMarket_storesIndex() {
    AtomicReference<String> capturedCsv = new AtomicReference<>();
    SetupController ctrl = new SetupController(
        () -> {
        },
        (name, cash, csv) -> capturedCsv.set(csv),
        (name, cash) -> {
        }
    );
    ctrl.setUseDefaultStocks(true);
    ctrl.selectMarket(1);
    ctrl.handleStart("X", "1000");
    // Just verify it doesn't crash when market index 1 is selected
    // (second built-in market exists in AppConfig.BUILT_IN_MARKETS)
    assertNotNull(capturedCsv.get());
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private static SetupController makeController() {
    return new SetupController(() -> {
    }, (n, c, csv) -> {
    }, (n, c) -> {
    });
  }
}
