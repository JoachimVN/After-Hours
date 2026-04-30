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
  void parseCash_blank_returnsInvalid() {
    assertTrue(SetupController.parseCash("") < 0);
  }

  @Test
  void parseCash_negative_returnsInvalid() {
    assertTrue(SetupController.parseCash("-100") < 0);
  }

  @Test
  void parseCash_notANumber_returnsInvalid() {
    assertTrue(SetupController.parseCash("abc") < 0);
  }

  @Test
  void parseCash_zero_returnsInvalid() {
    assertTrue(SetupController.parseCash("0") < 0);
  }

  @Test
  void parseCash_invalidSuffixInput_returnsInvalid() {
    assertTrue(SetupController.parseCash("1T") < 0);
  }

  @Test
  void parseCash_kSuffixInput_returnsValue() {
    assertEquals(1500.0, SetupController.parseCash("1.5k"), 1e-9);
  }

  @Test
  void parseCash_decimalInput_returnsValue() {
    assertEquals(1234.56, SetupController.parseCash("1234.56"), 1e-6);
  }

  @Test
  void parseCash_atTrillion_returnsInvalid() {
    assertTrue(SetupController.parseCash("1000000000000") < 0);
  }

  @Test
  void parseCash_aboveTrillionViaSuffix_returnsInvalid() {
    assertTrue(SetupController.parseCash("1001B") < 0);
  }

  // ── cashValidationMessage ─────────────────────────────────────────────────

  @Test
  void cashValidationMessage_blank_returnsNull() {
    assertNull(SetupController.cashValidationMessage(""));
    assertNull(SetupController.cashValidationMessage("   "));
    assertNull(SetupController.cashValidationMessage(null));
  }

  @Test
  void cashValidationMessage_notANumber_returnsMessage() {
    String msg = SetupController.cashValidationMessage("abc");
    assertNotNull(msg);
    assertTrue(msg.toLowerCase().contains("valid"));
  }

  @Test
  void cashValidationMessage_negative_returnsMessage() {
    String msg = SetupController.cashValidationMessage("-500");
    assertNotNull(msg);
    assertTrue(msg.toLowerCase().contains("positive"));
  }

  @Test
  void cashValidationMessage_zero_returnsMessage() {
    String msg = SetupController.cashValidationMessage("0");
    assertNotNull(msg);
    assertTrue(msg.toLowerCase().contains("greater"));
  }

  @Test
  void cashValidationMessage_tooLarge_returnsMessage() {
    String msg = SetupController.cashValidationMessage("1001B");
    assertNotNull(msg);
    assertTrue(msg.toLowerCase().contains("large") || msg.toLowerCase().contains("trillion"));
  }

  @Test
  void cashValidationMessage_valid_returnsNull() {
    assertNull(SetupController.cashValidationMessage("5000"));
    assertNull(SetupController.cashValidationMessage("1.5k"));
    assertNull(SetupController.cashValidationMessage("2m"));
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
  void isCashReady_noPreset_invalidCash_isFalse() {
    SetupController ctrl = makeController();
    assertFalse(ctrl.isCashReady(false, "0"));
    assertFalse(ctrl.isCashReady(false, "abc"));
    assertFalse(ctrl.isCashReady(false, "1T"));
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
