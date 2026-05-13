package edu.ntnu.idatt2003.g23.ui.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SequencedSet;
import java.util.stream.Collectors;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public final class AvatarUtil {

  private static final String EMOJI_PATH = "/images/emojis/";
  private static final String AVATAR_ORDER_RESOURCE = "/data/avatar-order.txt";
  private static final String DEFAULT_AVATAR = "bust-in-silhouette";
  private static final String CHICK_BASE_STEM = "egg";
  private static final Set<String> CHICK_STEMS = Set.of(
      "egg",
      "cracking-egg",
      "hatching-chick",
      "chick");
  private static final SequencedSet<String> HIDDEN_SELECTABLE_AVATARS = new LinkedHashSet<>(List.of(
      "fox",
      "smiling-face-with-horns"));

  private AvatarUtil() {
  }

  /**
   * Returns avatar stems in the order defined by {@code order.txt} in the emojis
   * folder.
   * Any PNG not listed in order.txt is appended alphabetically at the end.
   * To reorder: edit src/main/resources/images/emojis/order.txt, one stem per
   * line.
   * Lines starting with # are treated as comments and ignored.
   */
  public static List<String> loadAvatarNames() {
    try (var input = AvatarUtil.class.getResourceAsStream(AVATAR_ORDER_RESOURCE)) {
      if (input == null) {
        return List.of(DEFAULT_AVATAR);
      }
      try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
        return reader.lines()
            .map(String::strip)
            .map(AvatarUtil::normalizeAvatarStem)
            .filter(line -> !line.isBlank() && !line.startsWith("#"))
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));
      }
    } catch (Exception e) {
      return List.of(DEFAULT_AVATAR);
    }
  }

  /**
   * Returns the correct chick avatar stem for a given phase (0-3).
   */
  public static String getChickPhaseStem(int phase) {
    return switch (phase) {
      case 0 -> "egg";
      case 1 -> "cracking-egg";
      case 2 -> "hatching-chick";
      case 3 -> "chick";
      default -> "egg";
    };
  }

  /**
   * Returns selectable avatars (all avatars except the default silhouette).
   * Used for the profile avatar picker.
   */
  public static List<String> loadSelectableAvatarNames() {
    List<String> names = new ArrayList<>();

    try (var input = AvatarUtil.class.getResourceAsStream(AVATAR_ORDER_RESOURCE)) {
      if (input == null) {
        return List.of(DEFAULT_AVATAR);
      }
      try (var reader = new java.io.BufferedReader(
          new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))) {
        reader.lines()
            .map(String::strip)
            .map(AvatarUtil::normalizeAvatarStem)
            .filter(line -> !line.isBlank() && !line.startsWith("#"))
            .filter(stem -> !DEFAULT_AVATAR.equals(stem))
            .filter(stem -> !HIDDEN_SELECTABLE_AVATARS.contains(stem))
            .distinct()
            .forEach(names::add);
      }
    } catch (Exception e) {
      return List.of(DEFAULT_AVATAR);
    }

    if (names.isEmpty()) {
      return List.of(DEFAULT_AVATAR);
    }
    return names;
  }

  /**
   * Returns the selectable avatars grouped into picker rows.
   * The current resource order keeps humans on the first row and animals on the second row.
   */
  public static List<List<String>> loadSelectableAvatarRows() {
    List<String> names = loadSelectableAvatarNames();
    List<String> firstRow = new ArrayList<>(names.subList(0, Math.min(8, names.size())));
    List<String> secondRow = new ArrayList<>();
    if (names.size() > 8) {
      secondRow.addAll(names.subList(8, Math.min(16, names.size())));
    }
    return List.of(firstRow, secondRow);
  }

  public static boolean isChickAvatar(String avatar) {
    return avatar != null && CHICK_STEMS.contains(avatar);
  }

  public static String normalizeAvatarStem(String avatar) {
    if (isChickAvatar(avatar)) {
      return CHICK_BASE_STEM;
    }
    return avatar;
  }

  public static String getDisplayAvatarStem(String avatar, int chickPhaseUnlocked) {
    if (!isChickAvatar(avatar)) {
      return avatar;
    }
    return getChickPhaseStem(chickPhaseUnlocked);
  }

  /**
   * Creates an ImageView for the given avatar stem sized to
   * {@code size}×{@code size}.
   */
  public static ImageView createImageView(String stem, double size) {
    URL url = resolveAvatarUrl(stem);
    if (url == null) {
      url = resolveAvatarUrl(DEFAULT_AVATAR);
    }
    if (url == null) {
      return new ImageView();
    }
    ImageView iv = new ImageView(new Image(url.toExternalForm()));
    iv.setFitWidth(size);
    iv.setFitHeight(size);
    iv.setPreserveRatio(true);
    return iv;
  }

  private static URL resolveAvatarUrl(String stem) {
    String file = stem + ".png";

    String[] candidatePaths = {
        EMOJI_PATH + file,
        EMOJI_PATH + "humans/" + file,
        EMOJI_PATH + "animals-1/" + file,
        EMOJI_PATH + "animals-1/chicks/" + file,
        EMOJI_PATH + "animals-2/" + file
    };
    for (String candidatePath : candidatePaths) {
      URL candidate = AvatarUtil.class.getResource(candidatePath);
      if (candidate != null) {
        return candidate;
      }
    }
    return null;
  }
}
