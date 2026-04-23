package edu.ntnu.idatt2003.g23.io;

import java.util.List;

/**
 * Result of a lenient CSV parse: every non-blank, non-comment data line
 * is represented as a {@link CsvRow}, whether or not it is valid.
 *
 * <p>Rows with validation problems have a non-empty
 * {@link CsvRow#getErrorMessage()}.  Callers can check {@link #hasErrors()}
 * before deciding whether to open the in-game CSV editor.</p>
 */
public class CsvParseResult {

  private final List<CsvRow> rows;

  public CsvParseResult(List<CsvRow> rows) {
    this.rows = List.copyOf(rows);
  }

  /**
   * All data rows in source order (valid and invalid).
   */
  public List<CsvRow> getRows() {
    return rows;
  }

  /**
   * {@code true} if at least one row currently has a validation error.
   */
  public boolean hasErrors() {
    return rows.stream().anyMatch(CsvRow::hasError);
  }

  /**
   * Number of rows that currently carry a validation error.
   */
  public long errorCount() {
    return rows.stream().filter(CsvRow::hasError).count();
  }
}
