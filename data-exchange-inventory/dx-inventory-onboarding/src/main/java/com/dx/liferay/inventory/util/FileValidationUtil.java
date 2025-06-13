package com.dx.liferay.inventory.util;

import com.dx.liferay.inventory.constants.InventoryConstants;
import com.dx.liferay.inventory.model.ValidationResult;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class for validating uploaded files against sample file templates.
 */
public class FileValidationUtil {

    private static final Log _log = LogFactoryUtil.getLog(FileValidationUtil.class);

    /**
     * Validates an uploaded file against a sample file template by performing comprehensive checks
     * including basic file validation, structure comparison, and data value validation against
     * dynamic rules defined in the sample file.
     *
     * @param uploadedFile the file uploaded by the user to validate
     * @param sampleFileEntry the sample file entry containing structure and validation rules
     * @param userLocale the user's locale for localized error messages
     * @return ValidationResult containing validation status, errors, and file metadata
     */
    public static ValidationResult validateFile(File uploadedFile, FileEntry sampleFileEntry, Locale userLocale) {
        ValidationResult result = new ValidationResult();

        try {
            // 1. Basic file validations
            if (!validateBasicFile(uploadedFile, result)) {
                return result;
            }

            // 2. Load sample file structure with validation rules
            FileStructure sampleStructure = readSampleFileWithRules(sampleFileEntry);

            if (sampleStructure.isEmpty()) {
                result.addError("Could not read sample file structure");
                return result;
            }

            // 3. Parse uploaded file
            FileStructure uploadedStructure = parseUploadedFile(uploadedFile);
            if (uploadedStructure.isEmpty()) {
                result.addError("File is empty.");
                return result;
            }

            // 4. Compare structures
            compareStructures(sampleStructure, uploadedStructure, result);

            // 5. Validate data values against dynamic rules
            validateDataValues(sampleStructure, uploadedStructure, result);

            result.getMetadata().setFileName(uploadedFile.getName());
            result.getMetadata().setColumnCount(uploadedStructure.getColumnCount());
            result.getMetadata().setRowCount(uploadedStructure.getRowCount());

            result.setValid(result.getErrors().isEmpty());

        } catch (Exception e) {
            result.addError("Validation failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Performs basic file validation checks including existence, size, and format validation.
     * These are prerequisite checks that must pass before detailed structure validation.
     *
     * @param file the file to perform basic validation on
     * @param result the validation result to add errors to if validation fails
     * @return true if basic validation passes, false if any check fails
     */
    public static boolean validateBasicFile(File file, ValidationResult result) {
        if (file == null || !file.exists()) {
            result.addError("File does not exist");
            return false;
        }

        if (file.length() == 0) {
            result.addError("File is empty");
            return false;
        }

        if (file.length() > InventoryConstants.MAX_FILE_SIZE_BYTES) {
            result.addError("File size exceeds 10MB limit");
            return false;
        }

        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            result.addError("Unsupported file format. Only XLSX, XLS files are allowed");
            return false;
        }

        return true;
    }

    /**
     * Reads the sample file structure and validation rules from a two-sheet Excel template.
     * The first sheet contains the structure definition with headers and merged cells,
     * while the second sheet contains validation rules for dynamic data validation.
     *
     * @param fileEntry the sample file entry containing structure and validation definitions
     * @return FileStructure object populated with column definitions and validation rules
     */
    public static FileStructure readSampleFileWithRules(FileEntry fileEntry) {
        FileStructure structure = new FileStructure();

        try (InputStream inputStream = fileEntry.getContentStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            // Read first sheet for structure
            Sheet structureSheet = workbook.getSheetAt(0);
            readStructureFromSheet(structureSheet, structure);

            // Read second sheet for validation rules (whatever it's named)
            if (workbook.getNumberOfSheets() > 1) {
                Sheet rulesSheet = workbook.getSheetAt(1);
                readValidationRulesFromDropdownSheet(rulesSheet, structure);
            } else {
                _log.warn("No second sheet found for validation rules.");
            }
        } catch (Exception e) {
            _log.error("Error reading sample file: " + e.getMessage(), e);
        }
        return structure;
    }

    /**
     * Reads the structure definition from an Excel sheet, handling both merged headers
     * and sub-headers to create a complete column definition structure.
     *
     * @param sheet the Excel sheet containing structure definition with headers
     * @param structure the FileStructure object to populate with column definitions
     */
    private static void readStructureFromSheet(Sheet sheet, FileStructure structure) {
        try {
            Map<Integer, String> mergedRegions = getMergedRegions(sheet);

            // Read headers
            Row headerRow = sheet.getRow(0);
            Row subHeaderRow = sheet.getLastRowNum() >= 1 ? sheet.getRow(1) : null;

            if (headerRow != null) {
                IntStream.range(0, headerRow.getLastCellNum())
                        .forEach(colIdx -> {
                            String mainHeader = getCellValue(headerRow.getCell(colIdx)).trim();
                            String subHeader = subHeaderRow != null ?
                                    getCellValue(subHeaderRow.getCell(colIdx)).trim() : "";

                            // Handle merged headers
                            String mergedParent = mergedRegions.get(colIdx);
                            if (mergedParent != null && !mergedParent.isEmpty()) {
                                mainHeader = mergedParent;
                            }

                            String finalColumnName = !subHeader.isEmpty() ? subHeader : mainHeader;

                            if (!finalColumnName.isEmpty()) {
                                structure.addColumn(finalColumnName, FileStructure.DataType.TEXT);

                                if (!mainHeader.equals(finalColumnName) && !mainHeader.isEmpty()) {
                                    structure.addMergedHeader(mainHeader, finalColumnName);
                                }
                            }
                        });
            }
        } catch (Exception e) {
            _log.error("Error reading structure from sheet: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts merged cell region information from an Excel sheet and creates a mapping
     * of column indices to their corresponding merged header values.
     *
     * @param sheet the Excel sheet to analyze for merged cell regions
     * @return map of column indices to their merged header values
     */
    private static Map<Integer, String> getMergedRegions(Sheet sheet) {
        Map<Integer, String> mergedRegions = new HashMap<>();

        IntStream.range(0, sheet.getNumMergedRegions())
                .mapToObj(sheet::getMergedRegion)
                .forEach(mergedRegion -> {
                    // Get the value from the first cell of the merged region
                    Row row = sheet.getRow(mergedRegion.getFirstRow());
                    if (row != null) {
                        Cell firstCell = row.getCell(mergedRegion.getFirstColumn());
                        String mergedValue = getCellValue(firstCell).trim();

                        // Apply this value to all columns in the merged region
                        IntStream.rangeClosed(mergedRegion.getFirstColumn(), mergedRegion.getLastColumn())
                                .forEach(col -> mergedRegions.put(col, mergedValue));
                    }
                });

        return mergedRegions;
    }

    /**
     * Reads validation rules from the second sheet of the sample file
     *
     * @param rulesSheet the Excel sheet containing validation rule definitions
     * @param structure the FileStructure to add validation rules to
     */
    private static void readValidationRulesFromDropdownSheet(Sheet rulesSheet, FileStructure structure) {
        try {
            if (rulesSheet.getRow(0) == null) {
                _log.warn("No header row found in validation sheet");
                return;
            }

            // Process all rows in a single stream
            Map<String, Set<String>> columnData = IntStream.rangeClosed(1, rulesSheet.getLastRowNum())
                    .mapToObj(rulesSheet::getRow)
                    .filter(Objects::nonNull)
                    .collect(HashMap::new, (map, row) -> {
                        // Collect data from each column
                        addToColumnData(map, "classification", getCellValue(row.getCell(0)));
                        addToColumnData(map, "scale", getCellValue(row.getCell(1)));
                        addToColumnData(map, "boolean", getCellValue(row.getCell(2)));
                        addToColumnData(map, "year", getCellValue(row.getCell(3)));
                        addToColumnData(map, "month", getCellValue(row.getCell(4)));
                    }, (map1, map2) -> {});

            createValidationRulesFromMap(structure, columnData);

        } catch (Exception e) {
            _log.error("Error reading validation rules from dropdown sheet: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a non-empty trimmed value to the specified column's data set.
     *
     * @param map the column data map to add the value to
     * @param column the column identifier (e.g., "classification", "scale")
     * @param value the raw cell value to trim and add
     */
    private static void addToColumnData(Map<String, Set<String>> map, String column, String value) {
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
            map.computeIfAbsent(column, k -> new HashSet<>()).add(trimmed);
        }
    }

    /**
     * Creates validation rules for file structure based on collected column data from dropdown sheet.
     * Processes classification (ENUM), scale (NUMERIC range), boolean (BOOLEAN), year (NUMERIC exact),
     * and month (ENUM) validation rules with appropriate constraints.
     *
     * @param structure the FileStructure object to add validation rules to
     * @param columnData map of column types to their collected string values from dropdown sheet
     */
    private static void createValidationRulesFromMap(FileStructure structure, Map<String, Set<String>> columnData) {

        // Classification rule (Column A)
        Set<String> classificationValues = columnData.get("classification");
        if (classificationValues != null && !classificationValues.isEmpty()) {
            FileStructure.ValidationRule classificationRule =
                    new FileStructure.ValidationRule(FileStructure.DataType.ENUM);
            classificationRule.setAllowedValues(classificationValues);
            structure.addValidationRule("dataset classification", classificationRule);
        }

        // Scale rules (Column B)
        Set<String> scaleStrings = columnData.get("scale");
        if (scaleStrings != null && !scaleStrings.isEmpty()) {
            Set<Integer> scaleValues = scaleStrings.stream()
                    .map(value -> {
                        try {
                            return Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (!scaleValues.isEmpty()) {
                int minValue = scaleValues.stream().min(Integer::compareTo).orElse(1);
                int maxValue = scaleValues.stream().max(Integer::compareTo).orElse(5);

                // Create scale validation rules for priority fields
                String[] scaleFields = {"user demand", "economic impact", "better services", "better governance"};
                Arrays.stream(scaleFields).forEach(field -> {
                    FileStructure.ValidationRule scaleRule =
                            new FileStructure.ValidationRule(FileStructure.DataType.SCALE);
                    scaleRule.setRange(minValue, maxValue);
                    structure.addValidationRule(field, scaleRule);
                });
            }
        }

        // Boolean rules (Column C)
        Set<String> booleanValues = columnData.get("boolean");
        if (booleanValues != null && !booleanValues.isEmpty()) {
            String[] booleanFields = {"defined owner", "existing metadata", "already published", "open format"};
            Arrays.stream(booleanFields).forEach(field -> {
                FileStructure.ValidationRule booleanRule =
                        new FileStructure.ValidationRule(FileStructure.DataType.BOOLEAN);
                structure.addValidationRule(field, booleanRule);
            });
        }

        // Year rule (Column D) - take first valid year
        Set<String> yearStrings = columnData.get("year");
        if (yearStrings != null && !yearStrings.isEmpty()) {
            Optional<Integer> yearValue = yearStrings.stream()
                    .map(value -> {
                        try {
                            return Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .findFirst();

            if (yearValue.isPresent()) {
                FileStructure.ValidationRule yearRule =
                        new FileStructure.ValidationRule(FileStructure.DataType.NUMERIC);
                yearRule.setRange(yearValue.get(), yearValue.get());
                structure.addValidationRule("year", yearRule);
            }
        }

        // Month rule (Column E)
        Set<String> monthValues = columnData.get("month");
        if (monthValues != null && !monthValues.isEmpty()) {
            FileStructure.ValidationRule monthRule =
                    new FileStructure.ValidationRule(FileStructure.DataType.ENUM);
            monthRule.setAllowedValues(monthValues);
            structure.addValidationRule("month", monthRule);
        }
    }

    /**
     * Parses an uploaded file to extract its structure based on file type.
     * Currently supports Excel files (.xlsx and .xls formats).
     *
     * @param file the uploaded file to parse for structure analysis
     * @return FileStructure object containing the parsed structure, or empty structure if unsupported format
     */
    private static FileStructure parseUploadedFile(File file) {
        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return parseExcelFile(file);
        }
        return new FileStructure();
    }


    /**
     * Parses an Excel file to extract its complete structure including headers, merged cells,
     * and a sample of data for validation purposes. Handles both main headers and sub-headers.
     *
     * @param file the Excel file to parse
     * @return FileStructure object populated with column definitions and sample data
     */
    private static FileStructure parseExcelFile(File file) {
        FileStructure structure = new FileStructure();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Handle merged headers similar to sample file
            Map<Integer, String> mergedRegions = getMergedRegions(sheet);

            Row headerRow = sheet.getRow(0);
            Row subHeaderRow = sheet.getLastRowNum() >= 1 ? sheet.getRow(1) : null;

            if (headerRow != null) {
                // Read headers
                IntStream.range(0, headerRow.getLastCellNum())
                        .forEach(colIdx -> {
                            String mainHeader = getCellValue(headerRow.getCell(colIdx)).trim();
                            String subHeader = subHeaderRow != null ?
                                    getCellValue(subHeaderRow.getCell(colIdx)).trim() : "";

                            String mergedParent = mergedRegions.get(colIdx);
                            if (mergedParent != null && !mergedParent.isEmpty()) {
                                mainHeader = mergedParent;
                            }

                            String finalColumnName = !subHeader.isEmpty() ? subHeader : mainHeader;

                            if (!finalColumnName.isEmpty()) {
                                structure.addColumn(finalColumnName, FileStructure.DataType.TEXT);
                            }
                        });

                // Read data
                int dataStartRow = subHeaderRow != null ? 2 : 1;
                int maxRows = Math.min(sheet.getLastRowNum() + 1, dataStartRow + 100);

                IntStream.range(dataStartRow, maxRows)
                        .mapToObj(sheet::getRow)
                        .filter(Objects::nonNull)
                        .forEach(row -> {
                            IntStream.range(0, structure.getColumnCount())
                                    .forEach(colIdx -> {
                                        Cell cell = row.getCell(colIdx);
                                        String value = getCellValue(cell);
                                        structure.addDataToColumn(colIdx, value);
                                    });
                        });
            }

        } catch (Exception e) {
            _log.error("Error parsing Excel file: " + e.getMessage(), e);
        }

        return structure;
    }

    /**
     * Compares the structure of a sample file against an uploaded file to identify
     * discrepancies in column count, column names, and overall structure alignment.
     *
     * @param sample the sample file structure containing expected column definitions
     * @param uploaded the uploaded file structure to validate against the sample
     * @param result the validation result to add structural errors to
     */
    private static void compareStructures(FileStructure sample, FileStructure uploaded, ValidationResult result) {
        List<String> sampleColumns = sample.getColumnNames();
        List<String> uploadedColumns = uploaded.getColumnNames();

        // Compare column count
        if (sampleColumns.size() != uploadedColumns.size()) {
            result.addError(String.format("Column count mismatch. Expected: %d, Found: %d",
                    sampleColumns.size(), uploadedColumns.size()));
        }

        // Normalize and compare column names
        Set<String> sampleNormalized = sampleColumns.stream()
                .map(col -> col.toLowerCase().trim().replaceAll("\\s+", " "))
                .collect(Collectors.toSet());

        Set<String> uploadedNormalized = uploadedColumns.stream()
                .map(col -> col.toLowerCase().trim().replaceAll("\\s+", " "))
                .collect(Collectors.toSet());

        // Find missing and extra columns
        Set<String> missing = new HashSet<>(sampleNormalized);
        missing.removeAll(uploadedNormalized);

        Set<String> extra = new HashSet<>(uploadedNormalized);
        extra.removeAll(sampleNormalized);

        if (!missing.isEmpty()) {
            result.addError("Missing columns: " + String.join(", ", missing));
        }

        if (!extra.isEmpty()) {
            result.addError("Extra columns found: " + String.join(", ", extra));
        }
    }


    private static void validateDataValues(FileStructure sample, FileStructure uploaded, ValidationResult result) {
        Map<String, FileStructure.ValidationRule> rules = sample.getValidationRules();
        List<String> uploadedColumns = uploaded.getColumnNames();

        IntStream.range(0, uploadedColumns.size())
                .forEach(colIdx -> {
                    String columnName = uploadedColumns.get(colIdx);
                    String normalizedName = FileStructure.normalizeColumnName(columnName);
                    FileStructure.ValidationRule rule = rules.get(normalizedName);

                    if (rule != null) {
                        List<String> columnData = uploaded.getColumnData(colIdx);

                        IntStream.range(0, columnData.size())
                                .filter(rowIdx -> {
                                    String cellValue = columnData.get(rowIdx);
                                    return cellValue != null && !cellValue.trim().isEmpty();
                                })
                                .forEach(rowIdx -> {
                                    String cellValue = columnData.get(rowIdx);
                                    ValidationResult cellValidation = rule.validate(cellValue);

                                    if (!cellValidation.isValid()) {
                                        String errorMsg = String.format("Row %d, Column '%s': %s (Value: '%s')",
                                                rowIdx + 2, columnName,
                                                String.join(", ", cellValidation.getErrors()),
                                                cellValue);
                                        result.addError(errorMsg);
                                    }
                                });
                    }
                });
    }

    /**
     * Extracts cell value as string handling different cell types.
     *
     * @param cell the cell to extract value from
     * @return string representation of cell value
     */
    public static String getCellValue(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ?
                    cell.getDateCellValue().toString() : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}