package com.dx.liferay.inventory.configuration;

import com.dx.liferay.inventory.constants.InventoryConstants;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.ConfigurationAction;
import com.liferay.portal.kernel.portlet.DefaultConfigurationAction;
import com.liferay.portal.kernel.util.ParamUtil;
import org.osgi.service.component.annotations.Component;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;


/**
 * Configuration action for the DX Inventory Onboarding portlet.
 *
 * This class handles the configuration settings for the inventory onboarding portlet,
 * specifically managing the file entry ID preference that is used to associate
 * uploaded files with the portlet instance.
 *
 * The configuration action is automatically registered as an OSGi service component
 * and is bound to the DX Inventory Onboarding portlet through the component property.
 */
@Component(
        property = "javax.portlet.name=" + InventoryConstants.DXINVENTORYONBOARDING,
        service = ConfigurationAction.class
)
public class InventoryOnboardingConfiguration extends DefaultConfigurationAction {

    private static final Log _log = LogFactoryUtil.getLog(InventoryOnboardingConfiguration.class);


    /**
     * Processes the configuration action request for the inventory onboarding portlet.
     *
     * @param portletConfig the portlet configuration containing portlet metadata and settings
     * @param actionRequest the action request containing the form data
     * @param actionResponse the action response used to send data back to the client
     */
    @Override
    public void processAction(PortletConfig portletConfig, ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {

        setPreference(
                actionRequest, "fileEntryId",
                ParamUtil.getString(actionRequest, "fileEntryId"));

        super.processAction(portletConfig, actionRequest, actionResponse);
    }
}
