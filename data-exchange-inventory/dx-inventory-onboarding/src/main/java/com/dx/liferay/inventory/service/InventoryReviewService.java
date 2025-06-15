package com.dx.liferay.inventory.service;

import com.dx.liferay.inventory.constants.InventoryConstants;
import com.dx.liferay.inventory.util.InventoryHelper;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.petra.sql.dsl.DSLQueryFactoryUtil;
import com.liferay.petra.sql.dsl.Table;
import com.liferay.petra.sql.dsl.expression.Expression;
import com.liferay.petra.sql.dsl.query.DSLQuery;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.AuthTokenUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.WebKeys;
import org.osgi.service.component.annotations.*;

import javax.portlet.ActionRequest;
import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(service = InventoryReviewService.class)
public class InventoryReviewService {

    @Reference
    ObjectEntryLocalService _objectEntryLocalService;

    @Reference
    ObjectDefinitionLocalService _objectDefinitionLocalService;

    @Reference
    ObjectFieldLocalService _objectFieldLocalService;

    @Reference
    JSONFactory _jsonFactory;

    @Reference
    InventoryHelper _inventoryHelper;

    private static final Log _log = LogFactoryUtil.getLog(InventoryReviewService.class);

    /**
     * Retrieves complete inventory data including datasets and attributes.
     *
     * @param inventoryId the inventory ID to fetch
     * @param companyId the company ID
     * @return complete inventory data as JSON
     * @throws PortalException if any error occurs during data retrieval
     */
    public JSONObject getCompleteInventoryData(long inventoryId, long companyId) throws PortalException {

        final ObjectEntry inventoryEntry = _objectEntryLocalService.getObjectEntry(inventoryId);

        final JSONObject inventoryData = _jsonFactory.createJSONObject();
        inventoryData.put("inventoryId", inventoryId);
        inventoryData.put("inventoryName", _inventoryHelper.getInventoryName(inventoryEntry));

        final List<ObjectEntry> datasets = _inventoryHelper.getInventoryDatasetsList(inventoryEntry);
        final JSONArray datasetsArray = buildDatasetsArray(datasets, companyId);

        inventoryData.put("datasets", datasetsArray);
        inventoryData.put("datasetCount", datasets.size());

        return inventoryData;
    }

    /**
     * Safely retrieves ObjectEntry with error handling.
     *
     * @param objectEntryId the object entry ID
     * @return Optional containing the ObjectEntry if found
     */
    public Optional<ObjectEntry> getObjectEntrySecure(Long objectEntryId) {
        try {
            return Optional.of(_objectEntryLocalService.getObjectEntry(objectEntryId));
        } catch (PortalException e) {
            return Optional.empty();
        }
    }

    /**
     * Builds JSONArray of datasets with their attributes.
     *
     * @param datasets list of dataset entries
     * @param companyId the company ID
     * @return JSONArray containing dataset data
     */
    private JSONArray buildDatasetsArray(List<ObjectEntry> datasets, long companyId) {
        return datasets.stream()
                .map(dataset -> buildDatasetJson(dataset, companyId))
                .collect(Collector.of(
                        () -> _jsonFactory.createJSONArray(),
                        JSONArray::put,
                        (arr1, arr2) -> {
                            // Merge function for parallel streams if needed
                            for (int i = 0; i < arr2.length(); i++) {
                                arr1.put(arr2.get(i));
                            }
                            return arr1;
                        }
                ));
    }

    /**
     * Builds complete dataset JSON including basic info and attributes.
     *
     * @param dataset the dataset entry
     * @param companyId the company ID
     * @return JSON object containing dataset data
     */
    private JSONObject buildDatasetJson(ObjectEntry dataset, long companyId) {
        final JSONObject datasetJson = _jsonFactory.createJSONObject();

        try {
            final Map<String, Serializable> values = dataset.getValues();

            // Basic dataset information
            datasetJson.put("datasetId", dataset.getObjectEntryId());
            populateBasicDatasetFields(datasetJson, values);
            populatePrioritizationFields(datasetJson, values);
            populateQualityFields(datasetJson, values);
            populateReleasePlanFields(datasetJson, values);

            // Dataset attributes
            final JSONArray attributes = getDatasetAttributes(dataset.getObjectEntryId(), companyId);
            datasetJson.put("attributes", attributes);

        } catch (Exception e) {
            datasetJson.put("error", "Failed to load dataset data");
        }

        return datasetJson;
    }

    /**
     * Populates basic dataset fields (name, description, classification).
     */
    private void populateBasicDatasetFields(JSONObject datasetJson, Map<String, Serializable> values) {
        datasetJson.put("datasetName", extractTranslatableField(values, "datasetName"));
        datasetJson.put("datasetDescription", extractTranslatableField(values, "datasetDescription"));
        datasetJson.put("datasetClassification", extractSimpleField(values, "datasetClassification"));
    }

    /**
     * Populates prioritization fields (demand, impact, services, governance).
     */
    private void populatePrioritizationFields(JSONObject datasetJson, Map<String, Serializable> values) {
        Stream.of("userDemand", "economicImpact", "betterServices", "betterGovernance")
                .forEach(field -> datasetJson.put(field, extractSimpleField(values, field)));
    }

    /**
     * Populates quality and readiness fields.
     */
    private void populateQualityFields(JSONObject datasetJson, Map<String, Serializable> values) {
        Stream.of("definedOwner", "existingMetadata", "alreadyPublished", "openFormat")
                .forEach(field -> datasetJson.put(field, extractSimpleField(values, field)));
    }

    /**
     * Populates release plan fields.
     */
    private void populateReleasePlanFields(JSONObject datasetJson, Map<String, Serializable> values) {
        datasetJson.put("releaseYear", extractTranslatableField(values, "releaseYear"));
        datasetJson.put("releaseMonth", extractTranslatableField(values, "releaseMonth"));
    }

    /**
     * Retrieves attributes for a specific dataset using optimized query.
     *
     * @param datasetId the dataset ID
     * @param companyId the company ID
     * @return JSONArray of attributes
     */
    private JSONArray getDatasetAttributes(long datasetId, long companyId) {
        try {
            final List<ObjectEntry> attributes = queryDatasetAttributes(datasetId, companyId);

            return attributes.stream()
                    .map(this::buildAttributeJson)
                    .collect(Collector.of(
                            () -> _jsonFactory.createJSONArray(),
                            JSONArray::put,
                            (arr1, arr2) -> {
                                for (int i = 0; i < arr2.length(); i++) {
                                    arr1.put(arr2.get(i));
                                }
                                return arr1;
                            }
                    ));

        } catch (Exception e) {
            _log.error("Error retrieving attributes for dataset ID:");
            return _jsonFactory.createJSONArray();
        }
    }

    /**
     * Queries dataset attributes using DSL.
     */
    public List<ObjectEntry> queryDatasetAttributes(long datasetId,long companyId)
            throws PortalException {

        final ObjectDefinition attributeDefinition = _objectDefinitionLocalService
                .fetchObjectDefinition(companyId, InventoryConstants.DX_INVENTORY_ATTRIBUTE_OBJECT_NAME);

        final ObjectField relationshipField = _objectFieldLocalService.getObjectField(
                attributeDefinition.getObjectDefinitionId(),
                InventoryConstants.DX_INVENTORY_ATTRIBUTE_RELATIONSHIP_ID
        );

        final Table<?> relationshipTable = _objectFieldLocalService.getTable(
                attributeDefinition.getObjectDefinitionId(),
                relationshipField.getName()
        );

        final Expression<Long> datasetIdExpression = relationshipTable.getColumn(
                relationshipField.getDBColumnName(), Long.class
        );

        final DSLQuery attributesQuery = DSLQueryFactoryUtil
                .select(relationshipTable.getColumn("c_datasetInventoryAttributeDetailsId_", Long.class))
                .from(relationshipTable)
                .where(datasetIdExpression.eq(datasetId));

        final List<Long> attributeIds = _objectEntryLocalService.dslQuery(attributesQuery);

        return attributeIds.stream()
                .map(this::getObjectEntrySecure)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Builds attribute JSON object.
     */
    private JSONObject buildAttributeJson(ObjectEntry attribute) {
        final JSONObject attributeJson = _jsonFactory.createJSONObject();
        final Map<String, Serializable> values = attribute.getValues();

        attributeJson.put("attributeId", attribute.getObjectEntryId());
        attributeJson.put("attributeName", extractTranslatableField(values, "attribute"));
        attributeJson.put("attributeDescription", extractTranslatableField(values, "attributeDescription"));

        return attributeJson;
    }

    /**
     * Extracts translatable field value with null safety.
     */
    private String extractTranslatableField(Map<String, Serializable> values, String fieldName) {
        return Optional.ofNullable(values.get(fieldName + "_i18n"))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(i18nMap -> Stream.of("en_US", "ar_SA")
                        .map(locale -> (String) i18nMap.get(locale))
                        .filter(Objects::nonNull)
                        .filter(value -> !value.trim().isEmpty())
                        .findFirst()
                        .orElse(""))
                .orElseGet(() -> Optional.ofNullable(values.get(fieldName))
                        .map(Object::toString)
                        .filter(value -> !value.trim().isEmpty())
                        .orElse(""));
    }

    /**
     * Extracts simple field value with null safety.
     */
    private String extractSimpleField(Map<String, Serializable> values, String fieldName) {
        return Optional.ofNullable(values.get(fieldName))
                .map(Object::toString)
                .filter(value -> !value.trim().isEmpty())
                .orElse("");
    }



//    public String createDraftViaGraphQL(String inventoryName, int statusCode, ActionRequest actionRequest) throws Exception {
//        String csrfToken = AuthTokenUtil.getToken(PortalUtil.getHttpServletRequest(actionRequest));
//
//        // GraphQL mutation
//        String query = """
//            mutation {
//              c {
//                createInventoryDetails(
//                  InventoryDetails: {
//                    inventoryName: "%s",
//                    statusCode: %d
//                  }
//                ) {
//                  id
//                  inventoryName
//                  statusCode
//                }
//              }
//            }
//        """.formatted(inventoryName.replace("\"", "\\\""), statusCode);
//
//        // Build JSON body
//        JSONObject payload = JSONFactoryUtil.createJSONObject();
//        payload.put("query", query);
//
//        // Execute the GraphQL mutation using the working method
//        return executeGraphQLMutation(payload, csrfToken, actionRequest);
//    }

    public String executeInventoryGraphQLMutation(String mutationType, Long inventoryId, String inventoryName, int statusCode, ActionRequest actionRequest) throws Exception {
        String csrfToken = AuthTokenUtil.getToken(PortalUtil.getHttpServletRequest(actionRequest));

        String query;
        if ("create".equals(mutationType)) {
            query = """
            mutation {
              c {
                createInventoryDetails(
                  InventoryDetails: {
                    inventoryName: "%s",
                    statusCode: %d
                  }
                ) {
                  id
                  inventoryName
                  status
                }
              }
            }
        """.formatted(inventoryName.replace("\"", "\\\""), statusCode);
        } else if ("update".equals(mutationType)) {
            query = """
            mutation {
              c {
                updateInventoryDetails(
                  inventoryDetailsId: %d,
                  InventoryDetails: {
                    inventoryName: "%s",
                    statusCode: %d
                  }
                ) {
                  id
                  inventoryName
                  status
                }
              }
            }
        """.formatted(inventoryId, inventoryName.replace("\"", "\\\""), statusCode);
        } else {
            throw new IllegalArgumentException("Invalid mutation type: " + mutationType);
        }

        JSONObject payload = JSONFactoryUtil.createJSONObject();
        payload.put("query", query);
        _log.info("GraphQL " + mutationType + " query: " + payload);

        return executeGraphQLMutation(payload, csrfToken, actionRequest);
    }

    private String executeGraphQLMutation(JSONObject requestBody, String csrfToken, PortletRequest portletRequest) throws IOException, URISyntaxException {
        HttpServletRequest httpServletRequest = PortalUtil.getHttpServletRequest(portletRequest);
        HttpSession session = httpServletRequest.getSession(false);
        String jsessionId = session != null ? session.getId() : null;
        String companyId = String.valueOf(PortalUtil.getCompanyId(httpServletRequest));
        String guestLanguageId = (String) httpServletRequest.getSession().getAttribute("GUEST_LANGUAGE_ID");

        // Build proper cookie header (this is the key that was missing!)
        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append("JSESSIONID=").append(jsessionId).append("; ");
        cookieHeader.append("COOKIE_SUPPORT=true; ");
        cookieHeader.append("GUEST_LANGUAGE_ID=").append(guestLanguageId != null ? guestLanguageId : "en_US").append("; ");
        cookieHeader.append("COMPANY_ID=").append(companyId).append(";");

        String graphqlEndpoint = getGraphQLEndpoint((ActionRequest) portletRequest);
        URI url = new URI(graphqlEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("x-csrf-token", csrfToken);
        connection.setRequestProperty("Cookie", cookieHeader.toString());
        connection.setDoOutput(true);

        // Send request body
        if (requestBody != null && connection.getDoOutput()) {
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        // Read response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        return response.toString();
    }

    private String getGraphQLEndpoint(ActionRequest actionRequest) {
        ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
        String portalURL = themeDisplay.getPortalURL();
        return portalURL + "/o/graphql";
    }

}

