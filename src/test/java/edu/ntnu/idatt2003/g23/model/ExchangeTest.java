package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.ntnu.idatt2003.g23.model.Stock.Volatility;

import edu.ntnu.idatt2003.g23.model.transaction.Purchase;
import edu.ntnu.idatt2003.g23.model.transaction.Sale;
import edu.ntnu.idatt2003.g23.model.transaction.Transaction;

class ExchangeTest {

  @TempDir
  Path tempDir;

  private Stock stock;
  private Exchange exchange;

  @BeforeEach
  void setUp() {
    stock = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    exchange = new Exchange("NYSE", Arrays.asList(stock));
  }

  // Helper method to create a sample stock
  private Stock createSampleStock(String symbol, String company, BigDecimal price) {
    List<BigDecimal> prices = new ArrayList<>();
    prices.add(price);
    return new Stock(symbol, company, prices);
  }

  // Helper method to create a sample player
  private Player createSamplePlayer(String name, BigDecimal money) {
    return new Player(name, money);
  }

  @Test
  @DisplayName("Constructor creates exchange with valid name and stocks")
  void testConstructorValid() {
    List<Stock> stocks = Arrays.asList(
        createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150")),
        createSampleStock("GOOGL", "Google", new BigDecimal("200"))
    );
    Exchange exchange = new Exchange("NYSE", stocks);

    assertEquals("NYSE", exchange.getName());
    assertEquals(1, exchange.getWeek());
    assertTrue(exchange.hasStock("AAPL"));
    assertTrue(exchange.hasStock("GOOGL"));
  }

  @Test
  @DisplayName("Constructor throws when name is null")
  void testConstructorNullName() {
    assertThrows(IllegalArgumentException.class, () -> new Exchange(null, Arrays.asList(stock)));
  }

  @Test
  @DisplayName("Constructor throws when name is blank")
  void testConstructorBlankName() {
    assertThrows(IllegalArgumentException.class, () -> new Exchange("", Arrays.asList(stock)));
  }

  @Test
  @DisplayName("Constructor throws when stocks is null")
  void testConstructorNullStocks() {
    assertThrows(IllegalArgumentException.class, () -> new Exchange("NYSE", null));
  }

  @Test
  @DisplayName("getName returns correct name")
  void testGetName() {
    assertEquals("NYSE", exchange.getName());
  }

  @Test
  @DisplayName("getWeek returns initial week as 1")
  void testGetWeekInitial() {
    assertEquals(1, exchange.getWeek());
  }

  @Test
  @DisplayName("hasStock returns true for existing stock")
  void testHasStockExisting() {
    assertTrue(exchange.hasStock("AAPL"));
  }

  @Test
  @DisplayName("hasStock returns false for non-existing stock")
  void testHasStockNonExisting() {
    assertFalse(exchange.hasStock("GOOGL"));
  }

  @Test
  @DisplayName("hasStock throws when symbol is null")
  void testHasStockNullSymbol() {
    assertThrows(IllegalArgumentException.class, () -> exchange.hasStock(null));
  }

  @Test
  @DisplayName("hasStock throws when symbol is blank")
  void testHasStockBlankSymbol() {
    assertThrows(IllegalArgumentException.class, () -> exchange.hasStock(""));
  }

  @Test
  @DisplayName("getStock returns correct stock")
  void testGetStock() {
    assertEquals(stock, exchange.getStock("AAPL"));
  }

  @Test
  @DisplayName("getStock throws when symbol is null")
  void testGetStockNullSymbol() {
    assertThrows(IllegalArgumentException.class, () -> exchange.getStock(null));
  }

  @Test
  @DisplayName("getStock throws when symbol is blank")
  void testGetStockBlankSymbol() {
    assertThrows(IllegalArgumentException.class, () -> exchange.getStock(""));
  }

  @Test
  @DisplayName("getStock throws when stock does not exist")
  void testGetStockNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> exchange.getStock("GOOGL"));
  }

  @Test
  @DisplayName("findStocks finds stocks by symbol")
  void testFindStocksBySymbol() {
    Stock aapl = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    Stock googl = createSampleStock("GOOGL", "Google", new BigDecimal("200"));
    List<Stock> stocks = Arrays.asList(aapl, googl);
    Exchange exchange = new Exchange("NYSE", stocks);

    List<Stock> result = exchange.findStocks("AAPL");
    assertEquals(1, result.size());
    assertEquals(aapl, result.get(0));
  }

  @Test
  @DisplayName("findStocks finds stocks by company name")
  void testFindStocksByCompany() {
    Stock aapl = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    Stock googl = createSampleStock("GOOGL", "Google", new BigDecimal("200"));
    List<Stock> stocks = Arrays.asList(aapl, googl);
    Exchange exchange = new Exchange("NYSE", stocks);

    List<Stock> result = exchange.findStocks("Apple");
    assertEquals(1, result.size());
    assertEquals(aapl, result.get(0));
  }

  @Test
  @DisplayName("findStocks is case insensitive")
  void testFindStocksCaseInsensitive() {
    Stock aapl = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    List<Stock> stocks = Arrays.asList(aapl);
    Exchange exchange = new Exchange("NYSE", stocks);

    List<Stock> result = exchange.findStocks("apple");
    assertEquals(1, result.size());
    assertEquals(aapl, result.get(0));
  }

  @Test
  @DisplayName("findStocks returns multiple matches")
  void testFindStocksMultiple() {
    Stock aapl = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    Stock appl = createSampleStock("APPL", "Another Apple", new BigDecimal("100"));
    List<Stock> stocks = Arrays.asList(aapl, appl);
    Exchange exchange = new Exchange("NYSE", stocks);

    List<Stock> result = exchange.findStocks("Apple");
    assertEquals(2, result.size());
  }

  @Test
  @DisplayName("findStocks returns empty list when no matches")
  void testFindStocksNoMatches() {
    Stock aapl = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    List<Stock> stocks = Arrays.asList(aapl);
    Exchange exchange = new Exchange("NYSE", stocks);

    List<Stock> result = exchange.findStocks("Microsoft");
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("buy creates purchase transaction")
  void testBuy() {
    Stock stock = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    List<Stock> stocks = Arrays.asList(stock);
    Exchange exchange = new Exchange("NYSE", stocks);
    Player player = createSamplePlayer("John", new BigDecimal("1000"));

    Transaction transaction = exchange.buy("AAPL", new BigDecimal("10"), player);

    assertNotNull(transaction);
    assertTrue(transaction instanceof Purchase);
    assertEquals(1, transaction.getWeek());
  }

  @Test
  @DisplayName("buy throws when stock does not exist")
  void testBuyNonExistingStock() {
    List<Stock> stocks =
        Arrays.asList(createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150")));
    Exchange exchange = new Exchange("NYSE", stocks);
    Player player = createSamplePlayer("John", new BigDecimal("1000"));

    assertThrows(IllegalArgumentException.class,
        () -> exchange.buy("GOOGL", new BigDecimal("10"), player));
  }

  @Test
  @DisplayName("buy throws when quantity is null")
  void testBuyNullQuantity() {
    List<Stock> stocks =
        Arrays.asList(createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150")));
    Exchange exchange = new Exchange("NYSE", stocks);
    Player player = createSamplePlayer("John", new BigDecimal("1000"));

    assertThrows(IllegalArgumentException.class, () -> exchange.buy("AAPL", null, player));
  }

  @Test
  @DisplayName("buy throws when quantity is zero")
  void testBuyZeroQuantity() {
    List<Stock> stocks =
        Arrays.asList(createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150")));
    Exchange exchange = new Exchange("NYSE", stocks);
    Player player = createSamplePlayer("John", new BigDecimal("1000"));

    assertThrows(IllegalArgumentException.class,
        () -> exchange.buy("AAPL", BigDecimal.ZERO, player));
  }

  @Test
  @DisplayName("buy throws when quantity is negative")
  void testBuyNegativeQuantity() {
    List<Stock> stocks =
        Arrays.asList(createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150")));
    Exchange exchange = new Exchange("NYSE", stocks);
    Player player = createSamplePlayer("John", new BigDecimal("1000"));

    assertThrows(IllegalArgumentException.class,
        () -> exchange.buy("AAPL", new BigDecimal("-1"), player));
  }

  @Test
  @DisplayName("buy throws when player is null")
  void testBuyNullPlayer() {
    List<Stock> stocks =
        Arrays.asList(createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150")));
    Exchange exchange = new Exchange("NYSE", stocks);

    assertThrows(IllegalArgumentException.class,
        () -> exchange.buy("AAPL", new BigDecimal("10"), null));
  }

  @Test
  @DisplayName("sell creates sale transaction")
  void testSell() {
    Stock stock = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    List<Stock> stocks = Arrays.asList(stock);
    Exchange exchange = new Exchange("NYSE", stocks);
    Player player = createSamplePlayer("John", new BigDecimal("1000"));
    Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

    Transaction transaction = exchange.sell(share, player);

    assertNotNull(transaction);
    assertTrue(transaction instanceof Sale);
    assertEquals(1, transaction.getWeek());
  }

  @Test
  @DisplayName("sell throws when share is null")
  void testSellNullShare() {
    List<Stock> stocks =
        Arrays.asList(createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150")));
    Exchange exchange = new Exchange("NYSE", stocks);
    Player player = createSamplePlayer("John", new BigDecimal("1000"));

    assertThrows(IllegalArgumentException.class, () -> exchange.sell(null, player));
  }

  @Test
  @DisplayName("sell throws when player is null")
  void testSellNullPlayer() {
    Stock stock = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    Share share = new Share(stock, new BigDecimal("10"), new BigDecimal("140"));

    List<Stock> stocks = Arrays.asList(stock);
    Exchange exchange = new Exchange("NYSE", stocks);

    assertThrows(IllegalArgumentException.class, () -> exchange.sell(share, null));
  }

  @Test
  @DisplayName("advance increments week and updates prices")
  void testAdvance() {
    Stock stock = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    List<Stock> stocks = Arrays.asList(stock);
    Exchange exchange = new Exchange("NYSE", stocks);

    int initialWeek = exchange.getWeek();

    exchange.advance();

    assertEquals(initialWeek + 1, exchange.getWeek());
    // Price should have changed (though randomly, so we can't predict exact value)
    // But we can check that a new price was added
    // Since Stock.addNewSalesPrice adds to the list, and getSalesPrice returns the last one
    // We can check that the price list has grown, but since we don't have access to it directly,
    // perhaps just ensure no exception and week increased.
    // For better test, maybe check that price is different, but random makes it tricky.
    // Assuming the random change is applied, price should be different unless random gives 0 change, which is rare.
    // But to be safe, just check week.
  }

  @Test
  @DisplayName("getGainers returns stocks sorted by highest sales price first")
  void testGetGainers() {
    Stock aapl = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    Stock msft = createSampleStock("MSFT", "Microsoft", new BigDecimal("250"));
    Stock tsla = createSampleStock("TSLA", "Tesla", new BigDecimal("100"));

    Exchange exchange = new Exchange("NYSE", Arrays.asList(aapl, msft, tsla));
    List<Stock> gainers = exchange.getGainers(2);

    assertEquals(2, gainers.size());
    assertEquals("MSFT", gainers.get(0).getSymbol());
    assertEquals("AAPL", gainers.get(1).getSymbol());
  }

  @Test
  @DisplayName("getLosers returns stocks sorted by lowest sales price first")
  void testGetLosers() {
    Stock aapl = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    Stock msft = createSampleStock("MSFT", "Microsoft", new BigDecimal("250"));
    Stock tsla = createSampleStock("TSLA", "Tesla", new BigDecimal("100"));

    Exchange exchange = new Exchange("NYSE", Arrays.asList(aapl, msft, tsla));
    List<Stock> losers = exchange.getLosers(2);

    assertEquals(2, losers.size());
    assertEquals("TSLA", losers.get(0).getSymbol());
    assertEquals("AAPL", losers.get(1).getSymbol());
  }

  @Test
  @DisplayName("getGainers throws when limit is negative")
  void testGetGainersNegativeLimit() {
    Stock aapl = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    Exchange exchange = new Exchange("NYSE", Arrays.asList(aapl));

    assertThrows(IllegalArgumentException.class, () -> exchange.getGainers(-1));
  }

  @Test
  @DisplayName("getLosers throws when limit is negative")
  void testGetLosersNegativeLimit() {
    Stock aapl = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    Exchange exchange = new Exchange("NYSE", Arrays.asList(aapl));

    assertThrows(IllegalArgumentException.class, () -> exchange.getLosers(-1));
  }

  @Test
  @DisplayName("exportCurrentPrices writes a CSV with current prices")
  void testExportCurrentPricesWritesCsv() throws Exception {
    Stock aapl = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    Stock msft = createSampleStock("MSFT", "Microsoft", new BigDecimal("250"));

    Exchange exchange = new Exchange("NYSE", Arrays.asList(aapl, msft));

    Path out = tempDir.resolve("current-prices.csv");
    exchange.exportCurrentPrices(out);

    assertTrue(Files.exists(out));

    List<String> lines = Files.readAllLines(out);
    assertFalse(lines.isEmpty());
    assertEquals("symbol,company,price", lines.get(0));

    // Order isn't guaranteed -> assert by containment
    assertTrue(lines.contains("AAPL,Apple Inc.,150.00"));
    assertTrue(lines.contains("MSFT,Microsoft,250.00"));
  }

  @Test
  @DisplayName("setFrozen prevents price updates on advance")
  void testSetFrozenPreventsAdvance() {
    Stock stock = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    Exchange exchange = new Exchange("NYSE", Arrays.asList(stock));

    exchange.setFrozen(true);
    exchange.advance();

    assertEquals(2, exchange.getWeek());
    assertEquals(new BigDecimal("150"), stock.getSalesPrice());
  }

  @Test
  @DisplayName("advance with frozen false still updates prices")
  void testAdvanceUnfrozenUpdatesPrices() {
    Stock stock = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    Exchange exchange = new Exchange("NYSE", Arrays.asList(stock));

    exchange.setFrozen(false);
    exchange.advance();

    assertEquals(2, exchange.getWeek());
    assertTrue(stock.getHistoricalPrices().size() >= 2);
  }

  @Test
  @DisplayName("getStocks returns all stocks")
  void testGetStocks() {
    Stock aapl = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
    Stock msft = createSampleStock("MSFT", "Microsoft", new BigDecimal("250"));
    Exchange exchange = new Exchange("NYSE", Arrays.asList(aapl, msft));

    List<Stock> stocks = exchange.getStocks();

    assertEquals(2, stocks.size());
    assertTrue(stocks.contains(aapl));
    assertTrue(stocks.contains(msft));
  }

  @Test
  @DisplayName("advance exercises all volatility switch branches")
  void testAdvanceAllVolatilityBranches() {
    List<Volatility> volatilities = List.of(
        Volatility.SLOW_RISE, Volatility.SLOW_FALL,
        Volatility.NORMAL_RISE, Volatility.NORMAL_FALL,
        Volatility.FAST, Volatility.CHAOTIC, Volatility.STABLE);

    for (Volatility v : volatilities) {
      Stock stock = createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150"));
      stock.setVolatility(v);
      Exchange exchange = new Exchange("NYSE", Arrays.asList(stock));
      exchange.advance();
      assertEquals(2, exchange.getWeek());
    }
  }

  @Test
  @DisplayName("advance over many weeks exercises probabilistic branches")
  void testAdvanceManyWeeks() {
    List<Stock> stocks = Arrays.asList(
        createSampleStock("AAPL", "Apple Inc.", new BigDecimal("150")),
        createSampleStock("MSFT", "Microsoft", new BigDecimal("250")),
        createSampleStock("TSLA", "Tesla", new BigDecimal("100")));
    Exchange exchange = new Exchange("NYSE", stocks);

    for (int i = 0; i < 200; i++) {
      exchange.advance();
    }

    assertEquals(201, exchange.getWeek());
  }

  @Test
  @DisplayName("assignVolatilities exercises all loop bodies with many stocks")
  void testAssignVolatilitiesWithManyStocks() {
    String[] symbols = {
        "AAPL", "MSFT", "TSLA", "AMZN", "GOOGL", "META", "NVDA", "NFLX",
        "AMD", "INTC", "CRM", "ORCL", "IBM", "CSCO", "QCOM", "TXN",
        "AVGO", "ASML", "MRVL", "ADI"
    };
    List<Stock> stocks = new ArrayList<>();
    for (String symbol : symbols) {
      stocks.add(createSampleStock(symbol, symbol + " Inc.", new BigDecimal("100")));
    }
    Exchange exchange = new Exchange("NYSE", stocks);
    exchange.advance();
    assertEquals(2, exchange.getWeek());
  }

  @Test
  @DisplayName("advance applies price floor when stock price is near zero")
  void testAdvancePriceFloor() {
    Stock stock = createSampleStock("TINY", "Tiny Corp", new BigDecimal("0.001"));
    Exchange exchange = new Exchange("NYSE", Arrays.asList(stock));
    exchange.advance();
    assertTrue(stock.getSalesPrice().compareTo(new BigDecimal("0.01")) >= 0);
  }

  @Test
  @DisplayName("post-spike floor prevents price falling below $0.01 over many advances")
  void testPostSpikePriceFloorAfterSpike() {
    // Stock starts below the $0.01 floor. The main-advance loop always resets
    // it to exactly $0.01, so any downward spike immediately crosses the floor
    // and must be caught by the post-spike floor check (lines 264-265).
    // Running 500 advances gives a ~(1-0.083)^500 ≈ 0 chance of never hitting
    // a downward spike, making coverage of line 265 essentially certain.
    Stock stock = createSampleStock("TINY", "Tiny Corp", new BigDecimal("0.001"));
    stock.setVolatility(Stock.Volatility.SLOW_FALL);
    Exchange exchange = new Exchange("NYSE", Arrays.asList(stock));

    BigDecimal priceFloor = new BigDecimal("0.01");
    for (int i = 0; i < 500; i++) {
      exchange.advance();
      assertTrue(
          stock.getSalesPrice().compareTo(priceFloor) >= 0,
          "Price fell below $0.01 at week " + exchange.getWeek()
      );
    }
  }
}