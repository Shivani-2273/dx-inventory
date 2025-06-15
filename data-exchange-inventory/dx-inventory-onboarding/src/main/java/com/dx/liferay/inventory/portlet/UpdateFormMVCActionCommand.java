package com.dx.liferay.inventory.portlet;

import com.dx.liferay.inventory.constants.InventoryConstants;
import com.dx.liferay.inventory.service.InventoryService;
import com.dx.liferay.inventory.util.FormExtractionUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import java.util.List;
import java.util.Map;

@Component(
        immediate = true,
        property = {
                "javax.portlet.name=" + InventoryConstants.DXINVENTORYONBOARDING,
                "mvc.command.name=/updateDataset",
                "javax.portlet.display-name=Inventory Onboarding Form",
                "javax.portlet.init-param.template-path=/",
                "javax.portlet.init-param.view-template=/view.jsp",
                "javax.portlet.resource-bundle=content.Language",
                "javax.portlet.security-role-ref=power-user,user"

        },
        service = MVCActionCommand.class
)
public class UpdateFormMVCActionCommand implements MVCActionCommand {

    private static final Log _log = LogFactoryUtil.getLog(UpdateFormMVCActionCommand.class);

    @Override
    public boolean processAction(ActionRequest actionRequest, ActionResponse actionResponse) {
        try {
            ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
            long inventoryId = ParamUtil.getLong(actionRequest, "inventoryId");

            String actionType = ParamUtil.getString(actionRequest, "actionType", "submit");
            boolean isDraft = "draft".equals(actionType);

            _log.info("Processing action with type: " + actionType + " (isDraft: " + isDraft + ")");

            if (inventoryId <= 0) {
                _log.error("Invalid inventory ID: " + inventoryId);
                return false;
            }

            List<Map<String, Object>> updatedDatasets = _formExtractionUtil.extractDatasetsFromRequest(actionRequest, themeDisplay.getLanguageId());
            if (!updatedDatasets.isEmpty()) {
                // Update the inventory datasets with status
                _inventoryService.updateMultipleDatasets(themeDisplay.getCompanyId(), themeDisplay.getUserId(), inventoryId, updatedDatasets, themeDisplay.getLanguageId(), isDraft);
                _inventoryService.updateInventoryStatus(inventoryId, isDraft, actionRequest);
                _log.info("Successfully updated inventory " + inventoryId + " with " + updatedDatasets.size() + " datasets");
            } else {
                _log.warn("No datasets found in update request for inventory: " + inventoryId);
            }

        } catch (Exception e) {
            _log.error("Error processing update dataset action", e);
        }
        return true;
    }


    @Reference
    FormExtractionUtil _formExtractionUtil;

    @Reference
    InventoryService _inventoryService;

}
