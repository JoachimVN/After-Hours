package edu.ntnu.idatt2003.g23.ui.overlay;

import edu.ntnu.idatt2003.g23.AppConfig;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class SplashOverlayController {

  private final StackPane root;
  private final FadeTransition fadeTransition;
  private final ParallelTransition logoTransition;

  public SplashOverlayController(StackPane root) {
    this.root = root;
    SplashPaneState splashPaneState = createSplashPane();
    StackPane splashPane = splashPaneState.splashPane;
    this.logoTransition = splashPaneState.logoTransition;
    this.fadeTransition = createFadeTransition(splashPane);
  }

  public void fadeAfterStartup() {
    if (logoTransition != null) {
      logoTransition.playFromStart();
    }
    fadeAfter(AppConfig.SPLASH_DELAY);
  }

  public void fadeAfterFailure() {
    fadeAfter(AppConfig.SPLASH_FALLBACK_DELAY);
  }

  private SplashPaneState createSplashPane() {
    Rectangle bg = new Rectangle();
    bg.getStyleClass().add("splash-overlay");
    bg.widthProperty().bind(root.widthProperty());
    bg.heightProperty().bind(root.heightProperty());

    StackPane splashPane = new StackPane(bg);

    ColorAdjust colorAdjust = new ColorAdjust();
    colorAdjust.setBrightness(0.125); // Range 0 to 1.0 (0 is black/original, 1.0 is white)

    ImageView fadedLogo = createLogo("/images/logos/After_Hours_Logo_Centered_Black.png");
    if (fadedLogo != null) {
      fadedLogo.setEffect(colorAdjust);
      splashPane.getChildren().add(fadedLogo);
    }

    ImageView goldLogo = createLogo("/images/logos/After_Hours_Logo_Centered.png");
    if (goldLogo != null) {
      goldLogo.setOpacity(fadedLogo == null ? 1.0 : 0.0);
      splashPane.getChildren().add(goldLogo);
    }

    root.getChildren().add(splashPane);

    ParallelTransition transition = null;
    if (fadedLogo != null && goldLogo != null) {
      transition = createGrayToGoldTransition(fadedLogo, goldLogo);
    }

    return new SplashPaneState(splashPane, transition);
  }

  private ImageView createLogo(String resourcePath) {
    var logoUrl = SplashOverlayController.class.getResource(resourcePath);
    if (logoUrl == null) {
      return null;
    }

    ImageView logo = new ImageView(new Image(logoUrl.toExternalForm()));
    logo.setPreserveRatio(true);
    logo.setFitWidth(512);
    return logo;
  }

  private ParallelTransition createGrayToGoldTransition(ImageView fadedLogo, ImageView goldLogo) {
    FadeTransition fadedOut = new FadeTransition(AppConfig.SPLASH_LOGO_FADE_DURATION, fadedLogo);
    fadedOut.setDelay(AppConfig.SPLASH_LOGO_FADE_DURATION);
    fadedOut.setFromValue(1.0);
    fadedOut.setToValue(0.0);
    fadedOut.setInterpolator(Interpolator.LINEAR);

    FadeTransition goldIn = new FadeTransition(AppConfig.SPLASH_LOGO_FADE_DURATION, goldLogo);
    goldIn.setDelay(AppConfig.SPLASH_LOGO_FADE_DURATION);
    goldIn.setFromValue(0.0);
    goldIn.setToValue(1.0);
    goldIn.setInterpolator(Interpolator.LINEAR);

    return new ParallelTransition(fadedOut, goldIn);
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

  private record SplashPaneState(StackPane splashPane, ParallelTransition logoTransition) {
  }
}
