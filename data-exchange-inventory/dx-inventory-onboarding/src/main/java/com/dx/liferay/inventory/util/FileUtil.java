package com.dx.liferay.inventory.util;

import com.dx.liferay.inventory.constants.InventoryConstants;
import com.dx.liferay.inventory.exception.FileProcessingException;
import com.liferay.document.library.kernel.service.DLAppServiceUtil;
import com.liferay.document.library.kernel.util.DLUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import javax.portlet.RenderRequest;
import javax.portlet.ResourceRequest;
import java.io.File;

/**
 * Utility class for common file operations.
 *
 * Provides shared file handling methods to eliminate code duplication
 * across services and portlets.
 */
public class FileUtil {

    private static final Log _log = LogFactoryUtil.getLog(FileUtil.class);

    /**
     * Retrieves sample file entry from portlet preferences.
     *
     * @param resourceRequest the resource request containing portlet preferences
     * @return sample file entry or null if not found or error occurs
     */
    public static FileEntry getSampleFileEntry(ResourceRequest resourceRequest) {
        try {
            String fileEntryId = resourceRequest.getPreferences().getValue("fileEntryId", "");
            if (Validator.isNotNull(fileEntryId)) {
                return DLAppServiceUtil.getFileEntry(Long.parseLong(fileEntryId));
            }
        } catch (Exception e) {
            _log.error("Error retrieving sample file entry: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Sets the sample document URL as a request attribute for use in JSP views.
     * Generates a preview URL for the configured sample file that can be displayed to users
     * as a reference or download link.
     *
     * @param renderRequest the render request to set the document URL attribute on
     */
    public static void setSampleDocumentUrl(RenderRequest renderRequest) throws PortalException {
        ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);
        String fileEntryId = renderRequest.getPreferences().getValue("fileEntryId", StringPool.BLANK);

        if (Validator.isNotNull(fileEntryId)) {
            FileEntry fileEntry = DLAppServiceUtil.getFileEntry(Long.parseLong(fileEntryId));
            if (fileEntry != null) {
                String friendlyUrl = DLUtil.getPreviewURL(
                        fileEntry, fileEntry.getFileVersion(), themeDisplay, StringPool.BLANK, false, false
                );
                renderRequest.setAttribute("sampleDocumentUrl", friendlyUrl);
            }
        }
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     * All methods are static and should be accessed through the class name.
     */
    private FileUtil() {}
}