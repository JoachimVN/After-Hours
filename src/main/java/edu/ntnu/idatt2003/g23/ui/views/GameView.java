package edu.ntnu.idatt2003.g23.ui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class GameView {

    public static BorderPane build(Runnable onBack) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("home-page");

        Button backButton = new Button("\u2190 Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> onBack.run());

        HBox topBar = new HBox(backButton);
        topBar.setPadding(new Insets(24, 32, 0, 32));
        root.setTop(topBar);

        Label title = new Label("Start Trading");
        title.getStyleClass().add("page-title");

        Label placeholder = new Label("Coming soon\u2026");
        placeholder.getStyleClass().add("sub-tagline");

        VBox center = new VBox(16, title, placeholder);
        center.setAlignment(Pos.CENTER);
        root.setCenter(center);

        return root;
    }
}
