package edu.ntnu.idatt2003.g23.ui.util;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
    URL dirUrl = AvatarUtil.class.getResource(EMOJI_PATH);
    if (dirUrl == null) {
      return List.of();
    }
    try {
      Path dir = Path.of(dirUrl.toURI());

      // Collect all available PNG stems recursively (alphabetical fallback order).
      // This supports grouped emoji folders like humans/, animals-1/, animals-2/,
      // etc.
      List<String> allStems;
      try (var stream = Files.walk(dir)) {
        allStems = stream
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().toLowerCase().endsWith(".png"))
            // Ignore animals-2 folder
            .filter(p -> !p.toString().replace('\\', '/').contains("/animals-2/"))
            .map(p -> p.getFileName().toString().replaceFirst("\\.png$", ""))
            .distinct()
            .sorted()
            .collect(Collectors.toList());
      }

      // Apply avatar-order.txt if present
      URL orderUrl = AvatarUtil.class.getResource("/data/avatar-order.txt");
      Path orderFile = orderUrl != null ? Path.of(orderUrl.toURI()) : null;
      if (orderFile == null) {
        orderFile = dir.resolve("avatar-order.txt"); // fallback
      }
      if (orderFile != null && Files.exists(orderFile)) {
        List<String> ordered = Files.readAllLines(orderFile).stream()
            .map(String::strip)
            .map(AvatarUtil::normalizeAvatarStem)
            .filter(l -> !l.isBlank() && !l.startsWith("#"))
            .filter(allStems::contains)
            .collect(Collectors.toList());
        // LinkedHashSet preserves insertion order and deduplicates
        SequencedSet<String> result = new LinkedHashSet<>(ordered);
        // Append any PNG not mentioned in order.txt
        allStems.stream().filter(s -> !result.contains(s)).forEach(result::add);
        return new ArrayList<>(result);
      }

      return allStems;
    } catch (Exception e) {
      return List.of();
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
    return loadAvatarNames().stream()
        .map(AvatarUtil::normalizeAvatarStem)
        .filter(s -> !DEFAULT_AVATAR.equals(s))
        .filter(s -> !HIDDEN_SELECTABLE_AVATARS.contains(s))
        .distinct()
        .collect(Collectors.toList());
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

    // Fast path for flat structure.
    URL direct = AvatarUtil.class.getResource(EMOJI_PATH + file);
    if (direct != null) {
      return direct;
    }

    // Fallback for nested folder structures under /images/emojis/.
    URL dirUrl = AvatarUtil.class.getResource(EMOJI_PATH);
    if (dirUrl == null) {
      return null;
    }
    try {
      Path dir = Path.of(dirUrl.toURI());
      try (var stream = Files.walk(dir)) {
        Path match = stream
            .filter(Files::isRegularFile)
            .filter(p -> !p.toString().replace('\\', '/').contains("/animals-2/"))
            .filter(p -> p.getFileName().toString().equalsIgnoreCase(file))
            .findFirst()
            .orElse(null);
        return match == null ? null : match.toUri().toURL();
      }
    } catch (Exception e) {
      return null;
    }
  }
}
