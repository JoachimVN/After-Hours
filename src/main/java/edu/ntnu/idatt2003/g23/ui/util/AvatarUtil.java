package edu.ntnu.idatt2003.g23.ui.util;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.stream.Collectors;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public final class AvatarUtil {

    private static final String EMOJI_PATH = "/images/emojis/";
    private static final String DEFAULT_AVATAR = "bust-in-silhouette";
    private static final SequencedSet<String> HIDDEN_SELECTABLE_AVATARS = new LinkedHashSet<>(List.of(
            "fox",
            "smiling-face-with-horns"
    ));

    private AvatarUtil() {}

    /**
     * Returns avatar stems in the order defined by {@code order.txt} in the emojis folder.
     * Any PNG not listed in order.txt is appended alphabetically at the end.
     * To reorder: edit src/main/resources/images/emojis/order.txt, one stem per line.
     * Lines starting with # are treated as comments and ignored.
     */
    public static List<String> loadAvatarNames() {
        URL dirUrl = AvatarUtil.class.getResource(EMOJI_PATH);
        if (dirUrl == null) return List.of();
        try {
            Path dir = Path.of(dirUrl.toURI());

            // Collect all available PNG stems (alphabetical fallback order)
            List<String> allStems;
            try (var stream = Files.list(dir)) {
                allStems = stream
                        .filter(p -> p.toString().toLowerCase().endsWith(".png"))
                        .map(p -> p.getFileName().toString().replaceFirst("\\.png$", ""))
                        .sorted()
                        .collect(Collectors.toList());
            }

            // Apply avatar-order.txt if present
            URL orderUrl = AvatarUtil.class.getResource("/data/avatar-order.txt");
            Path orderFile = orderUrl != null ? Path.of(orderUrl.toURI()) : null;
            if (orderFile == null) orderFile = dir.resolve("avatar-order.txt"); // fallback
            if (orderFile != null && Files.exists(orderFile)) {
                List<String> ordered = Files.readAllLines(orderFile).stream()
                        .map(String::strip)
                        .filter(l -> !l.isBlank() && !l.startsWith("#"))
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
     * Returns selectable avatars (all avatars except the default silhouette).
     * Used for the profile avatar picker.
     */
    public static List<String> loadSelectableAvatarNames() {
        return loadAvatarNames().stream()
                .filter(s -> !DEFAULT_AVATAR.equals(s))
                .filter(s -> !HIDDEN_SELECTABLE_AVATARS.contains(s))
                .collect(Collectors.toList());
    }

    /** Creates an ImageView for the given avatar stem sized to {@code size}×{@code size}. */
    public static ImageView createImageView(String stem, double size) {
        URL url = AvatarUtil.class.getResource(EMOJI_PATH + stem + ".png");
        if (url == null) return new ImageView();
        ImageView iv = new ImageView(new Image(url.toExternalForm()));
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        return iv;
    }
}
