package edu.ntnu.idatt2003.g23.io;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Observable model for a single CSV data row, used in the in-game CSV editor.
 *
 * <p>All fields are JavaFX properties so the TableView can bind to them
 * and react live when the user edits a cell.</p>
 */
public class CsvRow {

    private final SimpleIntegerProperty lineNumber;
    private final SimpleStringProperty symbol;
    private final SimpleStringProperty company;
    private final SimpleStringProperty prices;
    private final SimpleStringProperty errorMessage;
    /** Which column contains the error: "symbol", "company", "prices", or "" for structural/no error. */
    private final SimpleStringProperty errorColumn;

    /**
     * @param lineNumber   original 1-based line number in the source file
     * @param symbol       stock ticker symbol (may be blank if malformed)
     * @param company      company name (may be blank if malformed)
     * @param prices       semicolon-separated price string, e.g. {@code "214.10;215.00"}
     * @param errorMessage non-empty description of the problem, or {@code ""} if valid
     */
    public CsvRow(int lineNumber, String symbol, String company, String prices, String errorMessage) {
        this.lineNumber    = new SimpleIntegerProperty(lineNumber);
        this.symbol        = new SimpleStringProperty(symbol       == null ? "" : symbol);
        this.company       = new SimpleStringProperty(company      == null ? "" : company);
        this.prices        = new SimpleStringProperty(prices       == null ? "" : prices);
        this.errorMessage  = new SimpleStringProperty(errorMessage == null ? "" : errorMessage);
        this.errorColumn   = new SimpleStringProperty("");
    }

    /** @return {@code true} if this row currently carries a validation error. */
    public boolean hasError() {
        return !errorMessage.get().isEmpty();
    }

    // ── lineNumber ────────────────────────────────────────────────────────────

    public int getLineNumber() { return lineNumber.get(); }
    public SimpleIntegerProperty lineNumberProperty() { return lineNumber; }

    // ── symbol ────────────────────────────────────────────────────────────────

    public String getSymbol() { return symbol.get(); }
    public void setSymbol(String v) { symbol.set(v == null ? "" : v); }
    public SimpleStringProperty symbolProperty() { return symbol; }

    // ── company ───────────────────────────────────────────────────────────────

    public String getCompany() { return company.get(); }
    public void setCompany(String v) { company.set(v == null ? "" : v); }
    public SimpleStringProperty companyProperty() { return company; }

    // ── prices ────────────────────────────────────────────────────────────────

    public String getPrices() { return prices.get(); }
    public void setPrices(String v) { prices.set(v == null ? "" : v); }
    public SimpleStringProperty pricesProperty() { return prices; }

    // ── errorMessage ──────────────────────────────────────────────────────────

    public String getErrorMessage() { return errorMessage.get(); }
    public void setErrorMessage(String v) { errorMessage.set(v == null ? "" : v); }
    public SimpleStringProperty errorMessageProperty() { return errorMessage; }

    // ── errorColumn ───────────────────────────────────────────────────────────

    /** "symbol", "company", "prices", or "" when no column-specific error. */
    public String getErrorColumn() { return errorColumn.get(); }
    public void setErrorColumn(String v) { errorColumn.set(v == null ? "" : v); }
    public SimpleStringProperty errorColumnProperty() { return errorColumn; }
}
