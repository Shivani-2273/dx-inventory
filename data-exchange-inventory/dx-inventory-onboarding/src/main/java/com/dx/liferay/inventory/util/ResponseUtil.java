package com.dx.liferay.inventory.util;

import com.dx.liferay.inventory.constants.InventoryConstants;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;

import javax.portlet.ResourceResponse;
import java.io.IOException;
import java.util.List;

/**
 * Utility class for handling JSON responses in portlet operations.
 *
 * Provides centralized methods for creating and writing JSON responses
 * to eliminate code duplication across services and portlets.
 */
public class ResponseUtil {

    /**
     * Writes JSON response to resource response.
     *
     * @param response the resource response to write to
     * @param json the JSON object to write
     * @throws IOException if writing fails
     */
    public static void writeJsonResponse(ResourceResponse response, JSONObject json) throws IOException {
        response.setContentType(InventoryConstants.JSON_CONTENT_TYPE);
        response.setCharacterEncoding(InventoryConstants.DEFAULT_ENCODING);
        response.getWriter().write(json.toString());
    }

    /**
     * Writes error response as JSON.
     *
     * @param resourceResponse the resource response to write to
     * @param errorMessage the error message
     * @param jsonFactory the JSON factory for creating objects
     * @throws IOException if writing fails
     */
    public static void writeErrorResponse(ResourceResponse resourceResponse, String errorMessage,
                                          JSONFactory jsonFactory) throws IOException {
        JSONObject errorJson = jsonFactory.createJSONObject();
        errorJson.put("success", false);
        errorJson.put("error", errorMessage);
        writeJsonResponse(resourceResponse, errorJson);
    }

    /**
     * Creates error response JSON object with single error.
     *
     * @param errorMessage the error message
     * @param jsonFactory the JSON factory
     * @return JSON object with error
     */
    public static JSONObject createErrorResponse(String errorMessage, JSONFactory jsonFactory) {
        JSONObject responseJson = jsonFactory.createJSONObject();
        responseJson.put("success", false);

        JSONArray errors = jsonFactory.createJSONArray();
        errors.put(errorMessage);
        responseJson.put("errors", errors);

        return responseJson;
    }

    /**
     * Creates error response JSON object with multiple errors.
     *
     * @param errors the list of error messages
     * @param jsonFactory the JSON factory
     * @return JSON object with errors
     */
    public static JSONObject createErrorResponse(List<String> errors, JSONFactory jsonFactory) {
        JSONObject responseJson = jsonFactory.createJSONObject();
        responseJson.put("success", false);

        JSONArray errorsArray = jsonFactory.createJSONArray();
        errors.forEach(errorsArray::put);
        responseJson.put("errors", errorsArray);

        return responseJson;
    }

    /**
     * Creates success response JSON object.
     *
     * @param jsonFactory the JSON factory
     * @return JSON object with success true
     */
    public static JSONObject createSuccessResponse(JSONFactory jsonFactory) {
        JSONObject responseJson = jsonFactory.createJSONObject();
        responseJson.put("success", true);
        return responseJson;
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     * All methods in ResponseUtil are static and should be accessed through the class name.
     */
    private ResponseUtil() {}
}