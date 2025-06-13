package com.dx.liferay.inventory.util;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import org.osgi.service.component.annotations.Component;

import javax.portlet.ActionRequest;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Component(
        immediate = true,
        service = FormExtractionUtil.class
)
public class FormExtractionUtil {

    private static final Log _log = LogFactoryUtil.getLog(FormExtractionUtil.class);


    public List<Map<String, Object>> extractDatasetsFromRequest(ActionRequest actionRequest, String userLocale) {
        int expectedCount = ParamUtil.getInteger(actionRequest, "totalDatasets", 1);
        _log.info("Expected dataset count from frontend: " + expectedCount);

        Set<Integer> datasetIds = findDatasetIds(actionRequest);

        return datasetIds.stream()
                .map(datasetId -> extractSingleDataset(actionRequest, userLocale, datasetId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Extracts a single dataset's complete information including basic metadata,
     * prioritization details, release plan, and associated attributes.
     *
     * @param actionRequest the action request containing form parameters
     * @param userLocale the user's locale for internationalization
     * @param datasetId the unique identifier for the dataset to extract
     * @return map containing all dataset information, or null if dataset name is invalid
     */
    private Map<String, Object> extractSingleDataset(ActionRequest actionRequest, String userLocale, int datasetId) {
        String datasetName = ParamUtil.getString(actionRequest, "datasetName_" + datasetId);

        if (Validator.isNull(datasetName)) {
            return null;
        }

        Map<String, Object> datasetData = new HashMap<>();

        // Check for actual database ID (for updates)
        long actualDatasetId = ParamUtil.getLong(actionRequest, "actualDatasetId_" + datasetId, 0);
        _log.info("Actual dataset id: " + actualDatasetId);
        if (actualDatasetId > 0) {
            datasetData.put("actualDatasetId", actualDatasetId);
            datasetData.put("isExisting", true);
            _log.info("Dataset {} has actual ID: {}" + datasetId + " and "+ actualDatasetId);
        } else {
            datasetData.put("isExisting", false);
            _log.info("Dataset {} is new (no actual ID)" + datasetId);
        }

        // Basic information
        datasetData.put("userLocale", userLocale);
        datasetData.put("formDatasetId", datasetId); // Form index
        datasetData.put("datasetName", datasetName.trim());
        datasetData.put("datasetDescription", ParamUtil.getString(actionRequest, "datasetDescription_" + datasetId).trim());
        datasetData.put("datasetClassification", ParamUtil.getString(actionRequest, "datasetClassification_" + datasetId));

        // Data Prioritization Details - Benefits
        datasetData.put("userDemand", ParamUtil.getInteger(actionRequest, "userDemand_" + datasetId));
        datasetData.put("economicImpact", ParamUtil.getInteger(actionRequest, "economicImpact_" + datasetId));
        datasetData.put("betterServices", ParamUtil.getInteger(actionRequest, "betterServices_" + datasetId));
        datasetData.put("betterGovernance", ParamUtil.getInteger(actionRequest, "betterGovernance_" + datasetId));

        // Data Prioritization Details - Quality & Readiness
        datasetData.put("definedOwner", ParamUtil.getString(actionRequest, "definedOwner_" + datasetId));
        datasetData.put("existingMetadata", ParamUtil.getString(actionRequest, "existingMetadata_" + datasetId));
        datasetData.put("alreadyPublished", ParamUtil.getString(actionRequest, "alreadyPublished_" + datasetId));
        datasetData.put("openFormat", ParamUtil.getString(actionRequest, "openFormat_" + datasetId));

        // Dataset Release Plan
        datasetData.put("releaseYear", ParamUtil.getString(actionRequest, "releaseYear_" + datasetId));
        datasetData.put("releaseMonth", ParamUtil.getString(actionRequest, "releaseMonth_" + datasetId));

        // Extract attributes with database IDs
        List<Map<String, Serializable>> attributes = extractAttributesForDataset(actionRequest, datasetId);
        datasetData.put("attributes", attributes);

        _log.info("Extracted dataset " + datasetId + " (actualId: " + actualDatasetId + ") with " + attributes.size() + " attributes");
        return datasetData;
    }



    /**
     * Finds all dataset IDs present in the request by scanning parameter names
     * for the datasetName pattern and extracting the numeric IDs.
     *
     * @param actionRequest the action request containing form parameters
     * @return sorted set of dataset IDs found in the request parameters
     */
    private Set<Integer> findDatasetIds(ActionRequest actionRequest) {
        return Collections.list(actionRequest.getParameterNames()).stream()
                .filter(paramName -> paramName.startsWith("datasetName_"))
                .map(this::tryParseDatasetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Safely parses a dataset ID from a parameter name by extracting the numeric portion
     * after the "datasetName_" prefix.
     *
     * @param paramName the parameter name containing the dataset ID suffix
     * @return the parsed dataset ID, or null if parsing fails
     */
    private Integer tryParseDatasetId(String paramName) {
        try {
            String idPart = paramName.substring("datasetName_".length());
            return Integer.parseInt(idPart);
        } catch (NumberFormatException e) {
            _log.warn("Invalid dataset parameter name: " + paramName);
            return null;
        }
    }


    /**
     * Extracts all attributes associated with a specific dataset by finding
     * attribute indices and creating attribute maps for each.
     *
     * @param actionRequest the action request containing form parameters
     * @param datasetId the dataset ID for which to extract attributes
     * @return list of attribute maps containing name and description for each attribute
     */
    private List<Map<String, Serializable>> extractAttributesForDataset(ActionRequest actionRequest, int datasetId) {
        Set<Integer> attributeIndices = findAttributeIndicesForDataset(actionRequest, datasetId);
        _log.info("Found " + attributeIndices.size() + " attributes for dataset " + datasetId + ": " + attributeIndices);

        return attributeIndices.stream()
                .map(index -> createAttributeMap(actionRequest, datasetId, index))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Finds all attribute indices for a specific dataset by scanning parameter names
     * for the attributeName pattern and extracting the numeric indices.
     *
     * @param actionRequest the action request containing form parameters
     * @param datasetId the dataset ID for which to find attribute indices
     * @return sorted set of attribute indices found for the dataset
     */
    private Set<Integer> findAttributeIndicesForDataset(ActionRequest actionRequest, int datasetId) {
        String prefix = "attributeName_" + datasetId + "_";

        return Collections.list(actionRequest.getParameterNames()).stream()
                .filter(name -> name.startsWith(prefix))
                .map(name -> name.substring(prefix.length()))
                .mapToInt(indexStr -> {
                    try {
                        return Integer.parseInt(indexStr);
                    } catch (NumberFormatException e) {
                        _log.warn("Invalid attribute index: " + indexStr);
                        return -1;
                    }
                })
                .filter(index -> index >= 0)
                .boxed()
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Creates an attribute map containing name and description for a specific attribute
     * of a dataset, extracting the values from request parameters.
     *
     * @param request the action request containing form parameters
     * @param datasetId the dataset ID that owns the attribute
     * @param index the attribute index within the dataset
     * @return map containing attributeName and attributeDescription, or null if name is empty
     */
    private Map<String, Serializable> createAttributeMap(ActionRequest request, int datasetId, int index) {
        String name = ParamUtil.getString(request, "attributeName_" + datasetId + "_" + index);
        if (Validator.isNull(name)) return null;

        String description = ParamUtil.getString(request, "attributeDescription_" + datasetId + "_" + index);

        // Check for actual attribute ID
        long actualAttributeId = ParamUtil.getLong(request, "actualAttributeId_" + datasetId + "_" + index, 0);

        Map<String, Serializable> map = new HashMap<>();
        map.put("attributeName", name.trim());
        map.put("attributeDescription", Validator.isNull(description) ? "" : description.trim());
        map.put("formIndex", index);

        if (actualAttributeId > 0) {
            map.put("actualAttributeId", actualAttributeId);
            map.put("isExisting", true);
            _log.info("Attribute " + datasetId + "_" + index + " has actual ID: " + actualAttributeId);
        } else {
            map.put("isExisting", false);
            _log.info("Attribute " + datasetId + "_" + index + " is new (no actual ID)");
        }

        return map;
    }
}
