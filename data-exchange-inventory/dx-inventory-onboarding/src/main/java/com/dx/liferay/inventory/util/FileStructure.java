package com.dx.liferay.inventory.util;

import com.dx.liferay.inventory.model.ValidationResult;
import java.util.*;

/**
 * Represents the structure of a file including column definitions, data types, validation rules,
 * and special field mappings. Used for parsing and validating Excel files against expected formats.
 * Supports identification of special fields like dataset names and attributes for inventory processing.
 */
public class FileStructure {
    private List<String> columnNames = new ArrayList<>();
    private Map<String, DataType> columnTypes = new HashMap<>();
    private List<List<String>> columnData = new ArrayList<>();
    private Map<String, Integer> maxLengths = new HashMap<>();
    private Map<String, ValidationRule> validationRules = new HashMap<>();
    private Map<String, String> mergedHeaders = new HashMap<>();

    private String datasetNameField;
    private String attributesField;
    private String attributeDescriptionField;
    private Set<String> specialFields = new HashSet<>();

    /**
     * Identifies and assigns special fields based on column positions.
     */
    public void identifySpecialFields() {
        List<String> columns = getColumnNames();

        if (columns.size() >= 5) {
            this.datasetNameField = columns.get(1);
            this.attributesField = columns.get(4);
            this.attributeDescriptionField = columns.get(5);

            specialFields.add(datasetNameField);
            specialFields.add(attributesField);
            specialFields.add(attributeDescriptionField);
        }
    }

    /**
     * Returns the field name used for dataset identification.
     *
     * @return the dataset name field, or null if not identified
     */
    public String getDatasetNameField() { return datasetNameField; }

    /**
     * Returns the field name used for attribute names.
     *
     * @return the attributes field name, or null if not identified
     */
    public String getAttributesField() { return attributesField; }

    /**
     * Returns the field name used for attribute descriptions.
     *
     * @return the attribute description field name, or null if not identified
     */
    public String getAttributeDescriptionField() { return attributeDescriptionField; }


    /**
     * Returns a copy of the set containing all identified special field names.
     *
     * @return set of special field names that require special processing
     */
    public Set<String> getSpecialFields() { return new HashSet<>(specialFields); }

    /**
     * Adds a new column to the file structure with the specified name and data type.
     * Initializes empty data storage and sets maximum length tracking for the column.
     *
     * @param columnName the name of the column to add
     * @param dataType the data type expected for values in this column
     */
    public void addColumn(String columnName, DataType dataType) {
        columnNames.add(columnName);
        columnTypes.put(normalizeColumnName(columnName), dataType);
        columnData.add(new ArrayList<>());
        maxLengths.put(normalizeColumnName(columnName), 0);
    }

    /**
     * Adds a mapping for merged cell headers, linking child headers to their parent headers.
     * Used to handle Excel files with merged header cells spanning multiple columns.
     *
     * @param parentHeader the main header name for the merged cell group
     * @param childHeader the sub-header name that falls under the parent header
     */
    public void addMergedHeader(String parentHeader, String childHeader) {
        mergedHeaders.put(normalizeColumnName(childHeader), normalizeColumnName(parentHeader));
    }

    /**
     * Associates a validation rule with a specific column for data validation purposes.
     *
     * @param columnName the name of the column to apply the validation rule to
     * @param rule the validation rule containing constraints and validation logic
     */
    public void addValidationRule(String columnName, ValidationRule rule) {
        validationRules.put(normalizeColumnName(columnName), rule);
    }

    /**
     * Adds data to a specific column and updates the maximum length tracking for that column.
     * Automatically trims whitespace from the data before storage.
     *
     * @param columnIndex the zero-based index of the column to add data to
     * @param data the data value to add to the column
     */
    public void addDataToColumn(int columnIndex, String data) {
        if (columnIndex < columnData.size()) {
            String cleanData = data != null ? data.trim() : "";
            columnData.get(columnIndex).add(cleanData);

            // Update max length
            String colName = normalizeColumnName(columnNames.get(columnIndex));
            maxLengths.put(colName, Math.max(maxLengths.get(colName), cleanData.length()));
        }
    }

    /**
     * Returns a copy of the list of column names in their original order.
     *
     * @return list of column names as they appear in the file structure
     */
    public List<String> getColumnNames() {
        return new ArrayList<>(columnNames);
    }

    /**
     * Returns a copy of all data values for the specified column.
     *
     * @param columnIndex the zero-based index of the column to retrieve data from
     * @return list of data values for the specified column, or empty list if index is invalid
     */
    public List<String> getColumnData(int columnIndex) {
        if (columnIndex < columnData.size()) {
            return new ArrayList<>(columnData.get(columnIndex));
        }
        return new ArrayList<>();
    }


    /**
     * Returns the total number of columns in the file structure.
     *
     * @return the number of columns defined in this structure
     */
    public int getColumnCount() {return columnNames.size();}

    /**
     * Returns the number of data rows in the file structure.
     *
     * @return the number of data rows, or 0 if no columns are defined
     */
    public int getRowCount() {return columnData.isEmpty() ? 0 : columnData.get(0).size();}

    /**
     * Checks if the file structure is empty (no columns defined).
     *
     * @return true if no columns have been added to the structure, false otherwise
     */
    public boolean isEmpty() {return columnNames.isEmpty();}

    /**
     * Returns a copy of all validation rules mapped by their normalized column names.
     *
     * @return map of normalized column names to their associated validation rules
     */
    public Map<String, ValidationRule> getValidationRules() {return new HashMap<>(validationRules);}

    /**
     * Normalizes a column name for consistent comparison and storage by converting to lowercase,
     * trimming whitespace, and collapsing multiple spaces into single spaces.
     *
     * @param columnName the column name to normalize
     * @return normalized column name suitable for use as a map key
     */
    public static String normalizeColumnName(String columnName) {
        if (columnName == null) return "";
        return columnName.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /**
     * Enumeration of supported data types for file structure columns.
     * Used for validation and type checking of column data.
     */
    public enum DataType {
        TEXT, NUMERIC, BOOLEAN, DATE, SCALE, ENUM
    }

    /**
     * Validation rule class that defines constraints and validation logic for column data.
     * Supports various data types and constraint types including allowed values, ranges, and patterns.
     */
    public static class ValidationRule {
        public DataType dataType;
        private Set<String> allowedValues;
        private Integer minValue;
        private Integer maxValue;
        private String pattern;
        private boolean required = true;

        /**
         * Constructs a validation rule for the specified data type.
         *
         * @param dataType the data type this rule will validate
         */
        public ValidationRule(DataType dataType) {this.dataType = dataType;}

        /**
         * Sets the allowed values for enumerated data types.
         *
         * @param allowedValues set of string values that are considered valid
         * @return this ValidationRule instance for method chaining
         */
        public ValidationRule setAllowedValues(Set<String> allowedValues) {
            this.allowedValues = allowedValues;
            return this;
        }

        /**
         * Sets the numeric range constraints for numeric and scale data types.
         *
         * @param min the minimum allowed value (inclusive)
         * @param max the maximum allowed value (inclusive)
         * @return this ValidationRule instance for method chaining
         */
        public ValidationRule setRange(Integer min, Integer max) {
            this.minValue = min;
            this.maxValue = max;
            return this;
        }

        /**
         * Validates a value against this rule's constraints and data type requirements.
         * Performs appropriate validation based on the configured data type and constraints.
         *
         * @param value the string value to validate
         * @return ValidationResult containing validation status and any error messages
         */
        public ValidationResult validate(String value) {
            ValidationResult result = new ValidationResult();

            if (value == null || value.trim().isEmpty()) {
                if (required) {
                    result.addError("Value is required");
                }
                return result;
            }

            String cleanValue = value.trim();

            switch (dataType) {
                case ENUM:
                    if (allowedValues != null && !allowedValues.contains(cleanValue)) {
                        result.addError("Value must be one of: " + String.join(", ", allowedValues));
                    }
                    break;

                case SCALE:
                case NUMERIC:
                    try {
                        int numValue = Integer.parseInt(cleanValue);
                        if (minValue != null && numValue < minValue) {
                            result.addError("Value must be >= " + minValue);
                        }
                        if (maxValue != null && numValue > maxValue) {
                            result.addError("Value must be <= " + maxValue);
                        }
                    } catch (NumberFormatException e) {
                        result.addError("Value must be a number");
                    }
                    break;

                case BOOLEAN:
                    Set<String> booleanValues = Set.of("yes", "no");
                    if (!booleanValues.contains(cleanValue.toLowerCase())) {
                        result.addError("Value must be Yes/No");
                    }
                    break;

                case DATE:
                    if (pattern != null && !cleanValue.matches(pattern)) {
                        result.addError("Value does not match expected pattern");
                    }
                    break;
            }
            return result;
        }
    }

}