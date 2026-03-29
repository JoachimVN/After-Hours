package edu.ntnu.idatt2003.g23;

import java.util.List;

import edu.ntnu.idatt2003.g23.io.StockCsvLoader;
import edu.ntnu.idatt2003.g23.model.Stock;

// public class App 
// {
//     public static void main( String[] args )
//     {
//         List<Stock> stocks = StockCsvLoader.loadFromResource("data/stocks/sp500_stocks.csv");

//         System.out.println("Loaded " + stocks.size() + " stocks:");
//         for (Stock stock : stocks) {
//             System.out.println(
//                     stock.getSymbol() + " - " + stock.getCompany() + " (latest: " + stock.getSalesPrice() + ")");
//         }
//     }
// }

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import edu.ntnu.idatt2003.g23.ui.HomePageView;
/**
 * Launches the  JavaFX application and manages top-level scene navigation.
 */
public class App extends Application {

    private Scene scene;

    /**
     * Starts the primary JavaFX stage and loads the home page.
     *
     * @param stage the primary application stage
     */
    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        root.getChildren().add(HomePageView.build(() -> {}));

        Rectangle splash = new Rectangle();
        splash.setFill(Color.BLACK);
        splash.widthProperty().bind(root.widthProperty());
        splash.heightProperty().bind(root.heightProperty());
        root.getChildren().add(splash);

        scene = new Scene(root, 1024, 768);
        scene.getStylesheets().add(getClass().getResource("/home.css").toExternalForm());

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        stage.setTitle("App");
        stage.setScene(scene);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMinWidth(860);
        stage.setMinHeight(620);
        stage.setMaximized(true);
        stage.show();

        FadeTransition fade = new FadeTransition(Duration.seconds(1.5), splash);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> root.getChildren().remove(splash));

        try {
            String musicPath = getClass().getResource("/sound/music/idatt2003_sound_test2.mp3").toExternalForm();
            MediaPlayer mediaPlayer = new MediaPlayer(new Media(musicPath));
            mediaPlayer.setOnPlaying(() -> {
                fade.setDelay(Duration.seconds(2.0));
                fade.play();
            });
            mediaPlayer.play();
        } catch (Exception e) {
            // fallback: just fade if music fails to load
            fade.setDelay(Duration.seconds(0.3));
            fade.play();
        }
    }


    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
