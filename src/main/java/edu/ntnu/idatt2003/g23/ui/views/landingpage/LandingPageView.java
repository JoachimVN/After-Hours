package edu.ntnu.idatt2003.g23.ui.views.landingpage;

import java.util.Locale;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public final class LandingPageView {

    public static BorderPane build(Runnable onPlay, Runnable onSettings, Runnable onQuit) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("home-page");

        // Title
        Label title = new Label("After Hours");
        title.getStyleClass().add("title");

        // Taglines
        Label tagline = new Label("\u2726  Lorem Ipsum \u2022 Lorem Ipsum \u2022 Lorem Ipsum  \u2726");
        tagline.getStyleClass().add("tagline");
        Label subTagline = new Label("Lorem ipsum dolor sit amet, consectetur adipiscing elit.");
        subTagline.getStyleClass().add("sub-tagline");
        VBox taglineBlock = new VBox(6, tagline, subTagline);
        taglineBlock.setAlignment(Pos.CENTER);

        // Buttons
        Button startButton = new Button("\u25B6   Start Trading");
        startButton.getStyleClass().add("start-button");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setOnAction(e -> onPlay.run());

        // ── Subtle shimmer + soft glow ─────────────────────────────────────
        DropShadow glow = new DropShadow();
        glow.setBlurType(BlurType.GAUSSIAN);
        glow.setSpread(0.0);
        glow.setColor(Color.rgb(245, 162, 1, 0.35));
        glow.setRadius(18);
        startButton.setEffect(glow);

        new AnimationTimer() {
            // Dark edges → warm gold center (baked gold-bar curve), light highlight
            private final double[] dark = {0.62, 0.40, 0.00};
            private final double[] mid  = {0.95, 0.64, 0.05};
            private final double[] hl   = {1.00, 0.94, 0.68};
            private final int[] stops = {0, 6, 13, 19, 25, 31, 38, 44, 50, 56, 63, 69, 75, 81, 88, 94, 100};

            private double gauss(double x, double peak, double sigma) {
                double d = x - peak; return Math.exp(-(d * d) / (2 * sigma * sigma));
            }
            private String hex(double r, double g, double b) {
                return String.format("#%02x%02x%02x",
                    Math.min(255, (int)(r * 255)),
                    Math.min(255, (int)(g * 255)),
                    Math.min(255, (int)(b * 255)));
            }

            @Override public void handle(long now) {
                double t  = now / 1_000_000_000.0;
                double pulse = (Math.sin(t * 0.9) + 1) / 2.0;
                // sin() path: eases in, slows, reverses — no hard wrap, fully organic
                double sp   = 50 + 80 * Math.sin(t * 0.52);          // −30→130, ~12 s cycle
                double tiltY = 5 * Math.sin(t * 0.29);               // ±5% diagonal drift

                StringBuilder sb = new StringBuilder(String.format(Locale.US,
                    "linear-gradient(from 0%% %.1f%% to 100%% %.1f%%",
                    50 - tiltY, 50 + tiltY));
                for (int p : stops) {
                    double curve = gauss(p, 50, 38); // gold-bar base: darker at edges
                    double br = dark[0] + (mid[0] - dark[0]) * curve;
                    double bg = dark[1] + (mid[1] - dark[1]) * curve;
                    double bb = dark[2] + (mid[2] - dark[2]) * curve;
                    double inf = gauss(p, sp, 16) * 0.56; // 25% brighter than before
                    sb.append(", ").append(hex(br + (hl[0] - br) * inf,
                                              bg + (hl[1] - bg) * inf,
                                              bb + (hl[2] - bb) * inf))
                      .append(" ").append(p).append("%");
                }
                sb.append(")");

                glow.setRadius(12 + pulse * 10);
                glow.setColor(Color.rgb(245, 162, 1, 0.18 + pulse * 0.22));
                startButton.setStyle("-fx-background-color: " + sb + ";");
            }
        }.start();

        Button settingsButton = new Button("\u2699   Settings");
        settingsButton.getStyleClass().add("secondary-button");
        settingsButton.setOnAction(e -> onSettings.run());

        Button quitButton = new Button("\u2715   Quit");
        quitButton.getStyleClass().addAll("secondary-button", "quit-button");
        quitButton.setOnAction(e -> onQuit.run());

        settingsButton.prefWidthProperty().bind(startButton.widthProperty().multiply(0.25));
        quitButton.prefWidthProperty().bind(startButton.widthProperty().multiply(0.25));

        HBox secondaryRow = new HBox(12, settingsButton, quitButton);
        secondaryRow.getStyleClass().add("landing-secondary-row");
        secondaryRow.setAlignment(Pos.CENTER);

        VBox buttonBlock = new VBox(12, startButton, secondaryRow);
        buttonBlock.setAlignment(Pos.CENTER);

        VBox center = new VBox(0, title, taglineBlock, buttonBlock);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(680);
        center.setPadding(new Insets(40, 0, 40, 0));
        VBox.setMargin(taglineBlock, new Insets(10, 0, 0, 0));
        VBox.setMargin(buttonBlock, new Insets(32, 0, 0, 0));

        root.setCenter(center);
        return root;
    }
}
