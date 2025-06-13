<%@ page import="com.liferay.portal.kernel.util.Constants" %>
<%@ include file="/init.jsp" %>
<%@ page import="javax.portlet.PortletPreferences" %>
<%@ taglib prefix="clay" uri="http://liferay.com/tld/clay" %>

<liferay-portlet:actionURL portletConfiguration="<%= true %>" var="configurationActionURL" />
<liferay-portlet:renderURL portletConfiguration="<%= true %>" var="configurationRenderURL" />

<aui:form action="<%= configurationActionURL %>" method="post" name="fm">
    <aui:input name="<%= Constants.CMD %>" type="hidden" value="<%= Constants.UPDATE %>" />
    <aui:input name="redirect" type="hidden" value="<%= configurationRenderURL %>" />

    <clay:container-fluid>
        <clay:row>
            <clay:col md="6" cssClass="mt-5">
                <aui:input name="fileEntryId"
                           label="File Entry ID"
                           type="text"
                           placeholder="Enter FileEntryId"
                           value="${portletPreferences.getValue('fileEntryId', '')}" />

                <aui:button-row>
                    <aui:button type="submit" name="submit" value="Save Settings" primary="true" />
                </aui:button-row>
            </clay:col>
        </clay:row>
    </clay:container-fluid>
</aui:form>
