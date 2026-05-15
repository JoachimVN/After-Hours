package edu.ntnu.idatt2003.g23.ui.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AvatarUtilTest {

  @Test
  @DisplayName("loadSelectableAvatarNames loads the ordered picker list from resources")
  void loadSelectableAvatarNamesLoadsOrderedResourceList() {
    List<String> avatars = AvatarUtil.loadSelectableAvatarNames();

    assertFalse(avatars.isEmpty());
    assertTrue(avatars.contains("boy"));
    assertTrue(avatars.contains("girl"));
    assertTrue(avatars.contains("egg"));
    assertFalse(avatars.contains("bust-in-silhouette"));
    assertFalse(avatars.contains("fox"));
    assertFalse(avatars.contains("bear"));
    assertFalse(avatars.contains("lion"));
    assertFalse(avatars.contains("mouse"));
    assertFalse(avatars.contains("octopus"));
    assertFalse(avatars.contains("panda"));
    assertFalse(avatars.contains("raccoon"));
    assertFalse(avatars.contains("wolf"));
  }

  @Test
  @DisplayName("loadSelectableAvatarRows keeps humans on the first row and animals on the second")
  void loadSelectableAvatarRowsKeepsTwoExplicitRows() {
    List<List<String>> rows = AvatarUtil.loadSelectableAvatarRows();

    assertEquals(2, rows.size());
    assertTrue(rows.get(0).contains("boy"));
    assertTrue(rows.get(0).contains("girl"));
    assertTrue(rows.get(1).contains("dog"));
    assertTrue(rows.get(1).contains("egg"));
    assertFalse(rows.get(0).contains("dog"));
    assertFalse(rows.get(1).contains("boy"));
  }
}
