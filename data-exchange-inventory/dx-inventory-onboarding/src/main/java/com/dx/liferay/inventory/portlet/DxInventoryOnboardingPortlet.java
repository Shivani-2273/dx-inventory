package com.dx.liferay.inventory.portlet;

import com.dx.liferay.inventory.constants.InventoryConstants;
import com.dx.liferay.inventory.model.ValidationResult;
import com.dx.liferay.inventory.service.ExcelParsingService;
import com.dx.liferay.inventory.service.InventoryReviewService;
import com.dx.liferay.inventory.service.InventoryService;
import com.dx.liferay.inventory.service.impl.InventoryServiceImpl;
import com.dx.liferay.inventory.util.*;
import com.dx.liferay.inventory.exception.FileProcessingException;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import org.osgi.service.component.annotations.*;

import javax.annotation.Resource;
import javax.portlet.*;
import java.io.*;
import java.util.*;

/**
 * Liferay MVC Portlet for inventory onboarding functionality.
 * Handles file upload, validation, and processing of Excel files for inventory data.
 */
@Component(
		configurationPid = "com.dx.liferay.inventory.configuration.InventoryOnboardingConfiguration",
		immediate = true,
		property = {
				"com.liferay.portlet.display-category=category.sample",
				"com.liferay.portlet.header-portlet-css=/css/main.css",
				"com.liferay.portlet.instanceable=true",
				"javax.portlet.display-name=DX Inventory Onboarding",
				"javax.portlet.init-param.template-path=/",
				"javax.portlet.init-param.view-template=/inventory-landing.jsp",
				"javax.portlet.name=" + InventoryConstants.DXINVENTORYONBOARDING,
				"javax.portlet.resource-bundle=content.Language",
				"javax.portlet.security-role-ref=power-user,user"
		},
		service = Portlet.class
)
public class DxInventoryOnboardingPortlet extends MVCPortlet {

	private static final Log _log = LogFactoryUtil.getLog(DxInventoryOnboardingPortlet.class);

	/**
	 * Renders the portlet view by setting up the sample document URL and delegating to the parent render method.
	 *
	 * @param renderRequest the render request containing request parameters and attributes
	 * @param renderResponse the render response for writing output to the portlet
	 */
	@Override
	public void render(RenderRequest renderRequest, RenderResponse renderResponse)
			throws IOException, PortletException {
		try {
			ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);
			FileUtil.setSampleDocumentUrl(renderRequest);

			List<Map<String, Object>> inventoryList = _inventoryService.getInventoryEntries(themeDisplay.getCompanyId());
			renderRequest.setAttribute("inventoryList", inventoryList);
		} catch (PortalException e) {
			throw new RuntimeException(e);
		}
		super.render(renderRequest, renderResponse);
	}

	/**
	 * Serves AJAX resource requests by routing to appropriate handlers based on resource ID.
	 * Supports file validation and file processing operations.
	 *
	 * @param resourceRequest the resource request containing the resource ID and parameters
	 * @param resourceResponse the resource response for writing JSON output

	 */
	@Override
	public void serveResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
			throws IOException {
		try {
			String resourceID = resourceRequest.getResourceID();
			_log.info("ServeResource called with resourceID: " + resourceID);

			switch (resourceID) {
				case InventoryConstants.VALIDATE_FILE_RESOURCE_ID:
					validateUploadedFile(resourceRequest, resourceResponse);
					break;
				case InventoryConstants.PROCESS_FILE_RESOURCE_ID:
					processUploadedFile(resourceRequest, resourceResponse);
					break;
				case InventoryConstants.FETCH_DATA_RESOURCE_ID:
					getInventoryData(resourceRequest,resourceResponse);
					break;
				default:
					super.serveResource(resourceRequest, resourceResponse);
			}
		} catch (Exception e) {
			_log.error("Unexpected error in serveResource: " + e.getMessage(), e);
		}
	}

	/**
	 * Validates an uploaded file against a sample file template.
	 * Checks file existence, compares structure with sample file, and returns validation results.
	 *
	 * @param request the resource request containing the uploaded file
	 * @param response the resource response for writing validation results as JSON
	 */
	private void validateUploadedFile(ResourceRequest request, ResourceResponse response) throws IOException {
		try {
			UploadPortletRequest uploadRequest = _portal.getUploadPortletRequest(request);
			File uploadedFile = uploadRequest.getFile("file");
			FileEntry sampleFile = FileUtil.getSampleFileEntry(request);

			JSONObject responseJson;
			if (uploadedFile == null || !uploadedFile.exists()) {
				responseJson = ResponseUtil.createErrorResponse("No file uploaded", _jsonFactory);
			} else if (sampleFile == null) {
				responseJson = ResponseUtil.createErrorResponse("Sample file not configured", _jsonFactory);
			} else {
				// Use ValidationUtil directly
				ValidationResult result = FileValidationUtil.validateFile(
						uploadedFile, sampleFile, request.getLocale());

				if (result.isValid()) {
					responseJson = ResponseUtil.createSuccessResponse(_jsonFactory);
					responseJson.put("columnCount", result.getMetadata().getColumnCount());
					responseJson.put("rowCount", result.getMetadata().getRowCount());
					responseJson.put("dataTypes", "All valid");
					responseJson.put("encoding", InventoryConstants.DEFAULT_ENCODING);
				} else {
					responseJson = ResponseUtil.createErrorResponse(result.getErrors(), _jsonFactory);
				}
			}

			ResponseUtil.writeJsonResponse(response, responseJson);
		} catch (Exception e) {
			ResponseUtil.writeErrorResponse(response, "Validation error", _jsonFactory);
		}
	}


	/**
	 * Processes an uploaded Excel file by parsing it into datasets using the sample file as a template.
	 * Validates inputs, identifies special fields, and converts the data to JSON format.
	 *
	 * @param resourceRequest the resource request containing the uploaded Excel file
	 * @param resourceResponse the resource response for writing processing results as JSON
	 */
	private void processUploadedFile(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
			throws IOException {

		try {
			UploadPortletRequest uploadPortletRequest = _portal.getUploadPortletRequest(resourceRequest);
			File uploadedFile = uploadPortletRequest.getFile("file");
			FileEntry sampleFileEntry = FileUtil.getSampleFileEntry(resourceRequest);

			// Get the sample structure for field identification
			FileStructure sampleStructure = FileValidationUtil.readSampleFileWithRules(sampleFileEntry);
			sampleStructure.identifySpecialFields();

			// Parse the Excel file using service
			List<Map<String, Object>> datasets = _excelParsingService.parseExcelFile(uploadedFile, sampleFileEntry);

			// Create and send success response
			JSONObject responseJson = createSuccessResponse(datasets,sampleStructure);
			ResponseUtil.writeJsonResponse(resourceResponse, responseJson);

		} catch (FileProcessingException e) {
			_log.error("File processing failed: " + e.getMessage(), e);
			ResponseUtil.writeErrorResponse(resourceResponse, e.getMessage(), _jsonFactory);
		} catch (Exception e) {
			_log.error("Unexpected error processing file: " + e.getMessage(), e);
			ResponseUtil.writeErrorResponse(resourceResponse, "Failed to process uploaded file. Please try again.", _jsonFactory);
		}
	}

	/**
	 * Creates a JSON success response containing processed datasets and field metadata.
	 *
	 * @param datasets the list of processed datasets to include in the response
	 * @param sampleStructure the file structure containing field metadata information
	 * @return JSON object with success status, datasets, and field metadata
	 * FileStructure structure removed param
	 */
	private JSONObject createSuccessResponse(List<Map<String, Object>> datasets, FileStructure sampleStructure) {
		JSONObject responseJson = ResponseUtil.createSuccessResponse(_jsonFactory);

		// Add datasets
		JSONArray datasetsArray = _jsonFactory.createJSONArray();
		datasets.stream()
				.map(this::convertDatasetToJson)
				.forEach(datasetsArray::put);
		responseJson.put("datasets", datasetsArray);

		// Add field metadata
		responseJson.put("fieldMetadata", createFieldMetadata(sampleStructure));

		return responseJson;
	}

	/**
	 * Creates field metadata JSON object containing special fields and regular fields information.
	 *
	 * @param structure the file structure containing field definitions and special field mappings
	 * @return JSON object with metadata about dataset name field, attributes field, and regular fields
	 */
	private JSONObject createFieldMetadata(FileStructure structure) {
		JSONObject metadata = _jsonFactory.createJSONObject();

		// Add special fields
		if (structure.getDatasetNameField() != null) {
			metadata.put("datasetNameField", structure.getDatasetNameField());
		}
		if (structure.getAttributesField() != null) {
			metadata.put("attributesField", structure.getAttributesField());
		}
		if (structure.getAttributeDescriptionField() != null) {
			metadata.put("attributeDescriptionField", structure.getAttributeDescriptionField());
		}

		// Add regular fields
		JSONArray regularFields = _jsonFactory.createJSONArray();
		structure.getColumnNames().stream()
				.filter(field -> !structure.getSpecialFields().contains(field))
				.forEach(regularFields::put);

		metadata.put("regularFields", regularFields);
		return metadata;
	}

	/**
	 * Converts a dataset map to JSON object format, handling attributes as a special array structure.
	 *
	 * @param dataset the dataset map containing key-value pairs and potentially a list of attributes
	 * @return JSON object representation of the dataset with attributes converted to JSON array
	 */
	private JSONObject convertDatasetToJson(Map<String, Object> dataset) {
		JSONObject json = _jsonFactory.createJSONObject();
		for (Map.Entry<String, Object> entry : dataset.entrySet()) {
			if (entry.getValue() instanceof List<?>) {
				JSONArray array = _jsonFactory.createJSONArray();
				for (Map<String, String> attr : (List<Map<String, String>>) entry.getValue()) {
					JSONObject a = _jsonFactory.createJSONObject();
					for (Map.Entry<String, String> e : attr.entrySet()) {
						a.put(e.getKey(), e.getValue());
					}
					array.put(a);
				}
				json.put("attributes", array);
			} else {
				json.put(entry.getKey(), entry.getValue());
			}
		}
		return json;
	}


	public void getInventoryData(ResourceRequest resourceRequest, ResourceResponse resourceResponse)
			throws IOException {

		final String inventoryIdParam = ParamUtil.getString(resourceRequest, "inventoryId");

		try {
			final long inventoryId = Long.parseLong(inventoryIdParam);
			final ThemeDisplay themeDisplay = (ThemeDisplay) resourceRequest.getAttribute(WebKeys.THEME_DISPLAY);

			final JSONObject inventoryData = _inventoryReviewService.getCompleteInventoryData(
					inventoryId, themeDisplay.getCompanyId());

			final JSONObject successResponse = ResponseUtil.createSuccessResponse(_jsonFactory);
			successResponse.put("inventory", inventoryData);

			ResponseUtil.writeJsonResponse(resourceResponse, successResponse);
		} catch (Exception e) {
			_log.error("Error fetching inventory data for ID:");
			ResponseUtil.writeErrorResponse(resourceResponse,
					"Failed to retrieve inventory data", _jsonFactory);
		}
	}


	@Reference
	Portal _portal;

	@Reference
	JSONFactory _jsonFactory;

	@Reference
	ExcelParsingService _excelParsingService;

	@Reference
	InventoryService _inventoryService;

	@Reference
	InventoryReviewService _inventoryReviewService;



}