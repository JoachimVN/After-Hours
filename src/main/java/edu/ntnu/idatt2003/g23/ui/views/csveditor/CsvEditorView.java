package edu.ntnu.idatt2003.g23.ui.views.csveditor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import edu.ntnu.idatt2003.g23.io.CsvParseResult;
import edu.ntnu.idatt2003.g23.io.CsvRow;
import edu.ntnu.idatt2003.g23.io.StockCsvLoader;
import edu.ntnu.idatt2003.g23.model.Stock;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.converter.DefaultStringConverter;

/**
 * In-game CSV editor that opens whenever a stock CSV file fails to parse.
 *
 * <p>All rows (valid and invalid) are shown in an editable {@link TableView}.
 * Invalid rows are highlighted in red.  The user must fix all errors before
 * the "Save &amp; Continue" button becomes active.  On confirmation the fixed
 * data is written to a new file chosen by the user, then passed to
 * {@code onSuccess} so the game can start.</p>
 */
public final class CsvEditorView {

    private CsvEditorView() {}

    /**
     * Builds the CSV editor view.
     *
     * @param result    the parse result to display and edit
     * @param onCancel  called when the user presses Back
     * @param onSuccess called with the corrected {@link Stock} list when the
     *                  user saves and continues
     */
    public static Parent build(CsvParseResult result, Runnable onCancel,
                               Consumer<List<Stock>> onSuccess) {

        ObservableList<CsvRow> rows =
                FXCollections.observableArrayList(result.getRows());

        // Single source of truth for whether any row has an error.
        // Updated explicitly whenever rows are edited, added, or removed.
        SimpleBooleanProperty hasErrors = new SimpleBooleanProperty(
                rows.stream().anyMatch(CsvRow::hasError));

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("home-page", "background-overlay");

        // ── Top bar ───────────────────────────────────────────────────────────
        Button backButton = new Button("\u2190 Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> onCancel.run());

        Label titleLabel = new Label("Fix CSV Errors");
        titleLabel.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(12, backButton, spacer, titleLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(24, 32, 8, 32));
        root.setTop(topBar);

        // ── Error summary label ───────────────────────────────────────────────
        Label errorCountLabel = new Label();
        errorCountLabel.getStyleClass().add("csv-error-count");

        // Tooltip removed as per user request

        // Shared refresh: call this after any mutation that may change error state.
        Runnable refreshState = () -> {
            updateErrorCount(errorCountLabel, rows);
            hasErrors.set(rows.stream().anyMatch(CsvRow::hasError));
        };

        refreshState.run();

        // ── Table ─────────────────────────────────────────────────────────────
        TableView<CsvRow> table = new TableView<>(rows);
        table.setEditable(true);
        table.getStyleClass().add("csv-editor-table");

        // Line # (read-only)
        TableColumn<CsvRow, Number> lineCol = new TableColumn<>("#");
        lineCol.setCellValueFactory(c -> c.getValue().lineNumberProperty());
        lineCol.setPrefWidth(52);
        lineCol.setMinWidth(52);
        lineCol.setMaxWidth(80);
        lineCol.setSortable(false);
        lineCol.setEditable(false);

        // Symbol
        TableColumn<CsvRow, String> symbolCol = new TableColumn<>("Symbol");
        symbolCol.setCellValueFactory(c -> c.getValue().symbolProperty());
        symbolCol.setCellFactory(tc -> errorAwareCell("symbol"));
        symbolCol.setPrefWidth(100);
        symbolCol.setSortable(false);
        symbolCol.setOnEditCommit(e -> {
            e.getRowValue().setSymbol(e.getNewValue());
            StockCsvLoader.validateRow(e.getRowValue());
            table.refresh();
            refreshState.run();
        });

        // Company
        TableColumn<CsvRow, String> companyCol = new TableColumn<>("Company");
        companyCol.setCellValueFactory(c -> c.getValue().companyProperty());
        companyCol.setCellFactory(tc -> errorAwareCell("company"));
        companyCol.setPrefWidth(220);
        companyCol.setSortable(false);
        companyCol.setOnEditCommit(e -> {
            e.getRowValue().setCompany(e.getNewValue());
            StockCsvLoader.validateRow(e.getRowValue());
            table.refresh();
            refreshState.run();
        });

        // Prices
        TableColumn<CsvRow, String> pricesCol = new TableColumn<>("Prices (semicolon-separated)");
        pricesCol.setCellValueFactory(c -> c.getValue().pricesProperty());
        pricesCol.setCellFactory(tc -> errorAwareCell("prices"));
        pricesCol.setPrefWidth(260);
        pricesCol.setSortable(false);
        pricesCol.setOnEditCommit(e -> {
            e.getRowValue().setPrices(e.getNewValue());
            StockCsvLoader.validateRow(e.getRowValue());
            table.refresh();
            refreshState.run();
        });

        // Error
        TableColumn<CsvRow, String> errorCol = new TableColumn<>("Error");
        errorCol.setCellValueFactory(c -> c.getValue().errorMessageProperty());
        errorCol.setPrefWidth(300);
        errorCol.setSortable(false);
        errorCol.setEditable(false);
        errorCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    setTooltip(new Tooltip(item));
                    setStyle("-fx-text-fill: #ff6b6b;");
                }
            }
        });

        // Skip (per-row button)
        TableColumn<CsvRow, Void> skipRowCol = new TableColumn<>("");
        skipRowCol.setPrefWidth(70);
        skipRowCol.setMinWidth(70);
        skipRowCol.setMaxWidth(70);
        skipRowCol.setSortable(false);
        skipRowCol.setEditable(false);
        skipRowCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Skip");
            {
                btn.getStyleClass().addAll("secondary-button", "csv-skip-button");
                btn.setStyle("-fx-pref-height: 24; -fx-font-size: 11; -fx-padding: 2 8;");
                btn.setOnAction(e -> {
                    CsvRow row = getTableView().getItems().get(getIndex());
                    rows.remove(row);
                    refreshState.run();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(lineCol, symbolCol, companyCol, pricesCol, errorCol, skipRowCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Row factory: highlight error rows
        table.setRowFactory(tv -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(CsvRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("csv-row-error", "csv-row-ok");
                if (!empty && item != null) {
                    if (item.hasError()) {
                        getStyleClass().add("csv-row-error");
                    } else {
                        getStyleClass().add("csv-row-ok");
                    }
                }
            }
        });

        // ── Navigation toolbar ────────────────────────────────────────────────
        Button prevErrBtn = new Button("\u2191 Prev Error");
        prevErrBtn.getStyleClass().addAll("secondary-button", "csv-nav-button");
        prevErrBtn.disableProperty().bind(hasErrors.not());
        prevErrBtn.setOnAction(e -> navigateError(table, rows, -1));

        Button nextErrBtn = new Button("Next Error \u2193");
        nextErrBtn.getStyleClass().addAll("secondary-button", "csv-nav-button");
        nextErrBtn.disableProperty().bind(hasErrors.not());
        nextErrBtn.setOnAction(e -> navigateError(table, rows, +1));

        Label navSep = new Label("|");
        navSep.getStyleClass().add("csv-nav-sep");

        TextField jumpField = new TextField();
        jumpField.setPromptText("Jump to line\u2026");
        jumpField.setPrefWidth(140);
        jumpField.getStyleClass().add("csv-jump-field");
        jumpField.setOnAction(e -> {
            handleJumpToLine(table, rows, jumpField.getText().trim());
            jumpField.clear();
        });

        Button jumpBtn = new Button("Go \u2192");
        jumpBtn.getStyleClass().addAll("secondary-button", "csv-nav-button");
        jumpBtn.setOnAction(e -> {
            handleJumpToLine(table, rows, jumpField.getText().trim());
            jumpField.clear();
        });

        HBox navBar = new HBox(8, prevErrBtn, nextErrBtn, navSep, jumpField, jumpBtn);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.getStyleClass().add("csv-nav-bar");

        // ── Bottom bar ────────────────────────────────────────────────────────
        Label hintLabel = new Label("Click a cell to edit it, or press Skip on a row to remove it.");
        hintLabel.getStyleClass().add("sub-tagline");

        // Add Row button
        Button addRowBtn = new Button("+ Add Row");
        addRowBtn.getStyleClass().add("secondary-button");
        addRowBtn.setStyle("-fx-pref-height: 44; -fx-font-size: 13;");
        addRowBtn.setOnAction(e -> {
            int nextLine = rows.size() > 0 ? rows.get(rows.size() - 1).getLineNumber() + 1 : 1;
            CsvRow newRow = new CsvRow(nextLine, "", "", "", "");
            rows.add(newRow);
            table.getSelectionModel().select(newRow);
            table.scrollTo(newRow);
            refreshState.run();
        });

        // "Skip all broken rows" — removes every row that still has an error
        Button skipAllBtn = new Button("\u2715  Skip All Broken Rows");
        skipAllBtn.getStyleClass().addAll("secondary-button", "csv-skip-button");
        skipAllBtn.setStyle("-fx-pref-height: 44; -fx-font-size: 13;");
        skipAllBtn.disableProperty().bind(hasErrors.not());
        skipAllBtn.setOnAction(e -> {
            rows.removeIf(CsvRow::hasError);
            refreshState.run();
        });

        // "Continue without saving" — starts game in memory, no file picker
        Button continueBtn = new Button("\u25B6  Continue (no save)");
        continueBtn.getStyleClass().add("secondary-button");
        continueBtn.setStyle("-fx-pref-height: 44; -fx-font-size: 13;");
        continueBtn.disableProperty().bind(hasErrors);
        continueBtn.setOnAction(e -> {
            rows.forEach(StockCsvLoader::validateRow);
            if (rows.stream().anyMatch(CsvRow::hasError)) return;
            List<Stock> stocks = rows.stream()
                    .map(StockCsvLoader::rowToStock)
                    .collect(Collectors.toList());
            onSuccess.accept(stocks);
        });

        // "Save As & Continue" — writes to a new file then starts game
        Button saveBtn = new Button("\u2714  Save As & Continue");
        saveBtn.getStyleClass().add("start-button");
        saveBtn.setStyle("-fx-pref-height: 44; -fx-font-size: 15;");
        saveBtn.disableProperty().bind(hasErrors);
        saveBtn.setOnAction(e -> handleSaveAndContinue(root, rows, onSuccess));

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        HBox bottomBar = new HBox(12, hintLabel, addRowBtn, bottomSpacer, skipAllBtn, continueBtn, saveBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(12, 32, 24, 32));

        // ── Assemble ──────────────────────────────────────────────────────────
        VBox centerBox = new VBox(8, errorCountLabel, navBar, new Separator(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        centerBox.setPadding(new Insets(0, 32, 0, 32));

        root.setCenter(centerBox);
        root.setBottom(bottomBar);

        // Escape → back
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                onCancel.run();
                e.consume();
            }
        });

        return root;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void updateErrorCount(Label label, ObservableList<CsvRow> rows) {
        long errors = rows.stream().filter(CsvRow::hasError).count();
        if (errors == 0) {
            label.setText("\u2714  All rows are valid \u2014 you can save and continue.");
            label.getStyleClass().removeAll("csv-error-count-bad");
            label.getStyleClass().add("csv-error-count-ok");
        } else {
            label.setText("\u2716  " + errors + " row" + (errors == 1 ? "" : "s")
                    + " still " + (errors == 1 ? "has" : "have") + " errors.");
            label.getStyleClass().removeAll("csv-error-count-ok");
            label.getStyleClass().add("csv-error-count-bad");
        }
    }

    private static void handleSaveAndContinue(BorderPane root,
                                               ObservableList<CsvRow> rows,
                                               Consumer<List<Stock>> onSuccess) {
        // Re-validate before saving
        rows.forEach(StockCsvLoader::validateRow);
        if (rows.stream().anyMatch(CsvRow::hasError)) {
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Fixed CSV As…");
        fc.setInitialFileName("stocks_fixed.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File target = fc.showSaveDialog(root.getScene().getWindow());

        if (target == null) {
            return; // user cancelled the save dialog
        }

        try {
            writeCsv(target, rows);
        } catch (IOException ex) {
            // Surface the write error — keep editor open
            javafx.scene.control.Alert alert =
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText("Could not save the CSV file");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
            return;
        }

        List<Stock> stocks = rows.stream()
                .map(StockCsvLoader::rowToStock)
                .collect(Collectors.toList());
        onSuccess.accept(stocks);
    }

    /** Writes all rows to a CSV file with a header line. */
    private static void writeCsv(File target, List<CsvRow> rows) throws IOException {
        try (PrintWriter pw = new PrintWriter(target, StandardCharsets.UTF_8)) {
            pw.println("symbol,company,prices");
            for (CsvRow row : rows) {
                pw.printf("%s,%s,%s%n",
                        escapeCsvField(row.getSymbol()),
                        escapeCsvField(row.getCompany()),
                        row.getPrices().trim());
            }
        }
    }

    /** Wraps a CSV field in double-quotes if it contains commas, quotes, or
     * newlines; doubles any embedded quote characters.
     */
    private static String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Returns a {@link TextFieldTableCell} that additionally applies the
     * {@code csv-cell-error-col} CSS class whenever the row's
     * {@code errorColumn} matches {@code colId}.
     */
    private static TextFieldTableCell<CsvRow, String> errorAwareCell(String colId) {
        return new TextFieldTableCell<>(new DefaultStringConverter()) {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("csv-cell-error-col");
                setGraphic(null);
                if (!empty && getTableRow() != null
                        && getTableRow().getItem() instanceof CsvRow row
                        && colId.equals(row.getErrorColumn())) {
                    getStyleClass().add("csv-cell-error-col");
                    // ⚠ warning badge on the left
                    Label badge = new Label("\u26A0");
                    badge.getStyleClass().add("csv-cell-error-badge");
                    setGraphic(badge);
                }
            }
        };
    }

    /**
     * Navigates to the previous ({@code direction < 0}) or next
     * ({@code direction > 0}) error row, wrapping around if needed.
     */
    private static void navigateError(TableView<CsvRow> table,
                                      ObservableList<CsvRow> rows, int direction) {
        int selected = table.getSelectionModel().getSelectedIndex();
        int n = rows.size();
        if (n == 0) return;

        if (direction > 0) {
            int start = selected < 0 ? 0 : selected + 1;
            for (int i = 0; i < n; i++) {
                int idx = (start + i) % n;
                if (rows.get(idx).hasError()) {
                    table.scrollTo(idx);
                    table.getSelectionModel().select(idx);
                    return;
                }
            }
        } else {
            int start = selected <= 0 ? n - 1 : selected - 1;
            for (int i = 0; i < n; i++) {
                int idx = ((start - i) % n + n) % n;
                if (rows.get(idx).hasError()) {
                    table.scrollTo(idx);
                    table.getSelectionModel().select(idx);
                    return;
                }
            }
        }
    }

    /**
     * Jumps to the row whose {@code lineNumber} matches the given string.
     * Silently does nothing if the text is not a valid integer or not found.
     */
    private static void handleJumpToLine(TableView<CsvRow> table,
                                         ObservableList<CsvRow> rows, String text) {
        try {
            int target = Integer.parseInt(text);
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).getLineNumber() == target) {
                    table.scrollTo(i);
                    table.getSelectionModel().select(i);
                    return;
                }
            }
        } catch (NumberFormatException ignored) {
            // non-numeric input — ignore silently
        }
    }
}
