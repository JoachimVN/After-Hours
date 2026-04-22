package edu.ntnu.idatt2003.g23.ui.views.saveselect;

import edu.ntnu.idatt2003.g23.io.GameSaveLoader.SaveMeta;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;

import java.nio.file.Path;
import java.util.List;

/**
 * View for the save-selection screen.
 *
 * <p>Shows a scrollable list of save cards. Each card displays:
 * player name, exchange, week, cash, date saved, and portfolio size.
 * Each card has Load, Rename, and Delete actions.
 */
public final class SaveSelectView {

    private final SaveSelectController controller;
    private final StackPane root;     // top-level — holds page + overlays
    private final StackPane overlay;  // popup layer

    public SaveSelectView(SaveSelectController controller) {
        this.controller = controller;
        this.overlay    = new StackPane();
        this.overlay.setPickOnBounds(false);
        BorderPane page = buildUI();
        this.root = new StackPane(page, overlay);
    }

    public StackPane getRoot() { return root; }

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
    Label title = new Label("Continue Game");
    title.getStyleClass().add("page-title");

    // ── Save list ─────────────────────────────────────────────────────────
    List<SaveMeta> saves = controller.loadSaveList();

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
        nameField.setStyle("-fx-font-family: 'Harlow Solid Italic'; -fx-font-size: 34; -fx-text-fill: #f0f7ff;");
        
        
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
            if (!focused) saveName.run();
        });
        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                saveName.run();
                nameField.getParent().requestFocus();
            }
        });

        Label avatarLabel = new Label(meta.profileAvatar());
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

        VBox info = new VBox(3, nameField, detailLabel, netWorthLabel, dateLabel);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        // ── Action buttons ─────────────────────────────────────────────────
        Button loadBtn = new Button("Load");
        loadBtn.getStyleClass().addAll("setup-start-button", "save-load-button");
        loadBtn.setMaxWidth(Double.MAX_VALUE);
        loadBtn.setOnAction(e -> {
            String err = controller.loadSave(meta.saveDir());
            if (err != null) showError("Load Failed", err);
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().addAll("save-delete-button");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> handleDelete(meta, cardList));

        VBox buttons = new VBox(6, loadBtn, deleteBtn);
        buttons.setAlignment(Pos.CENTER);
        buttons.setFillWidth(true);

        HBox card = new HBox(12, avatarLabel, info, buttons);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("save-card");
        if (meta.autosave()) card.getStyleClass().add("save-card-autosave");
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

        Button resumeBtn = new Button("Resume");
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

    private void handleRename(SaveMeta meta, Label nameLabel, VBox cardList) {
        // ── Header ────────────────────────────────────────────────────────────
        Label iconLbl  = new Label("✏");
        iconLbl.getStyleClass().add("dialog-action-icon-buy");
        Label titleLbl = new Label("RENAME SAVE");
        titleLbl.getStyleClass().add("dialog-title");
        HBox header = new HBox(10, iconLbl, titleLbl);
        header.getStyleClass().add("dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // ── Input ─────────────────────────────────────────────────────────────
        TextField field = new TextField(meta.displayName());
        field.getStyleClass().add("save-rename-field");
        field.setMaxWidth(280);
        Label hint = new Label("Enter a new display name");
        hint.getStyleClass().add("dialog-row-key");
        VBox body = new VBox(8, hint, field);
        body.getStyleClass().add("error-dialog-body");
        body.setPadding(new Insets(16, 22, 8, 22));

        // ── Buttons ───────────────────────────────────────────────────────────
        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-cancel-btn");
        Button confirmBtn = new Button("Rename");
        confirmBtn.getStyleClass().add("dialog-confirm-buy-btn");
        confirmBtn.setDefaultButton(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox btnRow = new HBox(10, cancelBtn, spacer, confirmBtn);
        btnRow.getStyleClass().add("dialog-btn-row");

        VBox card = new VBox(0, header, body, btnRow);
        card.getStyleClass().add("trade-dialog-root");
        card.setMaxWidth(360);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        Region backdrop = new Region();
        backdrop.getStyleClass().add("dialog-backdrop");

        StackPane popup = new StackPane(backdrop, card);
        StackPane.setAlignment(card, Pos.CENTER);

        Runnable dismiss = () -> overlay.getChildren().remove(popup);
        cancelBtn.setOnAction(ev -> dismiss.run());
        backdrop.setOnMouseClicked(ev -> dismiss.run());
        confirmBtn.setOnAction(ev -> {
            String newName = field.getText().strip();
            if (newName.isEmpty()) return;
            String safe = meta.saveDir().getFileName().toString()
                    .replaceFirst("^[^_]+", newName.replaceAll("[^A-Za-z0-9_\\-]", "_"));
            Path newPath = controller.renameSave(meta.saveDir(), safe, newName);
            dismiss.run();
            if (newPath != null) {
                nameLabel.setText(newName);
            } else {
                showError("Rename Failed", "Could not rename the save file.");
            }
        });
        popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) { dismiss.run(); ev.consume(); }
        });

        overlay.getChildren().add(popup);
        field.requestFocus();
        field.selectAll();
    }

    private void handleDelete(SaveMeta meta, VBox cardList) {
        // ── Header ────────────────────────────────────────────────────────────
        Label iconLbl  = new Label("🗑");
        iconLbl.getStyleClass().add("error-dialog-icon");
        Label titleLbl = new Label("DELETE SAVE");
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
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("dialog-confirm-sell-btn");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox btnRow = new HBox(10, cancelBtn, spacer, deleteBtn);
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
        cancelBtn.setOnAction(ev -> dismiss.run());
        backdrop.setOnMouseClicked(ev -> dismiss.run());
        deleteBtn.setOnAction(ev -> {
            dismiss.run();
            String err = controller.deleteSave(meta.saveDir());
            if (err != null) {
                showError("Delete Failed", err);
            } else {
                rebuildCardList(cardList);
            }
        });
        popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) { dismiss.run(); ev.consume(); }
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

    private void showError(String title, String message) {
        Label iconLbl  = new Label("⚠");
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
            if (ev.getCode() == KeyCode.ESCAPE) { dismiss.run(); ev.consume(); }
        });

        overlay.getChildren().add(popup);
        popup.requestFocus();
    }

    private static String formatMoney(String raw) {
        try {
            double val = Double.parseDouble(raw);
            if (val >= 1_000_000) return String.format("%.2fM", val / 1_000_000);
            if (val >= 1_000)    return String.format("%.2fK", val / 1_000);
            return String.format("%.2f", val);
        } catch (NumberFormatException e) {
            return raw;
        }
    }
}
