package edu.ntnu.idatt2003.g23.ui.navigation;

import edu.ntnu.idatt2003.g23.ui.BackgroundCanvas;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Coordinates top-level page swaps and simple transition effects.
 */
public final class NavigationCoordinator {

  private final StackPane root;
  private final BackgroundCanvas backgroundCanvas;

  public NavigationCoordinator(StackPane root, BackgroundCanvas backgroundCanvas) {
    this.root = root;
    this.backgroundCanvas = backgroundCanvas;
  }

  public void navigate(Parent page) {
    root.getChildren().setAll(backgroundCanvas, page);
  }

  public void fadeIn(Parent page) {
    // AI-ASSISTED: Drafted/refined with AI support and validated by the team.
    page.setOpacity(0);
    FadeTransition transition = new FadeTransition(Duration.millis(500), page);
    transition.setFromValue(0);
    transition.setToValue(1);
    transition.setInterpolator(Interpolator.EASE_BOTH);
    transition.play();
  }
}
