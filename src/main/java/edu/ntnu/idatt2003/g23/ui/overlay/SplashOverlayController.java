package edu.ntnu.idatt2003.g23.ui.overlay;

import edu.ntnu.idatt2003.g23.AppConfig;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
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
    colorAdjust.setBrightness(0.125);

    ImageView fadedLogo = createLogoImageView("/images/logos/After_Hours_Logo_Centered_Black.png");
    if (fadedLogo != null) {
      fadedLogo.setEffect(colorAdjust);
      splashPane.getChildren().add(fadedLogo);
    }

    ImageView regularLogo = createLogoImageView("/images/logos/After_Hours_Logo_Centered.png");
    if (regularLogo != null) {
      regularLogo.setOpacity(fadedLogo == null ? 1.0 : 0.0);
      splashPane.getChildren().add(regularLogo);
    }

    if (fadedLogo != null && regularLogo != null) {
      createLogoFadeTransition(fadedLogo, regularLogo).play();
    }

    root.getChildren().add(splashPane);
    return splashPane;
  }

  private ImageView createLogoImageView(String resourcePath) {
    var logoUrl = SplashOverlayController.class.getResource(resourcePath);
    if (logoUrl == null) {
      return null;
    }

    ImageView logo = new ImageView(new Image(logoUrl.toExternalForm()));
    logo.setPreserveRatio(true);
    logo.setFitWidth(512);
    return logo;
  }

  private ParallelTransition createLogoFadeTransition(ImageView fadedLogo, ImageView regularLogo) {
    FadeTransition fadedLogoTransition =
        new FadeTransition(AppConfig.SPLASH_LOGO_FADE_DURATION, fadedLogo);
    fadedLogoTransition.setFromValue(1.0);
    fadedLogoTransition.setToValue(0.0);

    FadeTransition regularLogoTransition =
        new FadeTransition(AppConfig.SPLASH_LOGO_FADE_DURATION, regularLogo);
    regularLogoTransition.setFromValue(0.0);
    regularLogoTransition.setToValue(1.0);

    return new ParallelTransition(fadedLogoTransition, regularLogoTransition);
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
