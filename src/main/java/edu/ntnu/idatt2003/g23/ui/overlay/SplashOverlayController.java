package edu.ntnu.idatt2003.g23.ui.overlay;

import edu.ntnu.idatt2003.g23.AppConfig;
import javafx.animation.FadeTransition;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class SplashOverlayController {

    private final StackPane root;
    private final FadeTransition fadeTransition;

    public SplashOverlayController(StackPane root) {
        this.root = root;
        Rectangle splashOverlay = createSplashOverlay();
        this.fadeTransition = createFadeTransition(splashOverlay);
    }

    public void fadeAfterStartup() {
        fadeAfter(AppConfig.SPLASH_DELAY);
    }

    public void fadeAfterFailure() {
        fadeAfter(AppConfig.SPLASH_FALLBACK_DELAY);
    }

    private Rectangle createSplashOverlay() {
        Rectangle splashOverlay = new Rectangle();
        splashOverlay.getStyleClass().add("splash-overlay");
        splashOverlay.widthProperty().bind(root.widthProperty());
        splashOverlay.heightProperty().bind(root.heightProperty());
        root.getChildren().add(splashOverlay);
        return splashOverlay;
    }

    private FadeTransition createFadeTransition(Rectangle splashOverlay) {
        FadeTransition fade = new FadeTransition(AppConfig.SPLASH_FADE_DURATION, splashOverlay);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(event -> root.getChildren().remove(splashOverlay));
        return fade;
    }

    private void fadeAfter(javafx.util.Duration delay) {
        fadeTransition.stop();
        fadeTransition.setDelay(delay);
        fadeTransition.play();
    }
}
