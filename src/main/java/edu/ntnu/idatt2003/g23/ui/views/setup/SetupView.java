package edu.ntnu.idatt2003.g23.ui.views.setup;

import java.util.List;
import java.util.function.BiConsumer;

import edu.ntnu.idatt2003.g23.AppConfig;
import edu.ntnu.idatt2003.g23.model.MarketOption;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.util.Duration;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * View for the new-game setup screen.
 *
 * Mirrors the GameView pattern: the constructor receives the data it needs and
 * creates its own {@link SetupController} internally. The view is responsible
 * solely for layout; all decisions are delegated to the controller.
 */
public final class SetupView {

    private final SetupController controller;
    private final BorderPane root;

    /**
     * Creates the setup view.
     *
     * @param onBack         navigate back to the home screen
     * @param onStartDefault start a game with a built-in market
     * @param onStartCsv     navigate to CSV import (name + cash chosen here)
     */
    public SetupView(Runnable onBack,
                     MarketStartHandler onStartDefault,
                     BiConsumer<String, Double> onStartCsv) {
        this.controller = new SetupController(onBack, onStartDefault, onStartCsv);
        this.root = buildUI();
    }

    public BorderPane getRoot() {
        return root;
    }

    // ── UI Construction ───────────────────────────────────────────────────────

    private BorderPane buildUI() {
        List<MarketOption> markets = AppConfig.BUILT_IN_MARKETS;

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("home-page", "background-overlay");

        // ── Top bar ───────────────────────────────────────────────────────────
        Button backButton = new Button("\u2190 Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> controller.handleBack());

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

        double[] presetValues = AppConfig.PRESET_CASH_VALUES;
        String[] presetLabels = AppConfig.PRESET_CASH_LABELS;

        ToggleGroup presetGroup = new ToggleGroup();
        HBox presetRow = new HBox(8);
        presetRow.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < presetValues.length; i++) {
            ToggleButton btn = new ToggleButton(presetLabels[i]);
            btn.getStyleClass().add("cash-preset-button");
            btn.setToggleGroup(presetGroup);
            btn.setUserData(presetValues[i]);
            presetRow.getChildren().add(btn);
        }

        TextField cashField = new TextField();
        cashField.setPromptText("Custom amount");
        cashField.getStyleClass().add("setup-text-field");
        cashField.setPrefWidth(150);

        // Preset -> cashField sync (guard against feedback loop)
        boolean[] fromPreset = {false};
        presetGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            if (sel != null && sel.getUserData() instanceof Double amount) {
                fromPreset[0] = true;
                cashField.setText(String.valueOf(amount.longValue()));
                fromPreset[0] = false;
            }
        });
        cashField.textProperty().addListener((obs, old, text) -> {
            if (!fromPreset[0]) presetGroup.selectToggle(null);
        });

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

        ToggleButton defaultBtn = new ToggleButton("\uD83D\uDCC8   Default Stocks");
        defaultBtn.getStyleClass().add("data-toggle-button");
        defaultBtn.setToggleGroup(dataGroup);
        defaultBtn.setSelected(true);
        defaultBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(defaultBtn, Priority.ALWAYS);

        // Prevent full deselection; notify controller on change
        dataGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            if (sel == null) { dataGroup.selectToggle(old); return; }
            controller.setUseDefaultStocks(sel == defaultBtn);
        });

        HBox dataRow = new HBox(10, csvBtn, defaultBtn);

        // ── Market ComboBox (shown when Default Stocks is active) ─────────────
        ComboBox<MarketOption> marketBox = new ComboBox<>();
        marketBox.getItems().addAll(markets);
        marketBox.setValue(markets.get(0));
        marketBox.getStyleClass().add("market-combo-box");
        marketBox.setMaxWidth(Double.MAX_VALUE);
        marketBox.setButtonCell(marketCell());
        marketBox.setCellFactory(lv -> marketCell());

        marketBox.valueProperty().addListener((obs, old, market) -> {
            if (market != null) controller.selectMarket(markets.indexOf(market));
        });

        Label marketLabel = new Label("MARKET");
        marketLabel.getStyleClass().add("setup-field-label");

        VBox marketSection = new VBox(6, marketLabel, marketBox);
        marketSection.setMinHeight(0);

        // Measure real preferred height regardless of DPI/font/CSS by forcing
        // a CSS + layout pass before the scene is attached.
        marketSection.setMaxWidth(580); // approximate card inner width for measurement
        marketSection.applyCss();
        marketSection.layout();
        final double SECTION_PREF = marketSection.prefHeight(-1);
        marketSection.setMaxWidth(Double.MAX_VALUE);

        // Start expanded (defaultBtn is selected on open)
        marketSection.setMaxHeight(SECTION_PREF);

        // Clip prevents content bleeding outside the VBox during close animation
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(marketSection.widthProperty());
        clip.setHeight(SECTION_PREF);
        marketSection.setClip(clip);

        // Smooth slide open/close when toggling data source
        dataGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean toDefault = (sel == defaultBtn);
            double target = toDefault ? SECTION_PREF : 0;
            Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(220),
                    new KeyValue(marketSection.maxHeightProperty(), target,
                        javafx.animation.Interpolator.EASE_BOTH),
                    new KeyValue(clip.heightProperty(), target,
                        javafx.animation.Interpolator.EASE_BOTH))
            );
            tl.play();
        });
        // managed drives layout space; collapses gap when section is hidden
        marketSection.managedProperty().bind(marketSection.maxHeightProperty().greaterThan(0));

        VBox dataSection = new VBox(8, dataLabel, dataRow, marketSection);

        // ── Start Game button ─────────────────────────────────────────────────
        Button startButton = new Button("\u25B6   Start Game");
        startButton.getStyleClass().add("start-button");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setDisable(true);
        startButton.setOnAction(e ->
            controller.handleStart(nameField.getText(), cashField.getText())
        );

        Runnable updateStartEnabled = () -> {
            boolean ready = controller.isCashReady(
                    presetGroup.getSelectedToggle() != null, cashField.getText());
            startButton.setDisable(!ready);
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

        // ── Keybindings ───────────────────────────────────────────────────────
        nameField.setOnAction(e -> cashField.requestFocus());
        cashField.setOnAction(e -> startButton.fire());
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ESCAPE -> { controller.handleBack(); e.consume(); }
                case SPACE -> {
                    if (e.getTarget() != nameField && !startButton.isDisable()) {
                        startButton.fire(); e.consume();
                    }
                }
                case ENTER -> {
                    if (e.getTarget() != nameField && e.getTarget() != cashField
                            && !startButton.isDisable()) {
                        startButton.fire(); e.consume();
                    }
                }
                default -> {}
            }
        });

        return root;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ListCell<MarketOption> marketCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(MarketOption market, boolean empty) {
                super.updateItem(market, empty);
                if (empty || market == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label name = new Label(market.name());
                    name.getStyleClass().add("market-combo-name");
                    Label desc = new Label(market.description());
                    desc.getStyleClass().add("market-combo-desc");
                    setGraphic(new VBox(2, name, desc));
                    setText(null);
                }
            }
        };
    }
}
