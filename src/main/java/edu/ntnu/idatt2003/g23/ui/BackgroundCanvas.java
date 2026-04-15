package edu.ntnu.idatt2003.g23.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import javafx.scene.transform.Affine;

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
        setEffect(new GaussianBlur(3.5));
    }  

    private void draw(double t) {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // Shared rotation angle drives both gradient and stars — same "looking around" tilt (rotation speed)
        double angleDeg = 3.5 * Math.sin(t * 0.20) + 0.8 * Math.sin(t * 0.53 + 1.1);
        double angleRad = Math.toRadians(angleDeg);

        // Gradient center: rotate the fixed zenith point (0.5, 0.12) around sky center (0.5, 0.5)
        // by the same angle so gradient and stars move as one rigid unit
        double zx = 0.0;          // offset from center: zenith is directly above
        double zy = -0.38;
        double centerX = 0.5 + zx * Math.cos(angleRad) - zy * Math.sin(angleRad);
        double centerY = 0.5 + zx * Math.sin(angleRad) + zy * Math.cos(angleRad);

        RadialGradient bg = new RadialGradient(
                0, 0,
                centerX, centerY,
                0.75,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1c3653")),
                new Stop(1, Color.web("#07091f"))
        );
        gc.setFill(bg);
        gc.fillRect(0, 0, w, h);

        // Stars — rotated by the exact same angle
        double cx = w / 2.0;
        double cy = h / 2.0;

        gc.save();
        Affine rotate = new Affine();
        rotate.appendRotation(angleDeg, cx, cy);
        gc.setTransform(rotate);

        for (double[] s : stars) {
            double x = s[0] * w;
            double y = s[1] * h;
            double r = s[2];
            double brightness = 0.2 + 0.8 * (0.5 + 0.5 * Math.sin(t * s[4] + s[3]));
            double d = r * 2;

            // Soft glow halo — deep blue tint
            gc.setFill(Color.color(0.45, 0.60, 0.90, brightness * 0.14));
            gc.fillOval(x - r * 2.2, y - r * 2.2, d * 2.2, d * 2.2);
            // Core dot — near-white with warm tint
            gc.setFill(Color.color(0.96, 0.95, 0.88, brightness * 0.5));
            gc.fillOval(x - r, y - r, d, d);
        }

        gc.restore();
    }

    public void stop() {
        timer.stop();
    }
}
