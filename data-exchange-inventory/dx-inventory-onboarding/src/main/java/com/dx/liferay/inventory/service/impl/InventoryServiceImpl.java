package com.dx.liferay.inventory.service.impl;

import com.dx.liferay.inventory.constants.InventoryConstants;
import com.dx.liferay.inventory.service.InventoryReviewService;
import com.dx.liferay.inventory.service.InventoryService;
import com.dx.liferay.inventory.util.InventoryHelper;
import com.liferay.object.model.*;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.petra.sql.dsl.DSLQueryFactoryUtil;
import com.liferay.petra.sql.dsl.Table;
import com.liferay.petra.sql.dsl.expression.Expression;
import com.liferay.petra.sql.dsl.query.DSLQuery;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.search.experiences.rest.dto.v1_0.In;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.ActionRequest;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Implementation of the InventoryService interface that manages inventory datasets and their attributes
 * using Liferay's Object framework.
 */
@Component(
        immediate = true,
        service = InventoryService.class
)
public class InventoryServiceImpl implements InventoryService {

    private static final Log _log = LogFactoryUtil.getLog(InventoryServiceImpl.class);

    ServiceContext serviceContext = new ServiceContext();

    @Override
    public ObjectEntry addInventory(long companyId, long userId, String userLocale, boolean isDraft, ActionRequest actionRequest) throws Exception {
        ObjectDefinition inventoryObjectDefinition =
                _objectDefinitionLocalService.fetchObjectDefinition(companyId, InventoryConstants.DX_INVENTORY_PARENT_OBJECT_NAME);

        String inventoryName = generateNextInventoryName(companyId);

        Map<String, Serializable> values = new HashMap<>();
        addTranslatableFieldToMap(values, "inventoryName", inventoryName, userLocale);

        ObjectEntry objectEntry;

        if (!isDraft) {
             objectEntry = _objectEntryLocalService.addObjectEntry(
                    userId, 0, inventoryObjectDefinition.getObjectDefinitionId(), values, new ServiceContext());
        }else{
            String responseJson =  _inventoryReviewService.createDraftViaGraphQL(inventoryName, actionRequest);
            long draftId = extractIdFromGraphQLResponse(responseJson);
            objectEntry = _objectEntryLocalService.getObjectEntry(draftId);
        }

        return objectEntry;
    }
    private long extractIdFromGraphQLResponse(String jsonResponse) throws JSONException {
        JSONObject jsonObject = JSONFactoryUtil.createJSONObject(jsonResponse);

        if (!jsonObject.has("data")) {
            _log.error("GraphQL response missing 'data': " + jsonResponse);
            return 0;
        }

        JSONObject data = jsonObject.getJSONObject("data");
        if (data == null || !data.has("c")) {
            _log.error("GraphQL response missing 'c': " + jsonResponse);
            return 0;
        }

        JSONObject c = data.getJSONObject("c");
        if (c == null || !c.has("createInventoryDetails")) {
            _log.error("GraphQL response missing 'createInventoryDetails': " + jsonResponse);
            return 0;
        }

        JSONObject createInventoryDetails = c.getJSONObject("createInventoryDetails");
        if (createInventoryDetails == null || !createInventoryDetails.has("id")) {
            _log.error("GraphQL response missing 'id': " + jsonResponse);
            return 0;
        }

        return createInventoryDetails.getLong("id");
    }

    private String generateNextInventoryName(long companyId) throws PortalException {
        ObjectDefinition inventoryObjectDefinition =
                _objectDefinitionLocalService.fetchObjectDefinition(companyId, InventoryConstants.DX_INVENTORY_PARENT_OBJECT_NAME);

        int count = _objectEntryLocalService.getObjectEntriesCount(0, inventoryObjectDefinition.getObjectDefinitionId());

        return "Data Inventory " + (count + 1);
    }

    /**
     * Creates a new dataset object entry in the inventory system using Liferay's Object framework.
     * Retrieves the inventory object definition and creates an entry with the provided values.
     *
     * @param companyId the company ID where the dataset will be created
     * @param inventoryValues map containing the dataset field values including translatable and non-translatable fields
     * @return the created ObjectEntry representing the new dataset with its assigned ID
     */
    @Override
    public ObjectEntry addDataSetObjectEntry(long companyId, long userId, Map<String, Serializable> inventoryValues) throws PortalException {

        ObjectDefinition inventoryDatasetObjectDefinition = _objectDefinitionLocalService.fetchObjectDefinition(
                companyId, InventoryConstants.DX_INVENTORY_OBJECT_NAME);

        // Create Inventory Onboarding entry
        return _objectEntryLocalService.addObjectEntry(
                userId,
                0,
                inventoryDatasetObjectDefinition.getObjectDefinitionId(),
                inventoryValues,
                serviceContext
        );
    }

    /**
     * Creates multiple attribute entries associated with a specific dataset inventory entry.
     * Each attribute includes translatable fields for name and description.
     *
     * @param companyId the company ID where the attributes will be created
     * @param entryValuesList list of maps containing attribute names, descriptions, and metadata
     * @param datasetInventoryEntryId the ID of the inventory dataset entry to link attributes to
     * @param userLocale the user's locale for proper internationalization of translatable fields
     */
    @Override
    public void addDatasetInventoryAttributesEntry(long companyId, long userId, List<Map<String, Serializable>> entryValuesList, long datasetInventoryEntryId, String userLocale) throws PortalException {

        ObjectDefinition datasetInventoryAttributeObjectDefinition = _objectDefinitionLocalService.fetchObjectDefinition(
                companyId, InventoryConstants.DX_INVENTORY_ATTRIBUTE_OBJECT_NAME);


        // Create attribute entries
        for (Map<String, Serializable> attributeValues : entryValuesList) {
            // Create a new map for the actual object entry
            Map<String, Serializable> objectValues = new HashMap<>();

            String attributeName = (String) attributeValues.get("attributeName");
            String attributeDescription = (String) attributeValues.get("attributeDescription");

            // Use helper method for translatable attribute fields
            addTranslatableFieldToMap(objectValues, "attribute", attributeName, userLocale);
            addTranslatableFieldToMap(objectValues, "attributeDescription", attributeDescription, userLocale);

            // Add reference to parent inventory entry
            objectValues.put(InventoryConstants.DX_INVENTORY_ATTRIBUTE_RELATIONSHIP_ID, datasetInventoryEntryId);

            ObjectEntry attributeEntry = _objectEntryLocalService.addObjectEntry(
                     userId,
                    0,
                    datasetInventoryAttributeObjectDefinition.getObjectDefinitionId(),
                    objectValues,
                    serviceContext
            );

            _log.info("Attribute entry created with ID: " + attributeEntry.getObjectEntryId() + " for attribute: " + attributeName);
        }
    }

    /**
     * Processes and creates multiple datasets in a single bulk operation, handling both
     * dataset creation and their associated attributes.
     *
     * @param companyId the company ID where the datasets will be created
     * @param datasets list of dataset maps, each containing dataset information, attributes, and locale data
     */
    @Override
    @SuppressWarnings("unchecked")
    public void addMultipleDatasets(long companyId, long userId, List<Map<String, Object>> datasets, long parentInventoryId) throws PortalException {
        if (datasets == null || datasets.isEmpty()) {
            _log.warn("No datasets to process");
            return;
        }
        for (Map<String, Object> datasetData : datasets) {
            try {
                // Create the main dataset entry
                Map<String, Serializable> inventoryValues = buildInventoryValues(datasetData, parentInventoryId);

                ObjectEntry datasetEntry = addDataSetObjectEntry(companyId, userId, inventoryValues);

                // Add attributes for this dataset
                List<Map<String, Serializable>> attributes = (List<Map<String, Serializable>>) datasetData.get("attributes");
                if (attributes != null && !attributes.isEmpty()) {
                    String userLocale = (String) datasetData.get("userLocale");

                    addDatasetInventoryAttributesEntry(companyId, userId, attributes, datasetEntry.getObjectEntryId(), userLocale);
                } else {
                    _log.info("No attributes found for dataset: " + datasetData.get("datasetName"));
                }
            } catch (Exception e) {
                _log.error("Error processing dataset: " + datasetData.get("datasetName") + " - " + e.getMessage(), e);
            }
        }
    }

    /**
     * Builds a map of inventory values from raw dataset data, handling both translatable
     * and non-translatable fields with proper data validation, cleaning, and type conversion.
    *
     * @param datasetData map containing raw dataset information from form submission or file parsing
     * @return map of properly formatted and validated inventory values ready for object entry creation
     */
    private Map<String, Serializable> buildInventoryValues(Map<String, Object> datasetData, Long parentInventoryId) throws PortalException {
        Map<String, Serializable> inventoryValues = new HashMap<>();

        String userLocale = (String) datasetData.get("userLocale");

        // Handle TRANSLATABLE fields
        addTranslatableFieldToMap(inventoryValues, "datasetName", (String) datasetData.get("datasetName"), userLocale);
        addTranslatableFieldToMap(inventoryValues, "datasetDescription", (String) datasetData.get("datasetDescription"), userLocale);

        // Handle NON-TRANSLATABLE fields
        String datasetClassification = (String) datasetData.get("datasetClassification");
        if (datasetClassification != null && !datasetClassification.trim().isEmpty() && !datasetClassification.equals("Choose Option")) {
            inventoryValues.put("datasetClassification", datasetClassification.trim());
        }

        // Data Prioritization Details - Benefits
        addIntegerValueIfValid(inventoryValues, datasetData, "userDemand");
        addIntegerValueIfValid(inventoryValues, datasetData, "economicImpact");
        addIntegerValueIfValid(inventoryValues, datasetData, "betterServices");
        addIntegerValueIfValid(inventoryValues, datasetData, "betterGovernance");

        // Data Prioritization Details - Quality & Readiness
        addStringValueIfNotEmpty(inventoryValues, datasetData, "definedOwner");
        addStringValueIfNotEmpty(inventoryValues, datasetData, "existingMetadata");
        addStringValueIfNotEmpty(inventoryValues, datasetData, "alreadyPublished");
        addStringValueIfNotEmpty(inventoryValues, datasetData, "openFormat");

        // Dataset Release Plan - TRANSLATABLE fields
        addTranslatableFieldToMap(inventoryValues, "releaseYear", (String) datasetData.get("releaseYear"), userLocale);
        addTranslatableFieldToMap(inventoryValues, "releaseMonth", (String) datasetData.get("releaseMonth"), userLocale);

        if(parentInventoryId != null) {
            inventoryValues.put(InventoryConstants.DX_INVENTORY_DATASET_RELATIONSHIP_ID, parentInventoryId);
        }
        return inventoryValues;
    }

    /**
     * Helper method to add translatable fields to a target map with proper internationalization support.
     * Creates both the base field value and the _i18n map containing locale-specific translations.
     * Used for fields that need to support multiple languages and localization.
     *
     * @param targetMap the map to add the translatable field to (inventory values or attribute values)
     * @param fieldName the name of the field to add (without _i18n suffix)
     * @param value the field value in the user's locale
     * @param userLocale the user's locale code for internationalization mapping
     */
    private void addTranslatableFieldToMap(Map<String, Serializable> targetMap, String fieldName, String value, String userLocale) {
        if (value != null && !value.trim().isEmpty()) {
            String trimmedValue = value.trim();

            // Set the default field value
            targetMap.put(fieldName, trimmedValue);

            // Set the _i18n field with locale map
            Map<String, String> i18nMap = new HashMap<>();
            i18nMap.put(userLocale, trimmedValue);
            targetMap.put(fieldName + "_i18n", (Serializable) i18nMap);

        } else {
            _log.info(fieldName + ": (empty or null)");
        }
    }


    @Override
    public void updateMultipleDatasets(long companyId, long userId, long inventoryId,
                                       List<Map<String, Object>> submittedDatasets, String userLocale, boolean isDraft)
            throws PortalException {

        if (submittedDatasets == null || submittedDatasets.isEmpty()) {
            _log.warn("No datasets provided for update - inventory: " + inventoryId);
            return;
        }

        try {
            // Get existing datasets
            ObjectEntry inventoryEntry = _objectEntryLocalService.getObjectEntry(inventoryId);
            List<ObjectEntry> existingDatasets = _inventoryHelper.getInventoryDatasetsList(inventoryEntry);

            // Create a map of existing datasets by ID for quick lookup
            Map<Long, ObjectEntry> existingDatasetsMap = existingDatasets.stream()
                    .collect(Collectors.toMap(ObjectEntry::getObjectEntryId, Function.identity()));

            // Track which existing datasets are being updated (so we know which ones to delete)
            Set<Long> processedDatasetIds = new HashSet<>();

            // Process each submitted dataset
            for (Map<String, Object> submittedDataset : submittedDatasets) {
                try {
                    processSubmittedDataset(companyId, userId, inventoryId, submittedDataset,
                            existingDatasetsMap, processedDatasetIds, userLocale);
                } catch (Exception e) {
                    _log.error("Error processing submitted dataset: " + submittedDataset.get("datasetName"), e);
                    throw new PortalException("Failed to process dataset: " + submittedDataset.get("datasetName"), e);
                }
            }

            // Delete datasets that were not in the submitted list
            deleteUnprocessedDatasets(existingDatasetsMap.keySet(), processedDatasetIds,companyId);

            _log.info("Successfully updated inventory " + inventoryId + " - Processed: " +
                    processedDatasetIds.size() + ", Deleted: " +
                    (existingDatasetsMap.size() - processedDatasetIds.size()));

        } catch (Exception e) {
            _log.error("Failed to update inventory datasets for inventory: " + inventoryId, e);
            throw new PortalException("Failed to update inventory datasets", e);
        }
    }

    private void processSubmittedDataset(long companyId, long userId, long inventoryId,
                                         Map<String, Object> submittedDataset,
                                         Map<Long, ObjectEntry> existingDatasetsMap,
                                         Set<Long> processedDatasetIds, String userLocale)
            throws PortalException {

        Long actualDatasetId = (Long) submittedDataset.get("actualDatasetId");
        Boolean isExisting = (Boolean) submittedDataset.get("isExisting");

        if (isExisting != null && actualDatasetId != null && actualDatasetId > 0) {
            // UPDATE existing dataset
            ObjectEntry existingDataset = existingDatasetsMap.get(actualDatasetId);
            if (existingDataset != null) {
                updateExistingDataset(companyId, userId, existingDataset, submittedDataset, userLocale);
                processedDatasetIds.add(actualDatasetId);
                _log.info("Updated dataset ID: "+ actualDatasetId);
            } else {
                _log.warn("Dataset with ID {} not found in existing datasets, creating new one" + actualDatasetId);
                createNewDataset(companyId, userId, inventoryId, submittedDataset, userLocale);
            }
        } else {
            // CREATE new dataset
            createNewDataset(companyId, userId, inventoryId, submittedDataset, userLocale);
            _log.info("Created new dataset:  "+ submittedDataset.get("datasetName"));
        }
    }

    private void deleteUnprocessedDatasets(Set<Long> existingDatasetIds, Set<Long> processedDatasetIds, long companyId) {
        existingDatasetIds.stream()
                .filter(id -> !processedDatasetIds.contains(id))
                .forEach(datasetId -> {
                    try {
                        deleteAttributesWithDataset(datasetId,companyId);
                        _log.info("Deleted unprocessed dataset ID: "+ datasetId);
                    } catch (Exception e) {
                        _log.info("Error deleting dataset ID: " + datasetId, e);
                    }
                });
    }

    private void updateExistingDataset(long companyId, long userId, ObjectEntry existingDataset,
                                       Map<String, Object> submittedData, String userLocale)
            throws PortalException {

        long datasetId = existingDataset.getObjectEntryId();

        // Update main dataset fields
        Map<String, Serializable> updatedValues = buildInventoryValues(submittedData, null);
        _objectEntryLocalService.updateObjectEntry(
                userId,
                datasetId,
                updatedValues,
                new ServiceContext()
        );

        // Update attributes using ID-based approach
        updateDatasetAttributes(companyId, userId, datasetId, submittedData, userLocale);
    }


    private void createNewDataset(long companyId, long userId, long inventoryId,
                                  Map<String, Object> submittedData, String userLocale)
            throws PortalException {

        // Create main dataset
        Map<String, Serializable> values = buildInventoryValues(submittedData, inventoryId);
        ObjectEntry newDataset = addDataSetObjectEntry(companyId, userId, values);

        // Create attributes
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> attributes =
                (List<Map<String, Serializable>>) submittedData.get("attributes");

        if (attributes != null && !attributes.isEmpty()) {
            addDatasetInventoryAttributesEntry(companyId, userId,attributes,
                    newDataset.getObjectEntryId(), userLocale);
        }
    }

    private void deleteAttributesWithDataset(long datasetId, long companyId) throws PortalException {
        try {

            List<ObjectEntry> attributes = _inventoryReviewService.queryDatasetAttributes(datasetId,companyId);

            List<Long> attributeIds = attributes.stream()
                    .map(ObjectEntry::getObjectEntryId)
                    .toList();

            // Delete each attribute
            for (Long attributeId : attributeIds) {
                try {
                    _objectEntryLocalService.deleteObjectEntry(attributeId);
                    _log.info("Deleted attribute: " + attributeId);
                } catch (Exception e) {
                    _log.error("Error deleting attribute: " + attributeId, e);
                }
            }
            //delete dataset
            _objectEntryLocalService.deleteObjectEntry(datasetId);

        } catch (Exception e) {
            _log.error("Error deleting attributes for dataset: " + datasetId, e);
        }
    }

    private void updateDatasetAttributes(long companyId, long userId, long datasetId,
                                         Map<String, Object> submittedData, String userLocale)
            throws PortalException {

        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> submittedAttributes =
                (List<Map<String, Serializable>>) submittedData.get("attributes");

        if (submittedAttributes == null) {
            submittedAttributes = new ArrayList<>();
        }

        // Get existing attributes for this dataset
        List<ObjectEntry> existingAttributes = _inventoryReviewService.queryDatasetAttributes(datasetId,companyId);

        // Create map of existing attributes by ID
        Map<Long, ObjectEntry> existingAttributesMap = existingAttributes.stream()
                .collect(Collectors.toMap(ObjectEntry::getObjectEntryId, Function.identity()));

        // Track processed attribute IDs
        Set<Long> processedAttributeIds = new HashSet<>();

        // Process each submitted attribute
        for (Map<String, Serializable> submittedAttribute : submittedAttributes) {
            try {
                processSubmittedAttribute(companyId, userId, datasetId, submittedAttribute,
                        existingAttributesMap, processedAttributeIds, userLocale);
            } catch (Exception e) {
                _log.error("Error processing attribute: " + submittedAttribute.get("attributeName"), e);
                // Continue with other attributes instead of failing completely
            }
        }

        // Delete attributes that were not in the submitted list
        deleteUnprocessedAttributes(existingAttributesMap.keySet(), processedAttributeIds);
    }

    private void processSubmittedAttribute(long companyId, long userId, long datasetId,
                                           Map<String, Serializable> submittedAttribute,
                                           Map<Long, ObjectEntry> existingAttributesMap,
                                           Set<Long> processedAttributeIds, String userLocale)
            throws PortalException {

        Long actualAttributeId = (Long) submittedAttribute.get("actualAttributeId");
        Boolean isExisting = (Boolean) submittedAttribute.get("isExisting");

        if (isExisting != null  && actualAttributeId != null && actualAttributeId > 0) {
            // UPDATE existing attribute
            ObjectEntry existingAttribute = existingAttributesMap.get(actualAttributeId);
            if (existingAttribute != null) {
                updateExistingAttribute(existingAttribute, userId, submittedAttribute, userLocale);
                processedAttributeIds.add(actualAttributeId);
            } else {
                _log.warn("Attribute with ID  not found, creating new one" + actualAttributeId);
                createNewAttribute(companyId, userId, datasetId, submittedAttribute, userLocale);
            }
        } else {
            // CREATE new attribute
            createNewAttribute(companyId, userId, datasetId, submittedAttribute, userLocale);
        }
    }

    private void deleteUnprocessedAttributes(Set<Long> existingAttributeIds, Set<Long> processedAttributeIds) {
        existingAttributeIds.stream()
                .filter(id -> !processedAttributeIds.contains(id))
                .forEach(attributeId -> {
                    try {
                        _objectEntryLocalService.deleteObjectEntry(attributeId);
                        _log.info("Deleted unprocessed attribute ID: "+ attributeId);
                    } catch (Exception e) {
                        _log.error("Error deleting attribute ID: " + attributeId, e);
                    }
                });
    }


    private void updateExistingAttribute(ObjectEntry existingAttribute, long userId,
                                         Map<String, Serializable> submittedAttribute, String userLocale)
            throws PortalException {

        Map<String, Serializable> attributeValues = new HashMap<>();

        String attributeName = (String) submittedAttribute.get("attributeName");
        String attributeDescription = (String) submittedAttribute.get("attributeDescription");

        addTranslatableFieldToMap(attributeValues, "attribute", attributeName, userLocale);
        addTranslatableFieldToMap(attributeValues, "attributeDescription", attributeDescription, userLocale);

        _objectEntryLocalService.updateObjectEntry(
                userId,
                existingAttribute.getObjectEntryId(),
                attributeValues,
                new ServiceContext()
        );
    }

    private void createNewAttribute(long companyId, long userId, long datasetId,
                                    Map<String, Serializable> attributeData, String userLocale)
            throws PortalException {

        ObjectDefinition attributeDefinition = _objectDefinitionLocalService.fetchObjectDefinition(
                companyId, InventoryConstants.DX_INVENTORY_ATTRIBUTE_OBJECT_NAME);

        Map<String, Serializable> attributeValues = new HashMap<>();

        String attributeName = (String) attributeData.get("attributeName");
        String attributeDescription = (String) attributeData.get("attributeDescription");

        addTranslatableFieldToMap(attributeValues, "attribute", attributeName, userLocale);
        addTranslatableFieldToMap(attributeValues, "attributeDescription", attributeDescription, userLocale);
        attributeValues.put(InventoryConstants.DX_INVENTORY_ATTRIBUTE_RELATIONSHIP_ID, datasetId);

        _objectEntryLocalService.addObjectEntry(userId, 0,
                attributeDefinition.getObjectDefinitionId(),
                attributeValues,
                new ServiceContext()
        );
    }



    @Override
    public List<Map<String, Object>> getInventoryEntries(long companyId) throws PortalException {
        List<Map<String, Object>> inventoryList = new ArrayList<>();

        try {
            // Get the inventory object definition
            ObjectDefinition inventoryObjectDefinition = _objectDefinitionLocalService.fetchObjectDefinition(
                    companyId, InventoryConstants.DX_INVENTORY_PARENT_OBJECT_NAME);

            if (inventoryObjectDefinition == null) {
                _log.error("Inventory object definition not found for company: " + companyId);
                return inventoryList;
            }

            // Fetch all object entries
            List<ObjectEntry> objectEntries = _objectEntryLocalService.getObjectEntries(0, inventoryObjectDefinition.getObjectDefinitionId(),
                    QueryUtil.ALL_POS,
                    QueryUtil.ALL_POS);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");

            // Convert each ObjectEntry to simple map
            for (ObjectEntry objectEntry : objectEntries) {
                Map<String, Object> inventoryData = new HashMap<>();

              String inventoryName = _inventoryHelper.getInventoryName(objectEntry);
              int datasetCount = _inventoryHelper.getInventoryDatasetCount(objectEntry);
              String status = _inventoryHelper.getInventoryStatus(objectEntry);
                inventoryData.put("id", objectEntry.getObjectEntryId());
                inventoryData.put("name", inventoryName);
                inventoryData.put("datasetCount", datasetCount);
                inventoryData.put("submittedDate", "Submitted: " + dateFormat.format(objectEntry.getCreateDate()));
                inventoryData.put("status", status);
                inventoryData.put("userId", objectEntry.getUserId());

                inventoryList.add(inventoryData);
            }

        } catch (Exception e) {
            _log.error("Error getting inventory entries", e);
            throw new PortalException("Failed to get inventory entries", e);
        }

        return inventoryList;
    }

    /**
     * Helper method to safely add string values to the inventory map only if they are not empty
     * or default placeholder values. Performs trimming and validation to ensure clean data storage.
     *
     * @param inventoryValues the inventory values map to add the field to
     * @param datasetData the source dataset data map containing the raw value
     * @param key the field key to extract from datasetData and add to inventoryValues
     */
    private void addStringValueIfNotEmpty(Map<String, Serializable> inventoryValues, Map<String, Object> datasetData, String key) {
        String value = (String) datasetData.get(key);
        if (value != null && !value.trim().isEmpty() && !value.equals("Choose Option")) {
            inventoryValues.put(key, value.trim());
        } else {
            _log.info(key + ": (empty or null)");
        }
    }

    private void addIntegerValueIfValid(Map<String, Serializable> inventoryValues, Map<String, Object> datasetData, String key) {
        Integer value = (Integer) datasetData.get(key);
        if (value != null && value > 0) {
            inventoryValues.put(key, value);
        }
    }

    @Reference
    InventoryHelper _inventoryHelper;

    @Reference
    InventoryReviewService _inventoryReviewService;

    @Reference
    ObjectDefinitionLocalService _objectDefinitionLocalService;

    @Reference
    ObjectEntryLocalService _objectEntryLocalService;

}