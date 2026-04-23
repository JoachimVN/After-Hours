package edu.ntnu.idatt2003.g23.ui.views.importcsv;

import java.io.File;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public final class ImportCsvView {

  public static BorderPane build(Runnable onBack, Runnable onMakeOwn,
                                 Consumer<File> onEditCsv, Consumer<File> onContinue,
                                 File initialFile) {
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
      if (chosenFile[0] != null) {
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
    Label pageTitle = new Label("Import CSV");
    pageTitle.getStyleClass().add("page-title");

    // Open CSV editor with empty data
    makeOwnBtn.setOnAction(e -> onMakeOwn.run());

    // Open CSV editor with selected file
    editCsvBtn.setOnAction(e -> {
      if (chosenFile[0] != null) {
        onEditCsv.accept(chosenFile[0]);
      }
    });

    Label actionHint = new Label("Create a fresh dataset or open the selected CSV in the editor.");
    actionHint.getStyleClass().addAll("sub-tagline", "import-csv-action-hint");

    HBox editorBtnRow = new HBox(18, makeOwnBtn, editCsvBtn);
    editorBtnRow.getStyleClass().add("import-csv-action-row");
    editorBtnRow.setAlignment(Pos.CENTER);

    VBox editorSection = new VBox(8, actionHint, editorBtnRow);
    editorSection.setAlignment(Pos.CENTER);
    editorSection.setPadding(new Insets(12, 0, 0, 0));

    VBox page = new VBox(22, pageTitle, dropZone, continueBtn, reqPanel, editorSection);
    page.setAlignment(Pos.CENTER);
    page.setPadding(new Insets(0, 0, 40, 0));
    root.setCenter(page);

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
