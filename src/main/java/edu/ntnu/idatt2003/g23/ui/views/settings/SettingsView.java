package edu.ntnu.idatt2003.g23.ui.views.settings;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

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

    public static BorderPane build(
            Runnable onBack,
            DoubleConsumer onMusicVolumeChange, double initialMusicVolume,
            DoubleConsumer onSfxVolumeChange,   double initialSfxVolume,
            Consumer<Boolean> onAnimationsChange, boolean animationsEnabled) {

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
        VBox musicBlock = volumeBlock("Music Volume", initialMusicVolume * 100.0, onMusicVolumeChange);

        // ── SFX Volume ────────────────────────────────────────────────────────
        VBox sfxBlock = volumeBlock("Sound Effects Volume", initialSfxVolume * 100.0, onSfxVolumeChange);

        // ── Animations ────────────────────────────────────────────────────────
        Label animLabel = new Label("Background Animations");
        animLabel.getStyleClass().add("settings-label");

        ToggleButton animToggle = new ToggleButton(animationsEnabled ? "ON" : "OFF");
        animToggle.setSelected(animationsEnabled);
        animToggle.getStyleClass().add("settings-mute-button");
        animToggle.selectedProperty().addListener((obs, wasOn, isOn) -> {
            animToggle.setText(isOn ? "ON" : "OFF");
            onAnimationsChange.accept(isOn);
        });

        HBox animRow = new HBox(12, animLabel, animToggle);
        animRow.setAlignment(Pos.CENTER_LEFT);

        // ── Dev Mode ──────────────────────────────────────────────────────────
        Label devLabel = new Label("Developer Mode");
        devLabel.getStyleClass().add("settings-label");

        ToggleButton devToggle = new ToggleButton(AppConfig.DEV_MODE.get() ? "ON" : "OFF");
        devToggle.setSelected(AppConfig.DEV_MODE.get());
        devToggle.getStyleClass().addAll("settings-mute-button", "settings-dev-button");
        devToggle.selectedProperty().addListener((obs, wasOn, isOn) -> {
            devToggle.setText(isOn ? "ON" : "OFF");
            AppConfig.DEV_MODE.set(isOn);
        });

        HBox devRow = new HBox(12, devLabel, devToggle);
        devRow.setAlignment(Pos.CENTER_LEFT);

        VBox settingsBlock = new VBox(28, musicBlock, sfxBlock, animRow, devRow);
        settingsBlock.setAlignment(Pos.CENTER_LEFT);
        settingsBlock.setMaxWidth(500);

        VBox center = new VBox(32, title, settingsBlock);
        center.setAlignment(Pos.CENTER);
        root.setCenter(center);

        return root;
    }

    private static VBox volumeBlock(String labelText, double initialValue, DoubleConsumer onChange) {
        Label label = new Label(labelText);
        label.getStyleClass().add("settings-label");

        double[] preMute = {initialValue};
        boolean[] muted  = {false};

        Slider slider = new Slider(0.0, 100.0, initialValue);
        slider.getStyleClass().add("settings-slider");
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(25);
        slider.valueProperty().addListener((obs, old, val) -> onChange.accept(val.doubleValue() / 100.0));
        HBox.setHgrow(slider, Priority.ALWAYS);

        Button muteBtn = new Button("Mute");
        muteBtn.getStyleClass().add("settings-mute-button");
        muteBtn.setMinWidth(90);
        muteBtn.setMaxWidth(90);
        muteBtn.setOnAction(e -> {
            if (muted[0]) {
                muted[0] = false;
                slider.setValue(preMute[0]);
                muteBtn.setText("Mute");
            } else {
                preMute[0] = slider.getValue();
                muted[0] = true;
                slider.setValue(0);
                muteBtn.setText("Unmute");
            }
        });

        HBox sliderRow = new HBox(12, slider, muteBtn);
        sliderRow.setAlignment(Pos.CENTER_LEFT);

        return new VBox(6, label, sliderRow);
    }
}
