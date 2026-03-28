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

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

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
        scene = new Scene(HomePageView.build(() -> {}), 1024, 768);
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
