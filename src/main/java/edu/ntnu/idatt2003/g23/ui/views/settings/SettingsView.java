package edu.ntnu.idatt2003.g23.ui.views.settings;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import edu.ntnu.idatt2003.g23.AppConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class SettingsView {

    /**
     * Builds the settings view without a save button (home/setup context).
     */
    public static BorderPane build(
            Runnable onBack,
            DoubleConsumer onMusicVolumeChange, double initialMusicVolume,
            boolean initialMusicMuted, Consumer<Boolean> onMusicMutedChange,
            DoubleConsumer onSfxVolumeChange, double initialSfxVolume,
            boolean initialSfxMuted, Consumer<Boolean> onSfxMutedChange,
            Consumer<Boolean> onAnimationsChange, boolean animationsEnabled,
            Consumer<Boolean> onDevModeChange, boolean devModeEnabled,
            Consumer<Boolean> onAutosaveChange, boolean autosaveEnabled,
            Consumer<Boolean> onAutosaveToastChange, boolean autosaveToastEnabled) {
        return build(onBack,
                onMusicVolumeChange, initialMusicVolume, initialMusicMuted, onMusicMutedChange,
                onSfxVolumeChange, initialSfxVolume, initialSfxMuted, onSfxMutedChange,
                onAnimationsChange, animationsEnabled,
                onDevModeChange, devModeEnabled,
                onAutosaveChange, autosaveEnabled,
                onAutosaveToastChange, autosaveToastEnabled,
                null);
    }

    /**
     * Builds the settings view, optionally with a Save Game button.
     *
     * @param onSave called when Save Game is pressed; {@code null} = no button shown
     */
    public static BorderPane build(
            Runnable onBack,
            DoubleConsumer onMusicVolumeChange, double initialMusicVolume,
            boolean initialMusicMuted, Consumer<Boolean> onMusicMutedChange,
            DoubleConsumer onSfxVolumeChange, double initialSfxVolume,
            boolean initialSfxMuted, Consumer<Boolean> onSfxMutedChange,
            Consumer<Boolean> onAnimationsChange, boolean animationsEnabled,
            Consumer<Boolean> onDevModeChange, boolean devModeEnabled,
            Consumer<Boolean> onAutosaveChange, boolean autosaveEnabled,
            Consumer<Boolean> onAutosaveToastChange, boolean autosaveToastEnabled,
            Runnable onSave) {

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("home-page", "background-overlay");

        Button backButton = new Button("\u2190 Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> onBack.run());

        HBox topBar = new HBox(backButton);
        topBar.setPadding(new Insets(24, 32, 0, 32));
        root.setTop(topBar);

        Label title = new Label("Settings");
        title.getStyleClass().add("page-title");

        // ── Music Volume ──────────────────────────────────────────────────────
        VBox musicBlock = volumeBlock("Music Volume", initialMusicVolume * 100.0,
                initialMusicMuted, onMusicVolumeChange, onMusicMutedChange);

        // ── SFX Volume ────────────────────────────────────────────────────────
        VBox sfxBlock = volumeBlock("Sound Effects Volume", initialSfxVolume * 100.0,
                initialSfxMuted, onSfxVolumeChange, onSfxMutedChange);

        // ── Animations ────────────────────────────────────────────────────────
        VBox animRow = toggleRow("Background Animations", animationsEnabled,
                "settings-mute-button", onAnimationsChange);

        // ── Dev Mode ──────────────────────────────────────────────────────────
        VBox devRow = toggleRow("Developer Mode", devModeEnabled,
                "settings-mute-button settings-dev-button", isOn -> {
                    AppConfig.DEV_MODE.set(isOn);
                    onDevModeChange.accept(isOn);
                });

        // ── Autosave ──────────────────────────────────────────────────────────
        VBox autosaveRow = toggleRow("Autosave (every minute)", autosaveEnabled,
                "settings-mute-button", onAutosaveChange);

        // ── Autosave Toast ────────────────────────────────────────────────────
        VBox toastRow = toggleRow("Show Autosave Notification", autosaveToastEnabled,
                "settings-mute-button", onAutosaveToastChange);

        VBox settingsBlock = new VBox(28, musicBlock, sfxBlock, animRow, devRow, autosaveRow, toastRow);
        settingsBlock.setAlignment(Pos.CENTER_LEFT);
        settingsBlock.setMaxWidth(500);

        // ── Save Game (only in game context) ──────────────────────────────────
        if (onSave != null) {
            Button saveBtn = new Button("\uD83D\uDCBE  Save Game");
            saveBtn.getStyleClass().addAll("settings-mute-button", "save-game-button");
            saveBtn.setMaxWidth(Double.MAX_VALUE);
            saveBtn.setOnAction(e -> onSave.run());
            settingsBlock.getChildren().add(saveBtn);
        }

        VBox center = new VBox(32, title, settingsBlock);
        center.setAlignment(Pos.CENTER);
        root.setCenter(center);

        // ── Keybindings ───────────────────────────────────────────────────────
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) { onBack.run(); e.consume(); }
        });

        return root;
    }

    private static VBox toggleRow(String labelText, boolean initialOn,
                                   String styleClasses, Consumer<Boolean> onChange) {
        Label label = new Label(labelText);
        label.getStyleClass().add("settings-label");

        ToggleButton toggle = new ToggleButton(initialOn ? "ON" : "OFF");
        toggle.setSelected(initialOn);
        for (String cls : styleClasses.split(" ")) toggle.getStyleClass().add(cls);
        toggle.selectedProperty().addListener((obs, wasOn, isOn) -> {
            toggle.setText(isOn ? "ON" : "OFF");
            onChange.accept(isOn);
        });

        HBox row = new HBox(12, label, toggle);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, row);
    }

    private static VBox volumeBlock(String labelText, double initialValue,
                                     boolean initialMuted,
                                     DoubleConsumer onVolumeChange,
                                     Consumer<Boolean> onMuteChange) {
        Label label = new Label(labelText);
        label.getStyleClass().add("settings-label");

        Slider slider = new Slider(0.0, 100.0, initialValue);
        slider.getStyleClass().add("settings-slider");
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(25);
        HBox.setHgrow(slider, Priority.ALWAYS);

        ToggleButton muteToggle = new ToggleButton(initialMuted ? "MUTED" : "ON");
        muteToggle.setSelected(initialMuted);
        muteToggle.getStyleClass().addAll("settings-mute-button", "settings-mute-toggle");
        muteToggle.setMinWidth(90);
        muteToggle.setMaxWidth(90);

        // When the slider moves while NOT muted, push the real volume
        slider.valueProperty().addListener((obs, old, val) -> {
            if (!muteToggle.isSelected()) {
                onVolumeChange.accept(val.doubleValue() / 100.0);
            }
        });

        muteToggle.selectedProperty().addListener((obs, wasOn, isMuted) -> {
            muteToggle.setText(isMuted ? "MUTED" : "ON");
            onMuteChange.accept(isMuted);
            // Push effective volume: 0 when muted, slider value when unmuted
            onVolumeChange.accept(isMuted ? 0.0 : slider.getValue() / 100.0);
        });

        // Apply initial muted state to controller immediately
        if (initialMuted) {
            onVolumeChange.accept(0.0);
        }

        HBox sliderRow = new HBox(12, slider, muteToggle);
        sliderRow.setAlignment(Pos.CENTER_LEFT);

        return new VBox(6, label, sliderRow);
    }
}
