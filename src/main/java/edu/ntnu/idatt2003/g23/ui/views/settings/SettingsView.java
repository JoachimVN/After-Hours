package edu.ntnu.idatt2003.g23.ui.views.settings;

import edu.ntnu.idatt2003.g23.AppConfig;
import edu.ntnu.idatt2003.g23.io.GameSaveExporter;
import edu.ntnu.idatt2003.g23.io.GameSaveLoader;
import edu.ntnu.idatt2003.g23.io.GameSaveLoader.SaveMeta;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.nio.file.Path;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public final class SettingsView {

    private SettingsView() {}

    // ── No-save overload (home / setup context) ───────────────────────────────

    public static StackPane build(
            Runnable onBack,
            Stage stage,
            DoubleConsumer onMusicVolumeChange, double initialMusicVolume,
            boolean initialMusicMuted, Consumer<Boolean> onMusicMutedChange,
            DoubleConsumer onSfxVolumeChange, double initialSfxVolume,
            boolean initialSfxMuted, Consumer<Boolean> onSfxMutedChange,
            Consumer<Boolean> onAnimationsChange, boolean animationsEnabled,
            boolean initialFullscreen, Consumer<Boolean> onFullscreenChange,
            Consumer<Boolean> onDevModeChange, boolean devModeEnabled,
            Consumer<Boolean> onAutosaveChange, boolean autosaveEnabled,
            Consumer<Boolean> onAutosaveToastChange, boolean autosaveToastEnabled) {
        return build(onBack, stage,
                onMusicVolumeChange, initialMusicVolume, initialMusicMuted, onMusicMutedChange,
                onSfxVolumeChange, initialSfxVolume, initialSfxMuted, onSfxMutedChange,
                onAnimationsChange, animationsEnabled,
                initialFullscreen, onFullscreenChange,
                null, null, null,
                onDevModeChange, devModeEnabled,
                onAutosaveChange, autosaveEnabled,
                onAutosaveToastChange, autosaveToastEnabled,
                null, null, null, null, null);
    }

    // ── Full overload ─────────────────────────────────────────────────────────

    public static StackPane build(
            Runnable onBack,
            Stage stage,
            DoubleConsumer onMusicVolumeChange, double initialMusicVolume,
            boolean initialMusicMuted, Consumer<Boolean> onMusicMutedChange,
            DoubleConsumer onSfxVolumeChange, double initialSfxVolume,
            boolean initialSfxMuted, Consumer<Boolean> onSfxMutedChange,
            Consumer<Boolean> onAnimationsChange, boolean animationsEnabled,
            boolean initialFullscreen, Consumer<Boolean> onFullscreenChange,
            /** Called when user picks a preset resolution: [width, height]. Null-safe. */
            Consumer<int[]> onResolutionChange,
            /** Called when user clicks "Maximize". Null-safe. */
            Runnable onMaximize,
            /** Called with the File chosen by the user for CSV export. Null-safe. */
            Consumer<File> onExport,
            Consumer<Boolean> onDevModeChange, boolean devModeEnabled,
            Consumer<Boolean> onAutosaveChange, boolean autosaveEnabled,
            Consumer<Boolean> onAutosaveToastChange, boolean autosaveToastEnabled,
            Path currentSavePath,
            Runnable onResetAll,
            Runnable onSave,
            /** Current player name if in-game context (null-safe). */
            String currentPlayerName,
            /** Called with new player name if in-game (null-safe). */
            Consumer<String> onNameChanged) {

        StackPane overlay = new StackPane();
        overlay.setPickOnBounds(false);

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("home-page", "background-overlay");

        // ── Top bar ───────────────────────────────────────────────────────────
        Button backButton = new Button("\u2190  Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> onBack.run());

        Button resetBtn = new Button("↺  Reset to Defaults");
        resetBtn.getStyleClass().add("settings-reset-btn");
        if (onResetAll != null) {
            resetBtn.setOnAction(e -> onResetAll.run());
        } else {
            resetBtn.setVisible(false);
            resetBtn.setManaged(false);
        }

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topBar = new HBox(backButton, topSpacer, resetBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(24, 32, 0, 32));
        root.setTop(topBar);

        // ── Title ─────────────────────────────────────────────────────────────
        Label title = new Label("Settings");
        title.getStyleClass().add("page-title");
        title.setPadding(new Insets(0, 0, 8, 0));

        // ── Sections ──────────────────────────────────────────────────────────
        VBox audioSection = buildAudioSection(
                onMusicVolumeChange, initialMusicVolume, initialMusicMuted, onMusicMutedChange,
                onSfxVolumeChange, initialSfxVolume, initialSfxMuted, onSfxMutedChange);
        audioSection.getStyleClass().add("settings-section-card-top");

        VBox displaySection = buildDisplaySection(
                animationsEnabled, onAnimationsChange,
                initialFullscreen, onFullscreenChange,
                onResolutionChange, onMaximize);

        VBox gameSection = buildGameSection(
                autosaveEnabled, onAutosaveChange,
                autosaveToastEnabled, onAutosaveToastChange,
                onSave);

        VBox dataSection = buildDataSection(stage, currentSavePath, onExport);
        VBox keybindsSection = buildKeybindsSection(overlay);
        VBox devSection = buildDevSection(devModeEnabled, onDevModeChange);

        VBox profileSection = null;
        if (currentPlayerName != null && onNameChanged != null) {
            profileSection = buildProfileSection(currentPlayerName, onNameChanged);
        }

        VBox allSections;
        if (profileSection != null) {
            allSections = new VBox(22,
                    title, profileSection, audioSection, displaySection, gameSection,
                    dataSection, keybindsSection, devSection);
        } else {
            allSections = new VBox(22,
                    title, audioSection, displaySection, gameSection,
                    dataSection, keybindsSection, devSection);
        }
        allSections.setAlignment(Pos.TOP_LEFT);
        allSections.setMaxWidth(700);
        HBox.setHgrow(allSections, Priority.ALWAYS);

        HBox centeringBox = new HBox(allSections);
        centeringBox.setAlignment(Pos.TOP_CENTER);
        centeringBox.setPadding(new Insets(28, 48, 56, 48));

        ScrollPane scroll = new ScrollPane(centeringBox);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("settings-scroll");

        root.setCenter(scroll);

        // ── Key bindings ────────────────────────────────────────────────────────────────
        StackPane rootPane = new StackPane(root, overlay);
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                if (!overlay.getChildren().isEmpty()) {
                    overlay.getChildren().clear();
                    e.consume();
                } else {
                    onBack.run();
                    e.consume();
                }
            }
        });

        return rootPane;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Section builders
    // ──────────────────────────────────────────────────────────────────────────

    private static VBox buildProfileSection(String currentPlayerName, Consumer<String> onNameChanged) {
        TextField nameField = new TextField(currentPlayerName);
        nameField.getStyleClass().addAll("settings-text-field", "settings-profile-name-field");
        nameField.setMaxWidth(Double.MAX_VALUE);
        nameField.setPromptText("Player name");

        Runnable saveName = () -> {
            String newName = nameField.getText().strip();
            if (!newName.isEmpty() && !newName.equals(currentPlayerName)) {
                onNameChanged.accept(newName);
            } else if (newName.isEmpty()) {
                nameField.setText(currentPlayerName);
            }
        };

        nameField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) saveName.run();
        });
        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                saveName.run();
                nameField.getParent().requestFocus();
            }
        });

        Label label = new Label("Player Name");
        label.getStyleClass().add("settings-label");
        return sectionCard("👤  Profile", label, nameField);
    }

    private static VBox buildAudioSection(
            DoubleConsumer onMusicVolumeChange, double initialMusicVolume,
            boolean initialMusicMuted, Consumer<Boolean> onMusicMutedChange,
            DoubleConsumer onSfxVolumeChange, double initialSfxVolume,
            boolean initialSfxMuted, Consumer<Boolean> onSfxMutedChange) {

        VBox musicBlock = volumeBlock("Music Volume", initialMusicVolume * 100.0,
                initialMusicMuted, onMusicVolumeChange, onMusicMutedChange);
        VBox sfxBlock = volumeBlock("Sound Effects", initialSfxVolume * 100.0,
                initialSfxMuted, onSfxVolumeChange, onSfxMutedChange);
        return sectionCard("\uD83D\uDD0A  Audio", musicBlock, sfxBlock);
    }

    private static VBox buildDisplaySection(
            boolean animationsEnabled, Consumer<Boolean> onAnimationsChange,
            boolean initialFullscreen, Consumer<Boolean> onFullscreenChange,
            Consumer<int[]> onResolutionChange, Runnable onMaximize) {

        VBox animRow = toggleRow("Background Animations", animationsEnabled, false, true, onAnimationsChange);
        // The view only notifies the controller; the controller applies the change to the stage.
        VBox fullscreenRow = toggleRow("Fullscreen", initialFullscreen, false, false, onFullscreenChange);
        VBox resBlock = buildResolutionBlock(onResolutionChange, onMaximize);
        return sectionCard("\uD83D\uDDA5  Display", animRow, fullscreenRow, resBlock);
    }

    private static VBox buildResolutionBlock(Consumer<int[]> onResolutionChange, Runnable onMaximize) {
        Label label = new Label("Window Size");
        label.getStyleClass().add("settings-label");

        String[] presetLabels = {"1280 \u00d7 720", "1600 \u00d7 900", "1920 \u00d7 1080", "2560 \u00d7 1440", "Custom\u2026"};
        int[]    presetW      = {1280, 1600, 1920, 2560, 0};
        int[]    presetH      = {720,  900,  1080, 1440, 0};

        ComboBox<String> presetBox = new ComboBox<>();
        presetBox.getItems().addAll(presetLabels);
        presetBox.setPromptText("Pick a preset\u2026");
        presetBox.getStyleClass().add("settings-combo");
        presetBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(presetBox, Priority.ALWAYS);

        Button maximizeBtn = new Button("\u26F6  Maximize");
        maximizeBtn.getStyleClass().add("settings-toggle");

        TextField wField = new TextField();
        wField.setPromptText("W");
        wField.getStyleClass().add("settings-resolution-field");
        wField.setPrefWidth(65);

        TextField hField = new TextField();
        hField.setPromptText("H");
        hField.getStyleClass().add("settings-resolution-field");
        hField.setPrefWidth(65);

        Label xLabel = new Label("\u00d7");
        xLabel.getStyleClass().add("settings-label");

        Button applyBtn = new Button("Apply");
        applyBtn.getStyleClass().add("settings-apply-btn");

        HBox customRow = new HBox(8, wField, xLabel, hField, applyBtn);
        customRow.setAlignment(Pos.CENTER_LEFT);
        customRow.setVisible(false);
        customRow.setManaged(false);

        presetBox.getSelectionModel().selectedIndexProperty().addListener((obs, o, idx) -> {
            int i = idx.intValue();
            if (i < 0) return;
            boolean isCustom = presetW[i] == 0;
            customRow.setVisible(isCustom);
            customRow.setManaged(isCustom);
            if (!isCustom && onResolutionChange != null) {
                onResolutionChange.accept(new int[]{presetW[i], presetH[i]});
            }
        });

        applyBtn.setOnAction(e -> {
            try {
                int w = Integer.parseInt(wField.getText().trim());
                int h = Integer.parseInt(hField.getText().trim());
                if (w >= (int) AppConfig.MIN_WIDTH && h >= (int) AppConfig.MIN_HEIGHT
                        && onResolutionChange != null) {
                    onResolutionChange.accept(new int[]{w, h});
                }
            } catch (NumberFormatException ignored) {}
        });

        maximizeBtn.setOnAction(e -> {
            presetBox.getSelectionModel().clearSelection();
            if (onMaximize != null) onMaximize.run();
        });

        HBox topRow = new HBox(10, presetBox, maximizeBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, label, topRow, customRow);
    }

    private static VBox buildGameSection(
            boolean autosaveEnabled, Consumer<Boolean> onAutosaveChange,
            boolean autosaveToastEnabled, Consumer<Boolean> onAutosaveToastChange,
            Runnable onSave) {

        VBox autosaveRow = toggleRow("Autosave (every minute)", autosaveEnabled, false, false, onAutosaveChange);
        VBox toastRow = toggleRow("Show Autosave Notification", autosaveToastEnabled, false, true, onAutosaveToastChange);

        if (onSave != null) {
            Button saveBtn = new Button("\uD83D\uDCBE  Save Game Now");
            saveBtn.getStyleClass().add("settings-save-game-btn");
            saveBtn.setMaxWidth(Double.MAX_VALUE);
            saveBtn.setOnAction(e -> onSave.run());
            return sectionCard("\uD83C\uDFAE  Game", autosaveRow, toastRow, new VBox(12, saveBtn));
        }
        return sectionCard("\uD83C\uDFAE  Game", autosaveRow, toastRow);
    }

    private static VBox buildDataSection(Stage stage, Path currentSavePath, Consumer<File> onExport) {
        Label label = new Label("Export Save Data");
        label.getStyleClass().add("settings-label");

        Label subLabel = new Label("Pick a save to export it as two files: one JSON and one CSV.");
        subLabel.getStyleClass().add("settings-sublabel");
        subLabel.setWrapText(true);

        ComboBox<SaveMeta> saveCombo = new ComboBox<>();
        saveCombo.setPromptText("Select a save\u2026");
        saveCombo.getStyleClass().add("settings-combo");
        saveCombo.setMaxWidth(Double.MAX_VALUE);
        saveCombo.setCellFactory(lv -> saveMetaCell(currentSavePath));
        saveCombo.setButtonCell(saveMetaCell(currentSavePath));

        try {
            List<SaveMeta> saves = GameSaveLoader.listSaves();
            saveCombo.getItems().setAll(saves);
        } catch (IOException ignored) {}

        Button exportBtn = new Button("\u2B07  Export JSON + CSV");
        exportBtn.getStyleClass().add("settings-toggle");
        exportBtn.disableProperty().bind(saveCombo.getSelectionModel().selectedItemProperty().isNull());

        Label statusLbl = new Label();
        statusLbl.getStyleClass().add("settings-sublabel");
        statusLbl.setWrapText(true);

        exportBtn.setOnAction(e -> {
            SaveMeta selected = saveCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            // The view owns the file-chooser dialog (it needs an owner window).
            // The actual file I/O is delegated to the controller via onExport.
            FileChooser fc = new FileChooser();
            fc.setTitle("Export Save Data (JSON + CSV)");
            fc.setInitialFileName(
                    selected.displayName().replaceAll("[^a-zA-Z0-9_\\-]", "_") + "_save_export");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File dest = fc.showSaveDialog(stage);
            if (dest == null) return;

            if (onExport != null) {
                try {
                    Path[] exported = GameSaveExporter.exportSaveDataFiles(selected.saveDir(), dest.toPath());
                    statusLbl.setText("\u2713  Exported: " + exported[0].getFileName() + " and " + exported[1].getFileName());
                    statusLbl.setStyle("-fx-text-fill: #4ecb71;");
                    onExport.accept(dest);
                } catch (IOException ex) {
                    statusLbl.setText("\u2715  Export failed: " + ex.getMessage());
                    statusLbl.setStyle("-fx-text-fill: #e05a5a;");
                }
            }
        });

        HBox btnRow = new HBox(exportBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(8, subLabel, saveCombo, btnRow, statusLbl);
        return sectionCard("\uD83D\uDCC4  Data", new VBox(6, label, content));
    }

    private static VBox buildKeybindsSection(StackPane overlay) {
        Button viewBtn = new Button("\u2328  View All Keybinds");
        viewBtn.getStyleClass().add("settings-toggle");
        viewBtn.setOnAction(e -> showKeybindsPopup(overlay));

        HBox btnRow = new HBox(viewBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        return sectionCard("\u2328  Keybinds", btnRow);
    }

    private static void showKeybindsPopup(StackPane overlay) {
        // ── Dim layer ────────────────────────────────────────────────────────────────
        StackPane dim = new StackPane();
        dim.getStyleClass().add("keybind-popup-dim");
        dim.setAlignment(Pos.CENTER);

        // ── Card ────────────────────────────────────────────────────────────────────
        Label title = new Label("\u2328  Keyboard Shortcuts");
        title.getStyleClass().add("keybind-popup-title");
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);

        Button closeBtn = new Button("\u2715");
        closeBtn.getStyleClass().add("keybind-popup-close");
        closeBtn.setOnAction(e -> overlay.getChildren().clear());

        HBox headerRow = new HBox(title, closeBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);        headerRow.setMaxWidth(Double.MAX_VALUE);
        Separator headerSep = new Separator();
        headerSep.getStyleClass().add("settings-section-sep");
        headerSep.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(20,
            keybindGroup("Main Menu", new String[][]{
                {"Enter",         "Start / Continue game"},
                {"S",             "Open Settings"},
            }),
            keybindGroup("Navigation (All Pages)", new String[][]{
                {"Esc",           "Go back / close popup"},
            }),
            keybindGroup("New Game Setup", new String[][]{
                {"Enter",         "Confirm / advance step"},
            }),
            keybindGroup("Import CSV / Save Select", new String[][]{
                {"Enter / Space", "Confirm selection"},
                {"Esc",          "Go back"},
            }),
            keybindGroup("CSV Editor", new String[][]{
                {"Tab",           "Move to next cell"},
                {"Enter",         "Commit cell edit"},
                {"Esc",           "Cancel edit"},
            }),
            keybindGroup("In-Game", new String[][]{
                {"N / Space",     "Advance to next week"},
                {"/",             "Focus stock search"},
                {"Ctrl + F",      "Focus stock search"},
                {"M",             "Open Market Movers"},
                {"H",             "Open Transaction History"},
                {"Esc",           "Clear search / go back"},
            })
        );
        content.setPadding(new Insets(4, 0, 4, 0));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("settings-scroll");
        scroll.setPrefHeight(400);
        scroll.setMaxHeight(500);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox card = new VBox(14, headerRow, headerSep, scroll);
        card.getStyleClass().add("keybind-popup-card");
        card.setPadding(new Insets(22, 26, 22, 26));
        card.setMaxWidth(580);
        card.setMaxHeight(560);

        dim.getChildren().add(card);
        dim.setOnMouseClicked(e -> {
            if (e.getTarget() == dim) overlay.getChildren().clear();
        });

        overlay.getChildren().setAll(dim);
    }

    private static VBox keybindGroup(String groupName, String[][] binds) {
        Label groupLabel = new Label(groupName.toUpperCase());
        groupLabel.getStyleClass().add("keybind-popup-group");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);

        for (int i = 0; i < binds.length; i++) {
            Label keyLbl = new Label(binds[i][0]);
            keyLbl.getStyleClass().add("settings-keybind-key");

            Label descLbl = new Label(binds[i][1]);
            descLbl.getStyleClass().add("settings-keybind-desc");

            grid.add(keyLbl, 0, i);
            grid.add(descLbl, 1, i);
        }

        return new VBox(8, groupLabel, grid);
    }

    private static VBox buildDevSection(boolean devModeEnabled, Consumer<Boolean> onDevModeChange) {
        VBox devRow = toggleRow("Developer Mode", devModeEnabled, true, false, isOn -> {
            AppConfig.DEV_MODE.set(isOn);
            onDevModeChange.accept(isOn);
        });
        Label note = new Label("Shows extra debug information during gameplay.");
        note.getStyleClass().add("settings-sublabel");
        return sectionCard("\uD83D\uDEE0  Developer", devRow, new VBox(note));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ──────────────────────────────────────────────────────────────────────────

    private static VBox sectionCard(String sectionTitle, Node... content) {
        Label header = new Label(sectionTitle);
        header.getStyleClass().add("settings-section-title");

        Separator sep = new Separator();
        sep.getStyleClass().add("settings-section-sep");
        sep.setMaxWidth(Double.MAX_VALUE);

        VBox body = new VBox(14, content);

        VBox card = new VBox(10, header, sep, body);
        card.getStyleClass().add("settings-section-card");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private static VBox toggleRow(String labelText, boolean initialOn,
                                   boolean isDanger, boolean defaultOn,
                                   Consumer<Boolean> onChange) {
        Label label = new Label(labelText);
        label.getStyleClass().add("settings-label");

        Label defaultTag = new Label("Default: " + (defaultOn ? "ON" : "OFF"));
        defaultTag.getStyleClass().add("settings-default-tag");

        VBox labelCol = new VBox(2, label, defaultTag);
        HBox.setHgrow(labelCol, Priority.ALWAYS);

        ToggleButton toggle = new ToggleButton(initialOn ? "ON" : "OFF");
        toggle.setSelected(initialOn);
        toggle.getStyleClass().add("settings-toggle");
        if (isDanger) toggle.getStyleClass().add("settings-toggle-danger");

        toggle.selectedProperty().addListener((obs, wasOn, isOn) -> {
            toggle.setText(isOn ? "ON" : "OFF");
            onChange.accept(isOn);
        });

        HBox row = new HBox(12, labelCol, toggle);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        return new VBox(row);
    }

    private static VBox volumeBlock(String labelText, double initialValue,
                                     boolean initialMuted,
                                     DoubleConsumer onVolumeChange,
                                     Consumer<Boolean> onMuteChange) {
        Label label = new Label(labelText);
        label.getStyleClass().add("settings-label");

        Label defaultVolumeTag = new Label("Default: 50%");
        defaultVolumeTag.getStyleClass().add("settings-default-tag");

        Slider slider = new Slider(0.0, 100.0, initialValue);
        slider.getStyleClass().add("settings-slider");
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(25);
        HBox.setHgrow(slider, Priority.ALWAYS);

        ToggleButton muteToggle = new ToggleButton(initialMuted ? "MUTED" : "ON");
        muteToggle.setSelected(initialMuted);
        muteToggle.getStyleClass().addAll("settings-toggle", "settings-mute-toggle");
        muteToggle.setMinWidth(80);
        muteToggle.setMaxWidth(80);

        slider.valueProperty().addListener((obs, old, val) -> {
            if (!muteToggle.isSelected()) {
                onVolumeChange.accept(val.doubleValue() / 100.0);
            }
        });

        muteToggle.selectedProperty().addListener((obs, wasOn, isMuted) -> {
            muteToggle.setText(isMuted ? "MUTED" : "ON");
            onMuteChange.accept(isMuted);
        });

        HBox sliderRow = new HBox(12, slider, muteToggle);
        sliderRow.setAlignment(Pos.CENTER_LEFT);
        sliderRow.setMaxWidth(Double.MAX_VALUE);

        return new VBox(4, label, defaultVolumeTag, sliderRow);
    }

    private static ListCell<SaveMeta> saveMetaCell(Path currentSavePath) {
        return new ListCell<>() {
            @Override
            protected void updateItem(SaveMeta item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    boolean isCurrent = currentSavePath != null
                            && item.saveDir() != null
                            && item.saveDir().equals(currentSavePath);
                    String prefix = isCurrent ? "▶  " : (item.autosave() ? "⌛ " : "");
                    setText(prefix + item.displayName()
                            + "  •  Week " + item.week()
                            + "  •  " + item.savedAt()
                            + (isCurrent ? "  — Playing" : ""));
                    setStyle(isCurrent ? "-fx-text-fill: #f5a201; -fx-font-weight: bold;" : "");
                }
            }
        };
    }
}
