package edu.ntnu.idatt2003.g23.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Full-screen canvas that renders an animated starfield with drifting nebula blobs.
 * Placed as the first child of the root StackPane so it appears behind all page content.
 */
public class BackgroundCanvas extends Canvas {

    private static final int STAR_COUNT = 160;

    // Stars: [xFrac, yFrac, radiusPx, twinklePhase, twinkleSpeed]
    private final double[][] stars = new double[STAR_COUNT][5];

    private final AnimationTimer timer;

    public BackgroundCanvas() {
        Random rng = new Random(7L);
        for (int i = 0; i < STAR_COUNT; i++) {
            stars[i][0] = rng.nextDouble();                      // x fraction of canvas width
            stars[i][1] = rng.nextDouble();                      // y fraction of canvas height
            stars[i][2] = 0.5 + rng.nextDouble() * 1.8;         // radius in px (0.5–2.3)
            stars[i][3] = rng.nextDouble() * Math.PI * 2.0;     // twinkle phase offset
            stars[i][4] = 0.3 + rng.nextDouble() * 1.1;         // twinkle speed (rad/s)
        }

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                draw(now / 1_000_000_000.0);
            }
        };
        timer.start();
    }

    private void draw(double t) {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // Stars
        for (double[] s : stars) {
            double x = s[0] * w;
            double y = s[1] * h;
            double r = s[2];
            double brightness = 0.2 + 0.8 * (0.5 + 0.5 * Math.sin(t * s[4] + s[3]));
            double d = r * 2;

            // Soft glow halo — warm teal tint
            gc.setFill(Color.color(0.55, 0.82, 0.95, brightness * 0.14));
            gc.fillOval(x - r * 2.2, y - r * 2.2, d * 2.2, d * 2.2);
            // Core dot — near-white with warm tint
            gc.setFill(Color.color(0.96, 0.95, 0.88, brightness * 0.5));
            gc.fillOval(x - r, y - r, d, d);
        }
    }

    public void stop() {
        timer.stop();
    }
}
