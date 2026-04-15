package edu.ntnu.idatt2003.g23.ui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class HomePageView {
    public static BorderPane build(Runnable onPlay, Runnable onImportCsv, Runnable onSettings) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("home-page");

        // Title
        Label title = new Label("AFTER HOURS");
        title.getStyleClass().add("title");

        // Taglines
        Label tagline = new Label("\u2726  Lorem Ipsum \u2022 Lorem Ipsum \u2022 Lorem Ipsum  \u2726");
        tagline.getStyleClass().add("tagline");
        Label subTagline = new Label("Ipsum Dolor Sit Amet");
        subTagline.getStyleClass().add("sub-tagline");
        VBox taglineBlock = new VBox(6, tagline, subTagline);
        taglineBlock.setAlignment(Pos.CENTER);

        // Buttons
        Button startButton = new Button("\u25B6   Start Trading");
        startButton.getStyleClass().add("start-button");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setOnAction(e -> onPlay.run());

        Button importButton = new Button("\u2B06   Import CSV");
        importButton.getStyleClass().add("secondary-button");
        importButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(importButton, Priority.ALWAYS);
        importButton.setOnAction(e -> onImportCsv.run());

        Button settingsButton = new Button("\u2699   Settings");
        settingsButton.getStyleClass().add("secondary-button");
        settingsButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(settingsButton, Priority.ALWAYS);
        settingsButton.setOnAction(e -> onSettings.run());

        HBox bottomButtons = new HBox(14, importButton, settingsButton);
        bottomButtons.setAlignment(Pos.CENTER);

        VBox buttonBlock = new VBox(12, startButton, bottomButtons);
        buttonBlock.setAlignment(Pos.CENTER);

        VBox center = new VBox(0, title, taglineBlock, buttonBlock);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(680);
        center.setPadding(new Insets(40, 0, 40, 0));
        VBox.setMargin(taglineBlock, new Insets(10, 0, 0, 0));
        VBox.setMargin(buttonBlock,  new Insets(32, 0, 0, 0));

        root.setCenter(center);
        return root;
    }
}