package com.dx.liferay.inventory.service;

import com.dx.liferay.inventory.constants.InventoryConstants;
import com.dx.liferay.inventory.exception.FileProcessingException;
import com.dx.liferay.inventory.util.FileStructure;
import com.dx.liferay.inventory.util.FileValidationUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.util.Validator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.osgi.service.component.annotations.Component;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Service for parsing Excel files and extracting structured data.
 * Handles Excel file processing and provides optimized data extraction
 * based on file structure definitions.
 */
@Component(service = ExcelParsingService.class)
public class ExcelParsingService {

    private static final Log _log = LogFactoryUtil.getLog(ExcelParsingService.class);

    /**
     * Parses an Excel file and extracts datasets using the provided sample file structure as a template.
     * Creates column mappings, identifies special fields, and processes rows to extract structured datasets.
     *
     * @param file the Excel file to parse
     * @param sampleFileEntry the sample file entry used as a structure reference for parsing
     * @return list of extracted datasets, each containing attributes and metadata
     */
    public List<Map<String, Object>> parseExcelFile(File file, FileEntry sampleFileEntry)
            throws FileProcessingException {

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
           FileStructure sampleStructure = FileValidationUtil.readSampleFileWithRules(sampleFileEntry);
            Map<String, Integer> columnMapping = generateColumnMapping(sheet, sampleStructure);

            int dataStartRow = getHeaderRowCount(sampleFileEntry);

            return parseDatasets(sheet, sampleStructure, columnMapping, dataStartRow);

        } catch (IOException e) {
            throw new FileProcessingException("Failed to read Excel file", e);
        } catch (Exception e) {
            throw new FileProcessingException("Error parsing Excel file: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a mapping between column names from the sample structure and their corresponding
     * column indices in the Excel sheet by comparing normalized header values.
     *
     * @param sheet the Excel sheet containing header row and data
     * @param structure the file structure containing expected column names
     * @return map of normalized column names to their column indices in the sheet
     */
    private Map<String, Integer> generateColumnMapping(Sheet sheet, FileStructure structure) {
        Map<String, Integer> mapping = new HashMap<>();
        Row headerRow = sheet.getRow(InventoryConstants.DEFAULT_HEADER_ROW_INDEX);

        if (headerRow != null) {
            IntStream.range(0, headerRow.getLastCellNum())
                    .forEach(colIdx -> {
                        String headerValue = FileStructure.normalizeColumnName(FileValidationUtil.getCellValue(headerRow.getCell(colIdx)));

                        structure.getColumnNames().stream()
                                .filter(sampleKey -> FileStructure.normalizeColumnName(sampleKey).equals(headerValue))
                                .findFirst()
                                .ifPresent(sampleKey -> mapping.put(FileStructure.normalizeColumnName(sampleKey), colIdx));
                    });
        }

        _log.debug("Generated column mapping: " + mapping);
        return mapping;
    }

    /**
     * Parses datasets from Excel sheet.
     * both regular fields and attributes for each dataset found.
     *
     * @param sheet the Excel sheet to parse
     * @param structure the file structure for field mapping
     * @param columnMapping the column index mapping
     * @param dataStartRow the row where data begins
     * @return list of parsed datasets
     */
    private List<Map<String, Object>> parseDatasets(Sheet sheet, FileStructure structure,
                                                    Map<String, Integer> columnMapping, int dataStartRow) {
        List<Map<String, Object>> datasets = new ArrayList<>();
        AtomicReference<Map<String, Object>> currentDataset = new AtomicReference<>();
        AtomicReference<String> lastDatasetName = new AtomicReference<>("");

        structure.identifySpecialFields();
        FieldIndices fieldIndices = new FieldIndices(structure, columnMapping);

        IntStream.rangeClosed(dataStartRow + 1, sheet.getLastRowNum())
                .forEach(i -> {
                    Row row = sheet.getRow(i);
                    if (isRowEmpty(row)) return;

                    String datasetName = getMergedCellValue(sheet, i, fieldIndices.datasetCol).trim();

                    if (isNewDataset(datasetName, lastDatasetName.get())) {
                        Map<String, Object> newDataset = createNewDataset(datasets);
                        currentDataset.set(newDataset);
                        lastDatasetName.set(datasetName);
                        _log.debug("New dataset detected at row " + i + ": " + datasetName);
                    }

                    if (currentDataset.get() != null) {
                        processDatasetRow(sheet, i, currentDataset.get(), columnMapping, structure, fieldIndices);
                    }
                });

        _log.info("Total datasets parsed: " + datasets.size());
        return datasets;
    }

    /**
     * Creates a new dataset entry with an empty attributes list and adds it to the datasets collection.
     *
     * @param datasets the list of datasets to add the new dataset to
     * @return the newly created dataset map with initialized attributes list
     */
    private Map<String, Object> createNewDataset(List<Map<String, Object>> datasets) {
        Map<String, Object> dataset = new HashMap<>();
        List<Map<String, String>> attributes = new ArrayList<>();
        dataset.put("attributes", attributes);
        datasets.add(dataset);
        return dataset;
    }

    /**
     * Determines if the current dataset name indicates the start of a new dataset
     * by comparing it to the previous dataset name.
     *
     * @param datasetName the current dataset name from the Excel row
     * @param lastDatasetName the previous dataset name encountered
     * @return true if this represents a new dataset, false if it's a continuation of the current dataset
     */
    private boolean isNewDataset(String datasetName, String lastDatasetName) {
        return !Validator.isBlank(datasetName) && !datasetName.equalsIgnoreCase(lastDatasetName);
    }

    /**
     * Processes a single row of dataset data by extracting regular field values and attribute information,
     * then populating the current dataset with this data.
     *
     * @param sheet the Excel sheet being processed
     * @param rowIndex the current row index being processed
     * @param dataset the dataset map to populate with extracted data
     * @param columnMapping the mapping of column names to their sheet indices
     * @param structure the file structure containing field definitions
     * @param fieldIndices the helper object containing special field column indices
     */
    @SuppressWarnings("unchecked")
    private void processDatasetRow(Sheet sheet, int rowIndex, Map<String, Object> dataset,
                                   Map<String, Integer> columnMapping, FileStructure structure,
                                   FieldIndices fieldIndices) {

        // Process regular fields using streams
        columnMapping.entrySet().stream()
                .filter(entry -> !fieldIndices.specialFields.contains(entry.getKey().toLowerCase()))
                .forEach(entry -> {
                    String field = entry.getKey();
                    int col = entry.getValue();
                    String value = cleanNumericValue(getMergedCellValue(sheet, rowIndex, col));
                    String originalFieldName = findOriginalFieldName(structure.getColumnNames(), field);
                    dataset.put(originalFieldName, value);
                });

        // Process attributes
        Map<String, String> attr = Map.of(
                fieldIndices.attributesField, getMergedCellValue(sheet, rowIndex, fieldIndices.attrCol).trim(),
                fieldIndices.attributeDescriptionField, getMergedCellValue(sheet, rowIndex, fieldIndices.attrDescCol).trim()
        );

        ((List<Map<String, String>>) dataset.get("attributes")).add(attr);
    }

    /**
     * Finds the original field name from the sample column names by matching
     * against the normalized mapped field name.
     *
     * @param sampleColumnNames list of original column names from the sample file
     * @param mappedFieldName the normalized field name used in column mapping
     * @return the original field name if found, otherwise returns the mapped field name
     */
    private String findOriginalFieldName(List<String> sampleColumnNames, String mappedFieldName) {
        return sampleColumnNames.stream()
                .filter(sampleField -> FileStructure.normalizeColumnName(sampleField).equals(mappedFieldName))
                .findFirst()
                .orElse(mappedFieldName);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;

        return IntStream.range(0, row.getLastCellNum())
                .mapToObj(row::getCell)
                .filter(Objects::nonNull)
                .noneMatch(cell -> cell.getCellType() != CellType.BLANK &&
                        !FileValidationUtil.getCellValue(cell).trim().isEmpty());
    }

    /**
     * Gets value from merged cell or regular cell.
     *
     * @param sheet the Excel sheet
     * @param rowIndex the row index
     * @param colIndex the column index
     * @return cell value as string
     */
    private String getMergedCellValue(Sheet sheet, int rowIndex, int colIndex) {
        if (colIndex < 0) return "";

        return IntStream.range(0, sheet.getNumMergedRegions())
                .mapToObj(sheet::getMergedRegion)
                .filter(region -> region.isInRange(rowIndex, colIndex))
                .findFirst()
                .map(region -> {
                    Row firstRow = sheet.getRow(region.getFirstRow());
                    return firstRow != null ? FileValidationUtil.getCellValue(firstRow.getCell(region.getFirstColumn())) : "";
                })
                .orElseGet(() -> {
                    Row row = sheet.getRow(rowIndex);
                    return row != null ? FileValidationUtil.getCellValue(row.getCell(colIndex)) : "";
                });
    }

    /**
     * Determines the number of header rows in the sample file by finding the first row
     * that contains any non-blank cell data.
     *
     * @param fileEntry the sample file entry to analyze for header row count
     * @return the number of header rows, or default count if no data found
     * @throws IOException if the file cannot be read
     * @throws PortalException if there's an error accessing the file content
     */
    private int getHeaderRowCount(FileEntry fileEntry) throws IOException, PortalException {
        try (InputStream is = fileEntry.getContentStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            return IntStream.rangeClosed(0, sheet.getLastRowNum())
                    .filter(i -> {
                        Row row = sheet.getRow(i);
                        if (row == null) return false;

                        // Check if any cell in this row is non-blank
                        return StreamSupport.stream(row.spliterator(), false)
                                .anyMatch(cell -> cell != null && cell.getCellType() != CellType.BLANK);
                    })
                    .boxed()
                    .findFirst()
                    .map(i -> i + 1)
                    .orElse(InventoryConstants.DEFAULT_HEADER_ROW_COUNT);
        }
    }

    /**
     * Cleans numeric string values by removing unnecessary decimal places.
     *
     * @param value the value to clean
     * @return cleaned value
     */
    private String cleanNumericValue(String value) {
        if (value != null && value.matches("\\d+\\.0")) {
            return value.substring(0, value.indexOf('.'));
        }
        return value != null ? value.trim() : "";
    }

    /**
     * Helper class for managing special field indices and providing convenient access
     * to column positions for dataset name, attributes, and attribute description fields.
     */
    private static class FieldIndices {
        final String attributesField;
        final String attributeDescriptionField;
        final int datasetCol;
        final int attrCol;
        final int attrDescCol;
        final Set<String> specialFields;

        /**
         * Constructs FieldIndices by extracting special field names from the structure
         * and mapping them to their corresponding column indices.
         *
         * @param structure the file structure containing special field definitions
         * @param columnMapping the mapping of normalized column names to indices
         */
        FieldIndices(FileStructure structure, Map<String, Integer> columnMapping) {
            this.attributesField = structure.getAttributesField();
            this.attributeDescriptionField = structure.getAttributeDescriptionField();

            String datasetNameField = structure.getDatasetNameField();
            this.datasetCol = columnMapping.getOrDefault(
                    datasetNameField != null ? datasetNameField.toLowerCase() : "", -1);
            this.attrCol = columnMapping.getOrDefault(
                    attributesField != null ? attributesField.toLowerCase() : "", -1);
            this.attrDescCol = columnMapping.getOrDefault(
                    attributeDescriptionField != null ? attributeDescriptionField.toLowerCase() : "", -1);

            this.specialFields = Set.of(
                    attributesField != null ? attributesField.toLowerCase() : "",
                    attributeDescriptionField != null ? attributeDescriptionField.toLowerCase() : ""
            );
        }
    }
}