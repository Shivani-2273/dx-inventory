package com.dx.liferay.inventory.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a validation process, containing validation status,
 * error messages, and metadata about the validated file.
 */
public class ValidationResult {
    private boolean valid = true;
    private List<String> errors = new ArrayList<>();
    private ValidationMetadata metadata;

    /**
     * Constructs a new ValidationResult with default valid status and empty error list.
     * Initializes the metadata object.
     */
    public ValidationResult() {
        this.metadata = new ValidationMetadata();
    }

    /**
     * Checks if the validation result is valid by ensuring both the valid flag is true
     * and no errors have been recorded.
     *
     * @return true if validation passed and no errors exist, false otherwise
     */
    public boolean isValid() { return valid && errors.isEmpty(); }

    /**
     * Sets the validation status.
     *
     * @param valid the validation status to set
     */
    public void setValid(boolean valid) { this.valid = valid; }

    /**
     * Returns a copy of the error list to prevent external modification.
     *
     * @return a new list containing all validation errors
     */
    public List<String> getErrors() { return new ArrayList<>(errors); }

    /**
     * Adds an error message to the validation result and sets the valid status to false.
     *
     * @param error the error message to add
     */
    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    /**
     * Returns the validation metadata containing file information.
     *
     * @return the validation metadata object
     */
    public ValidationMetadata getMetadata() { return metadata; }

    /**
     * Lightweight metadata class that stores essential validation information
     * about the processed file including name, column count, and row count.
     */
    public static class ValidationMetadata {
        private String fileName;
        private int columnCount = 0;
        private int rowCount = 0;


        /**
         * Sets the name of the validated file.
         *
         * @param fileName the file name to set
         */
        public void setFileName(String fileName) { this.fileName = fileName; }

        /**
         * Returns the number of columns in the validated file.
         *
         * @return the column count
         */
        public int getColumnCount() { return columnCount; }

        /**
         * Sets the number of columns in the validated file.
         *
         * @param columnCount the column count to set
         */
        public void setColumnCount(int columnCount) { this.columnCount = columnCount; }

        /**
         * Returns the number of rows in the validated file.
         *
         * @return the row count
         */
        public int getRowCount() { return rowCount; }

        /**
         * Sets the number of rows in the validated file.
         *
         * @param rowCount the row count to set
         */
        public void setRowCount(int rowCount) { this.rowCount = rowCount; }

    }
}
