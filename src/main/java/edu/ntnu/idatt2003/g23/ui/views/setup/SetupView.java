package edu.ntnu.idatt2003.g23.ui.views.setup;

import java.util.function.BiConsumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class SetupView {

    private static final double[] PRESETS      = {1_000, 5_000, 10_000, 50_000, 100_000};
    private static final String[] PRESET_LABELS = {"$1K", "$5K", "$10K", "$50K", "$100K"};

    public static BorderPane build(
            Runnable onBack,
            BiConsumer<String, Double> onStartDefault,
            BiConsumer<String, Double> onStartCsv) {

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("home-page", "background-overlay");

        // ── Top bar ──────────────────────────────────────────────────────────
        Button backButton = new Button("\u2190 Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> onBack.run());

        HBox topBar = new HBox(backButton);
        topBar.setPadding(new Insets(24, 32, 0, 32));
        root.setTop(topBar);

        // ── Player Name ───────────────────────────────────────────────────────
        Label nameLabel = new Label("PLAYER NAME");
        nameLabel.getStyleClass().add("setup-field-label");

        TextField nameField = new TextField();
        nameField.setPromptText("Enter your name");
        nameField.getStyleClass().add("setup-text-field");

        VBox nameSection = new VBox(8, nameLabel, nameField);

        // ── Starting Cash ─────────────────────────────────────────────────────
        Label cashLabel = new Label("STARTING CASH");
        cashLabel.getStyleClass().add("setup-field-label");

        ToggleGroup presetGroup = new ToggleGroup();
        HBox presetRow = new HBox(8);
        presetRow.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < PRESETS.length; i++) {
            ToggleButton btn = new ToggleButton(PRESET_LABELS[i]);
            btn.getStyleClass().add("cash-preset-button");
            btn.setToggleGroup(presetGroup);
            btn.setUserData(PRESETS[i]);
            presetRow.getChildren().add(btn);
        }

        TextField cashField = new TextField();
        cashField.setPromptText("Custom amount");
        cashField.getStyleClass().add("setup-text-field");
        cashField.setPrefWidth(150);

        // Preset → cashField sync (guard against feedback loop)
        boolean[] fromPreset = {false};
        presetGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            if (sel != null && sel.getUserData() instanceof Double amount) {
                fromPreset[0] = true;
                cashField.setText(String.valueOf(amount.longValue()));
                fromPreset[0] = false;
            }
        });

        // Manual edit → deselect preset
        cashField.textProperty().addListener((obs, old, text) -> {
            if (!fromPreset[0]) presetGroup.selectToggle(null);
        });

        // Do not pre-select any preset — cash field starts empty

        HBox cashInput = new HBox(10, presetRow, cashField);
        cashInput.setAlignment(Pos.CENTER_LEFT);

        VBox cashSection = new VBox(8, cashLabel, cashInput);

        // ── Stock Data Source ─────────────────────────────────────────────────
        Label dataLabel = new Label("STOCK DATA");
        dataLabel.getStyleClass().add("setup-field-label");

        ToggleGroup dataGroup = new ToggleGroup();

        ToggleButton csvBtn = new ToggleButton("\uD83D\uDCC2   Import CSV");
        csvBtn.getStyleClass().add("data-toggle-button");
        csvBtn.setToggleGroup(dataGroup);
        csvBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(csvBtn, Priority.ALWAYS);

        ToggleButton defaultBtn = new ToggleButton("\uD83D\uDCC8   Default Stocks  (S&P 500)");
        defaultBtn.getStyleClass().add("data-toggle-button");
        defaultBtn.setToggleGroup(dataGroup);
        defaultBtn.setSelected(true);
        defaultBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(defaultBtn, Priority.ALWAYS);

        // Prevent full deselection
        dataGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            if (sel == null) dataGroup.selectToggle(old);
        });

        HBox dataRow = new HBox(10, csvBtn, defaultBtn);
        VBox dataSection = new VBox(8, dataLabel, dataRow);

        // ── Start Game button ─────────────────────────────────────────────────
        Button startButton = new Button("\u25B6   Start Game");
        startButton.getStyleClass().add("start-button");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setDisable(true); // enabled once cash is chosen
        startButton.setOnAction(e -> {
            String name = nameField.getText().isBlank() ? "Player" : nameField.getText().trim();
            double cash = parseCash(cashField.getText());
            if (dataGroup.getSelectedToggle() == csvBtn) {
                onStartCsv.accept(name, cash);
            } else {
                onStartDefault.accept(name, cash);
            }
        });

        // Keep start button disabled until a cash amount is provided
        Runnable updateStartEnabled = () -> {
            boolean cashReady = presetGroup.getSelectedToggle() != null
                    || (!cashField.getText().isBlank() && parseCash(cashField.getText()) > 0);
            startButton.setDisable(!cashReady);
        };
        presetGroup.selectedToggleProperty().addListener((obs, old, sel) -> updateStartEnabled.run());
        cashField.textProperty().addListener((obs, old, text) -> updateStartEnabled.run());
        updateStartEnabled.run();

        // ── Form card ─────────────────────────────────────────────────────────
        VBox form = new VBox(28, nameSection, cashSection, dataSection, startButton);
        form.setPadding(new Insets(36, 40, 40, 40));
        form.setMaxWidth(660);
        form.getStyleClass().add("setup-card");

        Label pageTitle = new Label("New Game");
        pageTitle.getStyleClass().add("page-title");

        VBox page = new VBox(28, pageTitle, form);
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(0, 0, 40, 0));
        root.setCenter(page);

        // ── Keybindings ───────────────────────────────────────────────────────────
        // Enter in name field → advance to cash field
        nameField.setOnAction(e -> cashField.requestFocus());
        // Enter in cash field → start game
        cashField.setOnAction(e -> startButton.fire());
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ESCAPE -> { onBack.run(); e.consume(); }
                // Space anywhere (except name field) → start only if cash is set
                case SPACE -> {
                    if (e.getTarget() != nameField && !startButton.isDisable()) { startButton.fire(); e.consume(); }
                }
                // Enter when focus is not on a handled text field → start only if cash is set
                case ENTER -> {
                    if (e.getTarget() != nameField && e.getTarget() != cashField && !startButton.isDisable()) {
                        startButton.fire(); e.consume();
                    }
                }
                default -> {}
            }
        });

        return root;
    }

    private static double parseCash(String text) {
        try {
            String cleaned = text.replaceAll("[^0-9.]", "");
            double val = Double.parseDouble(cleaned);
            return val > 0 ? val : 10_000;
        } catch (NumberFormatException e) {
            return 10_000;
        }
    }
}
