package edu.ntnu.idatt2003.g23.ui.overlay;

import edu.ntnu.idatt2003.g23.AppConfig;
import javafx.animation.FadeTransition;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class SplashOverlayController {

  private final StackPane root;
  private final FadeTransition fadeTransition;

  public SplashOverlayController(StackPane root) {
    this.root = root;
    StackPane splashPane = createSplashPane();
    this.fadeTransition = createFadeTransition(splashPane);
  }

  public void fadeAfterStartup() {
    fadeAfter(AppConfig.SPLASH_DELAY);
  }

  public void fadeAfterFailure() {
    fadeAfter(AppConfig.SPLASH_FALLBACK_DELAY);
  }

  private StackPane createSplashPane() {
    Rectangle bg = new Rectangle();
    bg.getStyleClass().add("splash-overlay");
    bg.widthProperty().bind(root.widthProperty());
    bg.heightProperty().bind(root.heightProperty());

    StackPane splashPane = new StackPane(bg);

    ColorAdjust colorAdjust = new ColorAdjust();
    colorAdjust.setBrightness(0.125); // Range 0 to 1.0 (0 is black/original, 1.0 is white)

    var logoUrl = SplashOverlayController.class.getResource(
        "/images/logos/After_Hours_Logo_Centered_Black.png");
    if (logoUrl != null) {
      ImageView logo = new ImageView(new Image(logoUrl.toExternalForm()));
      logo.setPreserveRatio(true);
      logo.setFitWidth(512);
      logo.setEffect(colorAdjust);
      splashPane.getChildren().add(logo);
    }

    root.getChildren().add(splashPane);
    return splashPane;
  }

  private FadeTransition createFadeTransition(StackPane splashPane) {
    FadeTransition fade = new FadeTransition(AppConfig.SPLASH_FADE_DURATION, splashPane);
    fade.setFromValue(1.0);
    fade.setToValue(0.0);
    fade.setOnFinished(event -> root.getChildren().remove(splashPane));
    return fade;
  }

  private void fadeAfter(javafx.util.Duration delay) {
    fadeTransition.stop();
    fadeTransition.setDelay(delay);
    fadeTransition.play();
  }
}
