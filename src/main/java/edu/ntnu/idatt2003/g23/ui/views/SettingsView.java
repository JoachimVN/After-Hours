package edu.ntnu.idatt2003.g23.ui.views;

import java.util.function.DoubleConsumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class SettingsView {

    public static BorderPane build(Runnable onBack, DoubleConsumer onVolumeChange, double initialVolume) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("home-page");

        Button backButton = new Button("\u2190 Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> onBack.run());

        HBox topBar = new HBox(backButton);
        topBar.setPadding(new Insets(24, 32, 0, 32));
        root.setTop(topBar);

        Label title = new Label("Settings");
        title.getStyleClass().add("page-title");

        Label volumeLabel = new Label("Music Volume");
        volumeLabel.getStyleClass().add("settings-label");

        Slider volumeSlider = new Slider(0.0, 1.0, initialVolume);
        volumeSlider.getStyleClass().add("settings-slider");
        volumeSlider.setMaxWidth(400);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setMajorTickUnit(0.25);
        volumeSlider.valueProperty().addListener((obs, old, val) -> onVolumeChange.accept(val.doubleValue()));

        VBox settingsBlock = new VBox(10, volumeLabel, volumeSlider);
        settingsBlock.setAlignment(Pos.CENTER_LEFT);
        settingsBlock.setMaxWidth(400);

        VBox center = new VBox(32, title, settingsBlock);
        center.setAlignment(Pos.CENTER);
        root.setCenter(center);

        return root;
    }
}
