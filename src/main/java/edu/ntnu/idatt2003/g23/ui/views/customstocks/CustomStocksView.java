package edu.ntnu.idatt2003.g23.ui.views.customstocks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import edu.ntnu.idatt2003.g23.AppConfig;
import edu.ntnu.idatt2003.g23.io.GameSaveLoader;
import edu.ntnu.idatt2003.g23.io.GameSaveLoader.SaveMeta;
import edu.ntnu.idatt2003.g23.model.MarketOption;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public final class CustomStocksView {

  public static BorderPane build(Runnable onBack, Runnable onMakeOwn,
                                 Consumer<File> onEditCsv, Consumer<File> onContinue,
                                 Consumer<String> onEditBuiltInMarket,
                                 Consumer<SaveMeta> onEditSaveFile,
                                 Path currentSavePath,
                                 File initialFile) {
    boolean editorOnlyMode = onContinue == null;
    boolean hasBuiltInMarkets = onEditBuiltInMarket != null && !AppConfig.BUILT_IN_MARKETS.isEmpty();
    List<SaveMeta> availableSaves = List.of();
    if (onEditSaveFile != null) {
      try {
        availableSaves = GameSaveLoader.listSaves();
      } catch (IOException ignored) {
        availableSaves = List.of();
      }
    }
    boolean hasSaveFiles = onEditSaveFile != null && !availableSaves.isEmpty();

    BorderPane root = new BorderPane();
    root.getStyleClass().addAll("home-page", "background-overlay");

    // ── Top bar ──────────────────────────────────────────────────────────
    Button backButton = new Button("\u2190 Back");
    backButton.getStyleClass().add("back-button");
    backButton.setOnAction(e -> onBack.run());

    HBox topBar = new HBox(backButton);
    topBar.setPadding(new Insets(24, 32, 0, 32));
    root.setTop(topBar);

    // ── Drop zone ─────────────────────────────────────────────────────────
    Label dropIcon = new Label("\u2601"); // ☁
    dropIcon.setStyle("-fx-font-size: 52; -fx-text-fill: #4a6899;");

    Label dropLabel = new Label("Drop your CSV file here");
    dropLabel.getStyleClass().add("drop-zone-label");

    Label orLabel = new Label("\u2014  or  \u2014");
    orLabel.getStyleClass().add("sub-tagline");

    Button browseBtn = new Button("Browse Files");
    browseBtn.getStyleClass().add("secondary-button");
    browseBtn.setStyle("-fx-pref-height: 38; -fx-font-size: 13;");

    Label fileNameLabel = new Label("");
    fileNameLabel.getStyleClass().add("sub-tagline");
    fileNameLabel.setStyle("-fx-text-fill: #f5a201;");

    VBox dropZone = new VBox(12, dropIcon, dropLabel, orLabel, browseBtn, fileNameLabel);
    dropZone.getStyleClass().add("drop-zone");
    dropZone.setAlignment(Pos.CENTER);
    dropZone.setMaxWidth(580);
    dropZone.setPrefHeight(200);

    final File[] chosenFile = {null};

    Button makeOwnBtn = new Button("Make Your Own Stock Data");
    makeOwnBtn.getStyleClass().addAll("secondary-button", "import-csv-action-button");

    Button editCsvBtn = new Button("Edit Stock Data");
    editCsvBtn.getStyleClass().addAll("secondary-button", "import-csv-action-button");
    editCsvBtn.setDisable(true);

    Button continueBtn = new Button("\u25B6   Start with this data");
    continueBtn.getStyleClass().addAll("start-button", "import-csv-action-button");
    continueBtn.setMaxWidth(580);
    continueBtn.setStyle("-fx-pref-height: 58; -fx-font-size: 20;");
    continueBtn.setDisable(true);
    continueBtn.setVisible(!editorOnlyMode);
    continueBtn.setManaged(!editorOnlyMode);

    Runnable clearSelection = () -> {
      chosenFile[0] = null;
      fileNameLabel.setText("");
      fileNameLabel.setStyle("-fx-text-fill: #f5a201;");
      editCsvBtn.setDisable(true);
      continueBtn.setDisable(true);
    };

    Consumer<File> selectFile = file -> {
      if (file == null) {
        clearSelection.run();
        return;
      }
      if (!file.getName().toLowerCase().endsWith(".csv")) {
        chosenFile[0] = null;
        fileNameLabel.setText("\u2716  Please select a .csv file");
        fileNameLabel.setStyle("-fx-text-fill: #cc4444;");
        editCsvBtn.setDisable(true);
        continueBtn.setDisable(true);
        return;
      }
      chosenFile[0] = file;
      fileNameLabel.setText("\u2714  " + file.getName());
      fileNameLabel.setStyle("-fx-text-fill: #f5a201;");
      editCsvBtn.setDisable(false);
      continueBtn.setDisable(false);
    };

    // File chooser (Browse button)
    browseBtn.setOnAction(e -> {
      FileChooser fc = new FileChooser();
      fc.setTitle("Select CSV File");
      fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
      File f = fc.showOpenDialog(root.getScene().getWindow());
      if (f != null) {
        selectFile.accept(f);
      }
    });

    // Drag-and-drop
    dropZone.setOnDragOver(e -> {
      if (e.getDragboard().hasFiles()) {
        e.acceptTransferModes(TransferMode.COPY);
        dropZone.getStyleClass().remove("drag-over");
        dropZone.getStyleClass().add("drag-over");
      }
      e.consume();
    });
    dropZone.setOnDragExited(e -> dropZone.getStyleClass().remove("drag-over"));
    dropZone.setOnDragDropped(e -> {
      var db = e.getDragboard();
      dropZone.getStyleClass().remove("drag-over");
      if (db.hasFiles()) {
        File f = db.getFiles().get(0);
        selectFile.accept(f);
      }
      e.setDropCompleted(true);
      e.consume();
    });

    continueBtn.setOnAction(e -> {
      if (chosenFile[0] != null && onContinue != null) {
        onContinue.accept(chosenFile[0]);
      }
    });

    // ── Keybindings ───────────────────────────────────────────────────────
    // Escape → back to setup; Enter/Space (once file loaded) → start
    root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      switch (e.getCode()) {
        case ESCAPE -> {
          onBack.run();
          e.consume();
        }
        case ENTER, SPACE -> {
          if (!continueBtn.isDisable()) {
            continueBtn.fire();
            e.consume();
          }
        }
        default -> {
        }
      }
    });

    // ── CSV Format Requirements ───────────────────────────────────────────
    Label reqTitle = new Label("CSV Format Requirements");
    reqTitle.getStyleClass().add("setup-field-label");

    Label reqDesc = new Label("Your CSV file must include these columns:");
    reqDesc.setStyle("-fx-font-size: 12; -fx-text-fill: #4a6899;");

    VBox reqPanel = new VBox(10,
        reqTitle,
        reqDesc,
        buildReqRow("symbol", "Stock ticker symbol (e.g., AAPL)"),
        buildReqRow("name", "Company name (e.g., Apple Inc.)"),
        buildReqRow("price", "Current stock price (e.g., 150.25)"),
        buildExample());
    reqPanel.getStyleClass().add("csv-req-panel");
    reqPanel.setMaxWidth(580);
    reqPanel.setPadding(new Insets(20));

    // ── Page layout ───────────────────────────────────────────────────────
    Label pageTitle = new Label("Custom Stock Data");
    if (editorOnlyMode) {
      pageTitle.setText("Edit Stock Data");
    }
    pageTitle.getStyleClass().add("page-title");

    // Open CSV editor with empty data
    makeOwnBtn.setOnAction(e -> onMakeOwn.run());

    // Open CSV editor with selected file
    editCsvBtn.setOnAction(e -> {
      if (chosenFile[0] != null) {
        onEditCsv.accept(chosenFile[0]);
      }
    });

    Label actionHint = new Label(editorOnlyMode
        ? "Create a dataset, edit the current market, or open stock data in the editor."
        : "Create a fresh dataset or open the selected CSV in the editor.");
    actionHint.getStyleClass().addAll("sub-tagline", "import-csv-action-hint");

    HBox editorBtnRow = new HBox(18, makeOwnBtn, editCsvBtn);
    editorBtnRow.getStyleClass().add("import-csv-action-row");
    editorBtnRow.setAlignment(Pos.CENTER);

    VBox editorSection = new VBox(8, actionHint, editorBtnRow);
    editorSection.setAlignment(Pos.CENTER);
    editorSection.setPadding(new Insets(12, 0, 0, 0));

    VBox saveSection = null;
    if (hasSaveFiles) {
      Label saveTitle = new Label("Open stock data from a save");
      saveTitle.getStyleClass().add("setup-field-label");

      Label saveHint = new Label(
          "Pick a save and load its saved stock history into the CSV editor.");
      saveHint.getStyleClass().addAll("sub-tagline", "import-csv-action-hint");

      ComboBox<SaveMeta> saveCombo = new ComboBox<>();
      saveCombo.setPromptText("Select a save…");
      saveCombo.getStyleClass().addAll("market-combo-box", "import-csv-market-picker");
      saveCombo.setMaxWidth(Double.MAX_VALUE);
      saveCombo.setCellFactory(lv -> saveMetaCell(currentSavePath));
      saveCombo.setButtonCell(saveMetaCell(currentSavePath));
      saveCombo.getItems().setAll(availableSaves);

      Button openSaveBtn = new Button("\uD83D\uDCBE  Open Save Data");
      openSaveBtn.getStyleClass().addAll("secondary-button", "import-csv-action-button");
      openSaveBtn.disableProperty().bind(saveCombo.getSelectionModel().selectedItemProperty().isNull());
      openSaveBtn.setOnAction(e -> {
        SaveMeta selected = saveCombo.getSelectionModel().getSelectedItem();
        if (selected != null) {
          onEditSaveFile.accept(selected);
        }
      });

      HBox saveControls = new HBox(14, saveCombo, openSaveBtn);
      saveControls.setAlignment(Pos.CENTER_LEFT);
      HBox.setHgrow(saveCombo, Priority.ALWAYS);

      saveSection = new VBox(10, saveTitle, saveHint, saveControls);
      saveSection.getStyleClass().add("import-csv-built-in-panel");
      saveSection.setAlignment(Pos.CENTER_LEFT);
      saveSection.setMaxWidth(580);
      saveSection.setPadding(new Insets(18));
    }

    // ── Built-in Markets ──────────────────────────────────────────────────
    VBox builtInSection = null;
    if (hasBuiltInMarkets) {
      Label builtInTitle = new Label("Edit a built-in market");
      builtInTitle.getStyleClass().add("setup-field-label");

      Label builtInHint = new Label("Pick one of the default markets and open it in the CSV editor.");
      builtInHint.getStyleClass().addAll("sub-tagline", "import-csv-action-hint");

      ComboBox<MarketOption> builtInMarketBox = new ComboBox<>();
      builtInMarketBox.getItems().addAll(AppConfig.BUILT_IN_MARKETS);
      builtInMarketBox.getStyleClass().addAll("market-combo-box", "import-csv-market-picker");
      builtInMarketBox.setPromptText("Choose a built-in market");
      builtInMarketBox.setMaxWidth(Double.MAX_VALUE);
      builtInMarketBox.setButtonCell(marketCell());
      builtInMarketBox.setCellFactory(listView -> marketCell());

      Button editBuiltInBtn = new Button("\u270e  Open in Editor");
      editBuiltInBtn.getStyleClass().addAll("secondary-button", "import-csv-built-in-open-button");
      editBuiltInBtn.setDisable(true);

      builtInMarketBox.valueProperty().addListener((obs, oldValue, newValue) -> {
        editBuiltInBtn.setDisable(newValue == null);
      });

      editBuiltInBtn.setOnAction(e -> {
        MarketOption selectedMarket = builtInMarketBox.getValue();
        if (selectedMarket != null) {
          onEditBuiltInMarket.accept(selectedMarket.csvResource());
        }
      });

      HBox builtInControls = new HBox(14, builtInMarketBox, editBuiltInBtn);
      builtInControls.setAlignment(Pos.CENTER_LEFT);
      HBox.setHgrow(builtInMarketBox, Priority.ALWAYS);

      builtInSection = new VBox(10, builtInTitle, builtInHint, builtInControls);
      builtInSection.getStyleClass().add("import-csv-built-in-panel");
      builtInSection.setAlignment(Pos.CENTER_LEFT);
      builtInSection.setMaxWidth(580);
      builtInSection.setPadding(new Insets(18));
    }

    VBox page = new VBox(22, pageTitle, dropZone, continueBtn, reqPanel, editorSection);
    if (builtInSection != null) {
      page.getChildren().add(builtInSection);
    }
    if (saveSection != null) {
      page.getChildren().add(saveSection);
    }
    page.setAlignment(Pos.CENTER);
    page.setMaxWidth(700);
    page.setPadding(new Insets(0, 0, 40, 0));

    HBox centeringBox = new HBox(page);
    centeringBox.setAlignment(Pos.TOP_CENTER);
    centeringBox.setPadding(new Insets(0, 32, 40, 32));

    ScrollPane scroll = new ScrollPane(centeringBox);
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(false);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.getStyleClass().add("import-csv-scroll");

    root.setCenter(scroll);

    selectFile.accept(initialFile);

    return root;
  }

  private static HBox buildReqRow(String col, String desc) {
    Label colLabel = new Label(col);
    colLabel.setStyle(
        "-fx-text-fill: #f5a201; -fx-font-weight: bold; -fx-font-size: 13; -fx-min-width: 70;");
    Label descLabel = new Label("\u2013  " + desc);
    descLabel.setStyle("-fx-text-fill: #b8cce8; -fx-font-size: 13;");
    HBox row = new HBox(12, colLabel, descLabel);
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }

  private static ListCell<MarketOption> marketCell() {
    return new ListCell<>() {
      @Override
      protected void updateItem(MarketOption market, boolean empty) {
        super.updateItem(market, empty);
        if (empty || market == null) {
          setGraphic(null);
          setText(null);
        } else {
          Label name = new Label(market.name());
          name.getStyleClass().add("market-combo-name");
          Label desc = new Label(market.description());
          desc.getStyleClass().add("market-combo-desc");
          setGraphic(new VBox(2, name, desc));
          setText(null);
        }
      }
    };
  }

  private static ListCell<SaveMeta> saveMetaCell(Path currentSavePath) {
    return new ListCell<>() {
      @Override
      protected void updateItem(SaveMeta item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setStyle("");
        } else {
          boolean isCurrent = currentSavePath != null
              && item.saveDir() != null
              && item.saveDir().equals(currentSavePath);
          String prefix = isCurrent ? "▶  " : (item.autosave() ? "⌛ " : "");
          setText(prefix + item.displayName()
              + "  •  Week " + item.week()
              + "  •  " + item.savedAt()
              + (isCurrent ? "  — Playing" : ""));
          setStyle(isCurrent ? "-fx-text-fill: #f5a201; -fx-font-weight: bold;" : "");
        }
      }
    };
  }

  private static VBox buildExample() {
    Label ex = new Label(
        "symbol,name,price\n"
            + "AAPL,Apple Inc.,150.25\n"
            + "GOOGL,Alphabet Inc.,2800.50\n"
            + "MSFT,Microsoft Corp.,310.75");
    ex.setStyle(
        "-fx-font-family: monospace; -fx-font-size: 12;"
            + "-fx-text-fill: #7a9aaa; -fx-padding: 10 14 10 14;"
            + "-fx-background-color: #050b18; -fx-background-radius: 6;");
    VBox box = new VBox(ex);
    box.setPadding(new Insets(6, 0, 0, 0));
    return box;
  }
}
