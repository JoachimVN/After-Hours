package edu.ntnu.idatt2003.g23.ui.util;
import javafx.scene.control.Label;

public class LabelUtil {
    public static Label labelSmall(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("game-stat-key");
        return l;
    }
}
