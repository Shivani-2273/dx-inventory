package com.dx.liferay.inventory.portlet;

import com.dx.liferay.inventory.constants.InventoryConstants;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Component(
        immediate = true,
        property = {
                "javax.portlet.name=" + InventoryConstants.DXINVENTORYONBOARDING,
                "mvc.command.name=/updateInventoryScore",
                "javax.portlet.display-name=Inventory Onboarding Form",
                "javax.portlet.init-param.template-path=/",
                "javax.portlet.init-param.view-template=/view.jsp",
                "javax.portlet.resource-bundle=content.Language",
                "javax.portlet.security-role-ref=power-user,user"

        },
        service = MVCActionCommand.class
)
public class SubmitInventoryScoreMVCActionCommand implements MVCActionCommand {

    private static final Log _log = LogFactoryUtil.getLog(SubmitInventoryScoreMVCActionCommand.class);

    @Override
    public boolean processAction(ActionRequest actionRequest, ActionResponse actionResponse) {

        ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
        long inventoryId = ParamUtil.getLong(actionRequest, "inventoryId");
        String requestState = ParamUtil.getString(actionRequest, "overallRequestState");
        String feedback = ParamUtil.getString(actionRequest, "overallFeedback");

        // Get compliance ratings for each section
        int inventoryRating = ParamUtil.getInteger(actionRequest, "inventoryRating", -1);
        int prioritizationRating = ParamUtil.getInteger(actionRequest, "prioritizationRating", -1);
        int classificationRating = ParamUtil.getInteger(actionRequest, "classificationRating", -1);
        int releasePlanRating = ParamUtil.getInteger(actionRequest, "releasePlanRating", -1);

        _log.info("Overall request state: " + requestState);
        _log.info("Ratings - Inventory: " + inventoryRating + ", Prioritization: " + prioritizationRating +
                ", Classification: " + classificationRating + ", Release Plan: " + releasePlanRating);

        // Get the existing inventory object entry
        ObjectEntry inventoryEntry = null;
        try {
            inventoryEntry = _objectEntryLocalService.getObjectEntry(inventoryId);
            // Prepare the values map with existing values
            Map<String, Serializable> values = new HashMap<>(inventoryEntry.getValues());

            // Update compliance ratings (only if provided)
            if (inventoryRating >= 0) {
                values.put("inventoryComplianceRating", inventoryRating);
            }
            if (prioritizationRating >= 0) {
                values.put("prioritizationComplianceRating", prioritizationRating);
            }
            if (classificationRating >= 0) {
                values.put("classificationComplianceRating", classificationRating);
            }
            if (releasePlanRating >= 0) {
                values.put("releasePlanComplianceRating", releasePlanRating);
            }

            ServiceContext serviceContext = new ServiceContext();
            serviceContext.setUserId(themeDisplay.getUserId());

            // Update the object entry
             _objectEntryLocalService.updateObjectEntry(themeDisplay.getUserId(), inventoryEntry.getObjectEntryId(),
                    values,
                    serviceContext);
        } catch (PortalException e) {
            _log.error("ObjectEntry not found: " + inventoryId, e);
        }

        _log.info("Submitting inventory score onboarding form for " + inventoryId);
        return true;
    }

    @Reference
    ObjectEntryLocalService _objectEntryLocalService;
}
