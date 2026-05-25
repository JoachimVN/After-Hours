package edu.ntnu.idatt2003.g23.ui.views.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SettingsControllerTest {

  @Test
  void constructor_setsNavigationFieldsAndLeavesOptionalFieldsNull() {
    Runnable back = () -> {
    };
    SettingsController controller = new SettingsController(back, null);

    assertEquals(back, controller.onBack);
    assertNull(controller.stage);
    assertNull(controller.onSave);
    assertNull(controller.currentSavePath);
    assertNull(controller.onNameChanged);
  }
}
