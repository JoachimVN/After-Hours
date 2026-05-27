package edu.ntnu.idatt2003.g23.ui.overlay;

import edu.ntnu.idatt2003.g23.io.CsvEditorLoadAnalyzer;
import edu.ntnu.idatt2003.g23.io.CsvEditorLoadAnalyzer.LoadStats;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.util.Duration;

/**
 * Manages in-app overlay dialogs and toasts on behalf of the top-level {@code App} controller.
 * <p>
 * Encapsulates all JavaFX widget construction for notifications, confirmations, and warnings so
 * that the application controller stays focused on navigation and state management.
 */
public final class AppOverlayService {

  private static final String CANCEL_SOUND = "/audio/sfx/Cancel.wav";
  private static final String SELECT_SOUND = "/audio/sfx/Select.wav";
  private static final AudioClip CANCEL_CLIP = loadAudioClip(CANCEL_SOUND);
  private static final AudioClip SELECT_CLIP = loadAudioClip(SELECT_SOUND);

  private final StackPane root;

  public AppOverlayService(StackPane root) {
    this.root = root;
  }

  /**
   * Shows a modal in-app notification that the user must dismiss.
   */
  public void showNotification(String title, String message, boolean success) {
    Label iconLbl = new Label(success ? "\u2713" : "\u2715");
    iconLbl.getStyleClass().add(success ? "receipt-check" : "error-dialog-icon");
    Label titleLbl = new Label(title);
    titleLbl.getStyleClass().add(success ? "app-success-title" : "error-dialog-title");

    HBox header = new HBox(12, iconLbl, titleLbl);
    header.getStyleClass().add(success ? "app-success-header" : "error-dialog-header");
    header.setAlignment(Pos.CENTER_LEFT);

    Label msgLbl = new Label(message);
    msgLbl.getStyleClass().add(success ? "app-success-message" : "error-dialog-message");
    msgLbl.setWrapText(true);
    msgLbl.setMaxWidth(320);

    VBox body = new VBox(msgLbl);
    body.getStyleClass().add(success ? "app-success-body" : "error-dialog-body");

    Button okBtn = new Button("OK");
    okBtn.getStyleClass().add(success ? "dialog-confirm-buy-btn" : "dialog-confirm-error-btn");
    HBox btnRow = new HBox(okBtn);
    btnRow.setAlignment(Pos.CENTER_RIGHT);
    btnRow.getStyleClass().add("dialog-btn-row");

    VBox card = new VBox(0, header, body, btnRow);
    card.getStyleClass().add(success ? "app-success-root" : "error-dialog-root");
    card.setMaxWidth(420);
    card.setMaxHeight(Region.USE_PREF_SIZE);

    showPopup(card, ev -> {
      if (ev.getCode() == KeyCode.ESCAPE || ev.getCode() == KeyCode.ENTER) {
        ev.consume();
        return true;
      }
      return false;
    }, okBtn);
  }

  /**
   * Shows a non-blocking toast that auto-dismisses after 2 seconds.
   */
  public void showTimedNotification(String message) {
    Label msgLbl = new Label(message);
    msgLbl.getStyleClass().add("toast-message");
    msgLbl.setWrapText(false);

    VBox card = new VBox(msgLbl);
    card.getStyleClass().add("toast-card");
    card.setMaxWidth(260);
    card.setMaxHeight(Region.USE_PREF_SIZE);

    StackPane.setAlignment(card, Pos.BOTTOM_RIGHT);
    StackPane.setMargin(card, new Insets(0, 24, 32, 0));

    root.getChildren().add(card);
    PauseTransition pause = new PauseTransition(Duration.seconds(2));
    pause.setOnFinished(e -> root.getChildren().remove(card));
    pause.play();
  }

  /**
   * Shows a modal warning when a CSV file is large enough to potentially degrade editor
   * performance. Calls {@code onProceed} only if the user confirms.
   */
  public void showLargeFileWarning(LoadStats stats, Runnable onProceed) {
    Label iconLbl = new Label("\u26A0");
    iconLbl.getStyleClass().add("error-dialog-icon");
    Label titleLbl = new Label("Large File Warning");
    titleLbl.getStyleClass().add("error-dialog-title");

    HBox header = new HBox(12, iconLbl, titleLbl);
    header.getStyleClass().add("error-dialog-header");
    header.setAlignment(Pos.CENTER_LEFT);

    StringBuilder message = new StringBuilder(
        "This CSV is large enough that the editor may become slow or unresponsive.\n\n");
    message.append("Stocks: ").append(String.format("%,d", stats.rowCount()))
        .append("\nPrice points: ").append(String.format("%,d", stats.pricePointCount()));
    if (stats.priceCharCount() > CsvEditorLoadAnalyzer.PRICE_CHARS_WARN_THRESHOLD) {
      message.append("\nPrice text size: ")
          .append(String.format("%,d", stats.priceCharCount()))
          .append(" characters");
    }
    message.append("""
  Do you want to proceed?
  Your computer may explode.
  Don't say we didn't warn you!""");

    Label msgLbl = new Label(message.toString());
    msgLbl.getStyleClass().add("error-dialog-message");
    msgLbl.setWrapText(true);
    msgLbl.setMaxWidth(340);

    VBox body = new VBox(msgLbl);
    body.getStyleClass().add("error-dialog-body");

    Button cancelBtn = new Button("Cancel");
    cancelBtn.getStyleClass().add("dialog-cancel-btn");
    Button proceedBtn = new Button("Risk Everything");
    proceedBtn.getStyleClass().add("dialog-confirm-error-btn");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox btnRow = new HBox(8, cancelBtn, spacer, proceedBtn);
    btnRow.setAlignment(Pos.CENTER_RIGHT);
    btnRow.getStyleClass().add("dialog-btn-row");

    VBox card = new VBox(0, header, body, btnRow);
    card.getStyleClass().add("error-dialog-root");
    card.setMaxWidth(440);
    card.setMaxHeight(Region.USE_PREF_SIZE);

    showPopup(card, ev -> {
      if (ev.getCode() == KeyCode.ESCAPE) {
        ev.consume();
        return true;
      }
      return false;
    }, cancelBtn, proceedBtn);

    cancelBtn.setOnAction(ev -> {
      playCancelSound();
      root.getChildren().stream()
          .filter(n -> n instanceof StackPane stackPane && stackPane.getChildren().contains(card))
          .findFirst()
          .ifPresent(root.getChildren()::remove);
    });

    // Wire proceed separately so the lambda can close over onProceed
    // (cancelBtn dismiss is wired by showPopup via the primary button)
    proceedBtn.setOnAction(ev -> {
      playSelectSound();
      root.getChildren().stream()
          .filter(n -> n instanceof StackPane stackPane && stackPane.getChildren().contains(card))
          .findFirst()
          .ifPresent(root.getChildren()::remove);
      onProceed.run();
    });
  }

  // ── Internal helpers ──────────────────────────────────────────────────────

  /**
   * Wraps {@code card} in a backdrop + StackPane popup, registers dismiss on backdrop click and
   * optional key handler, wires the first button as the dismiss/confirm action, then adds the
   * popup to the root and requests focus.
   */
  private void showPopup(VBox card, java.util.function.Function<KeyEvent, Boolean> keyDismiss,
      Button... dismissButtons) {
    Region backdrop = new Region();
    backdrop.getStyleClass().add("dialog-backdrop");

    StackPane popup = new StackPane(backdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);

    Runnable dismiss = () -> root.getChildren().remove(popup);
    backdrop.setOnMouseClicked(ev -> dismiss.run());

    for (Button btn : dismissButtons) {
      btn.setOnAction(ev -> dismiss.run());
    }

    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      if (Boolean.TRUE.equals(keyDismiss.apply(ev))) {
        dismiss.run();
      }
    });

    root.getChildren().add(popup);
    popup.requestFocus();
  }

  private static AudioClip loadAudioClip(String resourcePath) {
    var resource = AppOverlayService.class.getResource(resourcePath);
    if (resource == null) {
      return null;
    }
    return new AudioClip(resource.toExternalForm());
  }

  private static void playCancelSound() {
    if (CANCEL_CLIP != null) {
      CANCEL_CLIP.play();
    }
  }

  private static void playSelectSound() {
    if (SELECT_CLIP != null) {
      SELECT_CLIP.play();
    }
  }
}
