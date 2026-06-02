package edu.ntnu.idatt2003.g23.ui.views.saveselect;

import java.nio.file.Path;
import java.util.List;

import edu.ntnu.idatt2003.g23.io.GameSaveLoader.SaveMeta;
import edu.ntnu.idatt2003.g23.ui.util.AvatarUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;

/**
 * View for the save-selection screen.
 *
 * <p>Shows a scrollable list of save cards. Each card displays:
 * player name, exchange, week, cash, date saved, and portfolio size.
 * Each card has Load, Rename, and Delete actions.
 */
public final class SaveSelectView {

  private static final String CANCEL_SOUND = "/audio/sfx/Cancel.wav";
  private static final String SELECT_SOUND = "/audio/sfx/Select.wav";
  private static final AudioClip CANCEL_CLIP = loadAudioClip(CANCEL_SOUND);
  private static final AudioClip SELECT_CLIP = loadAudioClip(SELECT_SOUND);

  private final SaveSelectController controller;
  private final StackPane root;     // top-level — holds page + overlays
  private final StackPane overlay;  // popup layer

  public SaveSelectView(SaveSelectController controller) {
    this.controller = controller;
    this.overlay = new StackPane();
    this.overlay.setPickOnBounds(false);
    BorderPane page = buildUI();
    this.root = new StackPane(page, overlay);
  }

  public StackPane getRoot() {
    return root;
  }

  // ── Build ─────────────────────────────────────────────────────────────────

  private BorderPane buildUI() {
    BorderPane pane = new BorderPane();
    pane.getStyleClass().addAll("home-page", "background-overlay");

    // ── Top bar ───────────────────────────────────────────────────────────
    Button backBtn = new Button("\u2190 Back");
    backBtn.getStyleClass().add("back-button");
    backBtn.setOnAction(e -> controller.handleBack());

    Button newGameBtn = new Button("+ New Game");
    newGameBtn.getStyleClass().addAll("start-button", "new-game-button");
    newGameBtn.setOnAction(e -> controller.handleNewGame());

    HBox topBar = new HBox(backBtn);
    topBar.setAlignment(Pos.CENTER_LEFT);
    topBar.setPadding(new Insets(24, 32, 0, 32));

    // ── Title ─────────────────────────────────────────────────────────────
    List<SaveMeta> saves = controller.loadSaveList();
    boolean hasSavesOrSession = !saves.isEmpty() || controller.hasSession();
    Label title = new Label(hasSavesOrSession ? "Continue Game" : "New Game");
    title.getStyleClass().add("page-title");

    // ── Save list ─────────────────────────────────────────────────────────

    VBox cardList = new VBox(16);
    cardList.setAlignment(Pos.TOP_CENTER);
    cardList.setPadding(new Insets(8, 0, 24, 0));

    if (controller.hasSession()) {
      cardList.getChildren().add(buildResumeCard());
    }

    if (saves.isEmpty() && !controller.hasSession()) {
      Label empty = new Label("No save files found.");
      empty.getStyleClass().add("settings-label");
      cardList.getChildren().add(empty);
    } else {
      for (SaveMeta meta : saves) {
        cardList.getChildren().add(buildCard(meta, cardList));
      }
    }

    cardList.getChildren().add(buildNewGameCard());

    ScrollPane scroll = new ScrollPane(cardList);
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.getStyleClass().add("save-scroll");
    scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

    // ── Center wrapper (vertical centering) ───────────────────────────────
    VBox centerContent = new VBox(20, title, scroll);
    centerContent.setAlignment(Pos.TOP_CENTER);
    centerContent.setPadding(new Insets(32, 64, 32, 64));
    VBox.setVgrow(scroll, Priority.ALWAYS);

    VBox centerWrapper = new VBox(centerContent);
    centerWrapper.setAlignment(Pos.CENTER); // <-- vertical centering
    centerWrapper.setFillWidth(true);

    pane.setTop(topBar);
    pane.setCenter(centerWrapper);

    // Escape → back
    pane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      if (e.getCode() == KeyCode.ESCAPE && overlay.getChildren().isEmpty()) {
        controller.handleBack();
        e.consume();
      }
    });

    return pane;
  }


  // ── Card builders ─────────────────────────────────────────────────────────

  private VBox buildCard(SaveMeta meta, VBox cardList) {
    // ── Info lines ─────────────────────────────────────────────────────
    TextField nameField = new TextField(meta.displayName());
    nameField.getStyleClass().add("save-card-name-field");
    nameField.setStyle(
        "-fx-font-family: 'Harlow Solid Italic'; -fx-font-size: 34; -fx-text-fill: #f0f7ff;");


    Runnable saveName = () -> {
      String newName = nameField.getText().strip();
      if (!newName.isEmpty() && !newName.equals(meta.displayName())) {
        String safe = meta.saveDir().getFileName().toString()
            .replaceFirst("^[^_]+", newName.replaceAll("[^A-Za-z0-9_\\-]", "_"));
        Path newPath = controller.renameSave(meta.saveDir(), safe, newName);
        if (newPath != null) {
          // Rebuild card list to reflect the rename
          rebuildCardList(cardList);
        } else {
          nameField.setText(meta.displayName());
        }
      } else {
        nameField.setText(meta.displayName());
      }
    };

    nameField.focusedProperty().addListener((obs, oldV, focused) -> {
      if (!focused) {
        saveName.run();
      }
    });
    nameField.setOnKeyPressed(e -> {
      if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
        saveName.run();
        nameField.getParent().requestFocus();
      }
    });

    Label avatarLabel = new Label();
    avatarLabel.setGraphic(AvatarUtil.createImageView(meta.profileAvatar(), 46.8));
    avatarLabel.getStyleClass().add("save-card-avatar");

    Label detailLabel = new Label(
        meta.exchangeName() + "  \u2022  Week " + meta.week() +
            "  \u2022  Cash: $" + formatMoney(meta.money()) +
            "  \u2022  " + meta.portfolioSize() + " stock" +
            (meta.portfolioSize() == 1 ? "" : "s") +
            (meta.totalShares() > 0 ? "  \u2022  " + meta.totalShares() + " shares" : ""));
    detailLabel.getStyleClass().add("save-card-detail");

    Label netWorthLabel = new Label("Net Worth: $" + formatMoney(meta.netWorthHint()));
    netWorthLabel.getStyleClass().add("save-card-networth");

    Label dateLabel = new Label((meta.autosave() ? "Autosaved:  " : "Saved: ") + meta.savedAt());
    dateLabel.getStyleClass().add("save-card-date");

    Label flaggedLabel = null;
    if (meta.flagged()) {
      flaggedLabel = new Label("Modified");
      flaggedLabel.getStyleClass().add("save-card-flag");
    }

    VBox info = flaggedLabel == null
        ? new VBox(3, nameField, detailLabel, netWorthLabel, dateLabel)
        : new VBox(3, nameField, detailLabel, netWorthLabel, dateLabel, flaggedLabel);
    info.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(info, Priority.ALWAYS);

    // ── Action buttons ─────────────────────────────────────────────────
    Button loadBtn = new Button("\u25b6  Load");
    loadBtn.getStyleClass().addAll("setup-start-button", "save-load-button");
    loadBtn.setMaxWidth(Double.MAX_VALUE);
    loadBtn.setOnAction(e -> {
      String err = controller.loadSave(meta.saveDir());
      if (err != null) {
        showError("Load Failed", err);
      }
    });

    Button editBtn = new Button("\u270e  Edit");
    editBtn.getStyleClass().add("save-rename-button");
    editBtn.setMaxWidth(Double.MAX_VALUE);
    editBtn.setTooltip(buildButtonTooltip("Edit market data for this save"));
    editBtn.setOnAction(e -> controller.handleEditSave(meta));

    if (!controller.isDevModeEnabled()) {
      editBtn.setVisible(false);
      editBtn.setManaged(false);
    }

    Button deleteBtn = new Button("\u2715  Delete");
    deleteBtn.getStyleClass().addAll("save-delete-button");
    deleteBtn.setMaxWidth(Double.MAX_VALUE);
    deleteBtn.setOnAction(e -> handleDelete(meta, cardList));

    VBox buttons = new VBox(6, loadBtn, editBtn, deleteBtn);
    buttons.setAlignment(Pos.CENTER);
    buttons.setFillWidth(true);

    HBox card = new HBox(12, avatarLabel, info, buttons);
    card.setAlignment(Pos.CENTER_LEFT);
    card.getStyleClass().add("save-card");
    if (meta.autosave()) {
      card.getStyleClass().add("save-card-autosave");
    }
    card.setPadding(new Insets(12, 16, 12, 16));
    card.setMaxWidth(700);

    // Make avatar full height of card
    VBox.setVgrow(avatarLabel, Priority.ALWAYS);
    avatarLabel.setMaxHeight(Double.MAX_VALUE);
    avatarLabel.setStyle("-fx-alignment: center;");

    VBox wrapper = new VBox(card);
    wrapper.setAlignment(Pos.CENTER);
    return wrapper;
  }

  private VBox buildNewGameCard() {
    Button newBtn = new Button("+ Start a New Game");
    newBtn.getStyleClass().addAll("start-button", "new-game-button");
    newBtn.setMaxWidth(700);
    newBtn.setOnAction(e -> controller.handleNewGame());

    VBox wrapper = new VBox(newBtn);
    wrapper.setAlignment(Pos.CENTER);
    wrapper.setPadding(new Insets(8, 0, 0, 0));
    return wrapper;
  }

  private VBox buildResumeCard() {
    Label nameLabel = new Label("\u25B6  Resume Current Game");
    nameLabel.getStyleClass().add("save-card-title");

    Label detail = new Label("Your last session \u2014 not saved to disk");
    detail.getStyleClass().add("save-card-detail");

    VBox info = new VBox(4, nameLabel, detail);
    info.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(info, Priority.ALWAYS);

    Button resumeBtn = new Button("\u25b6  Resume");
    resumeBtn.getStyleClass().addAll("setup-start-button", "save-load-button");
    resumeBtn.setOnAction(e -> controller.resumeSession());

    HBox card = new HBox(20, info, resumeBtn);
    card.setAlignment(Pos.CENTER_LEFT);
    card.getStyleClass().addAll("save-card", "resume-card");
    card.setPadding(new Insets(20, 24, 20, 24));
    card.setMaxWidth(700);

    VBox wrapper = new VBox(card);
    wrapper.setAlignment(Pos.CENTER);
    return wrapper;
  }

  // ── Handlers ──────────────────────────────────────────────────────────────

  private void handleDelete(SaveMeta meta, VBox cardList) {
    // ── Header ────────────────────────────────────────────────────────────
    Label iconLbl = new Label("\u2715");
    iconLbl.getStyleClass().add("error-dialog-icon");
    Label titleLbl = new Label("Delete Save");
    titleLbl.getStyleClass().add("error-dialog-title");
    HBox header = new HBox(10, iconLbl, titleLbl);
    header.getStyleClass().add("error-dialog-header");
    header.setAlignment(Pos.CENTER_LEFT);

    // ── Body ──────────────────────────────────────────────────────────────
    Label msg = new Label("Delete \"" + meta.displayName() + "\"?\nThis cannot be undone.");
    msg.getStyleClass().add("error-dialog-message");
    msg.setWrapText(true);
    msg.setMaxWidth(300);
    VBox body = new VBox(msg);
    body.getStyleClass().add("error-dialog-body");

    // ── Buttons ───────────────────────────────────────────────────────────
    Button cancelBtn = new Button("Cancel");
    cancelBtn.getStyleClass().add("dialog-cancel-btn");
    Button deleteBtn = new Button("\u2715 Delete");
    deleteBtn.getStyleClass().add("dialog-confirm-error-btn");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox btnRow = new HBox(10, cancelBtn, spacer, deleteBtn);
    btnRow.getStyleClass().add("dialog-btn-row");

    VBox card = new VBox(0, header, body, btnRow);
    card.getStyleClass().add("error-dialog-root");
    card.setMaxWidth(360);
    card.setMaxHeight(Region.USE_PREF_SIZE);

    Region backdrop = new Region();
    backdrop.getStyleClass().add("dialog-backdrop");

    StackPane popup = new StackPane(backdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);

    Runnable dismiss = () -> overlay.getChildren().remove(popup);
    cancelBtn.setOnAction(ev -> {
      playCancelSound();
      dismiss.run();
    });
    backdrop.setOnMouseClicked(ev -> dismiss.run());
    deleteBtn.setOnAction(ev -> {
      playSelectSound();
      dismiss.run();
      String err = controller.deleteSave(meta.saveDir());
      if (err != null) {
        showError("Delete Failed", err);
      } else {
        rebuildCardList(cardList);
      }
    });
    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      if (ev.getCode() == KeyCode.ESCAPE) {
        dismiss.run();
        ev.consume();
      }
    });

    overlay.getChildren().add(popup);
    popup.requestFocus();
  }

  private void rebuildCardList(VBox cardList) {
    cardList.getChildren().clear();
    List<SaveMeta> saves = controller.loadSaveList();
    if (saves.isEmpty()) {
      Label empty = new Label("No save files found.");
      empty.getStyleClass().add("settings-label");
      cardList.getChildren().add(empty);
    } else {
      for (SaveMeta meta : saves) {
        cardList.getChildren().add(buildCard(meta, cardList));
      }
    }
    cardList.getChildren().add(buildNewGameCard());
  }

  private static AudioClip loadAudioClip(String resourcePath) {
    var resource = SaveSelectView.class.getResource(resourcePath);
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

  private void showError(String title, String message) {
    Label iconLbl = new Label("⚠");
    iconLbl.getStyleClass().add("error-dialog-icon");
    Label titleLbl = new Label(title.toUpperCase());
    titleLbl.getStyleClass().add("error-dialog-title");
    HBox header = new HBox(10, iconLbl, titleLbl);
    header.getStyleClass().add("error-dialog-header");
    header.setAlignment(Pos.CENTER_LEFT);

    Label msgLbl = new Label(message);
    msgLbl.getStyleClass().add("error-dialog-message");
    msgLbl.setWrapText(true);
    msgLbl.setMaxWidth(300);
    VBox body = new VBox(msgLbl);
    body.getStyleClass().add("error-dialog-body");

    Button okBtn = new Button("OK");
    okBtn.getStyleClass().add("dialog-confirm-sell-btn");
    HBox btnRow = new HBox(okBtn);
    btnRow.setAlignment(Pos.CENTER_RIGHT);
    btnRow.getStyleClass().add("dialog-btn-row");

    VBox card = new VBox(0, header, body, btnRow);
    card.getStyleClass().add("error-dialog-root");
    card.setMaxWidth(360);
    card.setMaxHeight(Region.USE_PREF_SIZE);

    Region backdrop = new Region();
    backdrop.getStyleClass().add("error-dialog-backdrop");

    StackPane popup = new StackPane(backdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);

    Runnable dismiss = () -> overlay.getChildren().remove(popup);
    okBtn.setOnAction(ev -> dismiss.run());
    backdrop.setOnMouseClicked(ev -> dismiss.run());
    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      if (ev.getCode() == KeyCode.ESCAPE) {
        dismiss.run();
        ev.consume();
      }
    });

    overlay.getChildren().add(popup);
    popup.requestFocus();
  }

  private static String formatMoney(String raw) {
    try {
      double val = Double.parseDouble(raw);
      if (val >= 1_000_000) {
        return String.format("%.2fM", val / 1_000_000);
      }
      if (val >= 1_000) {
        return String.format("%.2fK", val / 1_000);
      }
      return String.format("%.2f", val);
    } catch (NumberFormatException _) {
      return raw;
    }
  }

  private static Tooltip buildButtonTooltip(String message) {
    Tooltip tooltip = new Tooltip(message);
    tooltip.setShowDelay(javafx.util.Duration.millis(120));
    tooltip.setShowDuration(javafx.util.Duration.INDEFINITE);
    tooltip.getStyleClass().add("save-card-tooltip");
    return tooltip;
  }
}
