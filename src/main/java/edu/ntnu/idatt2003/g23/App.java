package edu.ntnu.idatt2003.g23;

import edu.ntnu.idatt2003.g23.io.StockCsvLoader;
import edu.ntnu.idatt2003.g23.model.Stock;

import java.util.List;

public class App 
{
    public static void main( String[] args )
    {
        List<Stock> stocks = StockCsvLoader.loadFromResource("data/stocks/sp500_stocks.csv");

        System.out.println("Loaded " + stocks.size() + " stocks:");
        for (Stock stock : stocks) {
            System.out.println(
                    stock.getSymbol() + " - " + stock.getCompany() + " (latest: " + stock.getSalesPrice() + ")");
        }
    }
}
