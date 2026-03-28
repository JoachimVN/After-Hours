package edu.ntnu.idatt2003.g23.ui;

import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

public final class HomePageView {
    public static BorderPane build(Runnable onProfileClick) {
        BorderPane root = new BorderPane();
        
        Label title = new Label("App");
        title.getStyleClass().add("page-title");
        
        VBox center = new VBox();
        center.setAlignment(Pos.CENTER);
        center.getChildren().add(title);
        
        root.setCenter(center);
        return root;
    }
}
