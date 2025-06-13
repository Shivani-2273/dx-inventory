package com.dx.liferay.inventory.portlet;

import com.dx.liferay.inventory.constants.InventoryConstants;
import com.dx.liferay.inventory.service.InventoryService;
import com.dx.liferay.inventory.util.FormExtractionUtil;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectDefinitionLocalServiceUtil;
import com.liferay.object.service.ObjectFieldLocalServiceUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.security.auth.AuthTokenUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MVC Action Command that handles form submission for adding inventory datasets.
 * Processes form data to extract multiple datasets with their attributes and metadata,
 * then delegates to the inventory service for persistence.
 */
@Component(
        immediate = true,
        property = {
                "javax.portlet.name=" + InventoryConstants.DXINVENTORYONBOARDING,
                "mvc.command.name=/addDataset",
                "javax.portlet.display-name=Inventory Onboarding Form",
                "javax.portlet.init-param.template-path=/",
                "javax.portlet.init-param.view-template=/view.jsp",
                "javax.portlet.resource-bundle=content.Language",
                "javax.portlet.security-role-ref=power-user,user"

        },
        service = MVCActionCommand.class
)
public class SubmitFormMVCActionCommand implements MVCActionCommand {

    private static final Log _log = LogFactoryUtil.getLog(SubmitFormMVCActionCommand.class);

    /**
     * Processes the form submission action by extracting datasets from the request
     * and delegating to the inventory service for persistence.
     *
     * @param actionRequest the action request containing form parameters and user context
     * @param actionResponse the action response for potential redirects or response data
     * @return true to indicate successful processing regardless of outcome
     */
    @Override
    public boolean processAction(ActionRequest actionRequest, ActionResponse actionResponse) {
        try{
            _log.info("Processing action command");
            ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

            String actionType = ParamUtil.getString(actionRequest, "actionType", "submit");
            boolean isDraft = "draft".equals(actionType);

            _log.info("Processing action with type: " + actionType + " (isDraft: " + isDraft + ")");
            List<Map<String, Object>> datasets = _formExtractionUtil.extractDatasetsFromRequest(actionRequest,themeDisplay.getLanguageId());

            if (!datasets.isEmpty()) {

                ObjectEntry inventoryEntry = _inventoryService.addInventory(
                        themeDisplay.getCompanyId(),
                        themeDisplay.getUserId(),
                        themeDisplay.getLanguageId(),
                        isDraft,actionRequest);

                // Process all datasets
                _inventoryService.addMultipleDatasets(
                        themeDisplay.getCompanyId(),
                        themeDisplay.getUserId(),
                        datasets,
                        inventoryEntry.getObjectEntryId()
                );
                _log.info("Successfully processed " + datasets.size() + " datasets");
            } else {
                _log.warn("No datasets found in request");
            }

        }catch (Exception e) {
            _log.error(e);
        }
        return true;
    }


    @Reference
    private FormExtractionUtil _formExtractionUtil;

    @Reference
    private InventoryService _inventoryService;

}
