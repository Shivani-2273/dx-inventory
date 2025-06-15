package com.dx.liferay.inventory.service;

import com.liferay.object.model.ObjectEntry;
import com.liferay.portal.kernel.exception.PortalException;

import javax.portlet.ActionRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing inventory datasets and their associated attributes.
 * Provides operations for creating dataset entries, managing dataset attributes,
 * and bulk processing of multiple datasets within the Liferay Object framework.
 */
public interface InventoryService {

    /**
     * Creates a new dataset object entry in the inventory system.
     *
     * @param companyId the company ID where the dataset will be created
     * @param inventoryValues map containing the dataset field values and metadata
     * @return the created ObjectEntry representing the new dataset
     * @throws PortalException if there's an error creating the dataset entry
     */
    ObjectEntry addDataSetObjectEntry(long companyId, long userId, Map<String, Serializable> inventoryValues) throws PortalException;

    /**
     * Adds multiple attribute entries associated with a specific dataset inventory entry.
     * Creates attribute records that are linked to the parent dataset.
     *
     * @param companyId the company ID where the attributes will be created
     * @param entryValuesList list of maps containing attribute names, descriptions, and metadata
     * @param datasetInventoryEntryId the ID of the inventory dataset entry
     * @param userLocale the user's locale for internationalization and formatting
     * @throws PortalException if there's an error creating the attribute entries
     */
    void addDatasetInventoryAttributesEntry(long companyId, long userId, List<Map<String, Serializable>> entryValuesList, long datasetInventoryEntryId, String userLocale) throws PortalException;

    /**
     * Processes and creates multiple datasets in a single operation.
     * Handles both dataset creation and their associated attributes in bulk.
     *
     * @param companyId the company ID where the datasets will be created
     * @param userId ID of user adding inventory request
     * @param datasets list of dataset maps, each containing dataset information and associated attributes
     * @param parentInventoryId is parent id for specific inventory
     * @throws PortalException if there's an error during the bulk dataset creation process
     */
    void addMultipleDatasets(long companyId, long userId, List<Map<String, Object>> datasets, long parentInventoryId) throws PortalException;


    List<Map<String, Object>> getInventoryEntries(long companyId) throws PortalException;


     ObjectEntry addInventory(long companyId, long userId, String userLocale, boolean isDraft, ActionRequest actionRequest) throws Exception;

    /**
     * Updates multiple datasets for an existing inventory, handling create, update, and delete operations.
     * Compares submitted datasets with existing ones to determine what changes need to be made.
     *
     * @param companyId the company ID where the datasets exist
     * @param inventoryId the ID of the inventory being updated
     * @param submittedDatasets list of dataset maps from the form submission
     * @param userLocale the user's locale for internationalization
     * @throws PortalException if any error occurs during the update process
     */
    void updateMultipleDatasets(long companyId, long userId, long inventoryId, List<Map<String, Object>> submittedDatasets, String userLocale, boolean isDraft)
            throws PortalException;


    /**
     * Updates the inventory status based on the action type (draft vs submit)
     * @param inventoryId the inventory entry ID to update
     * @param isDraft true if saving as draft, false if submitting
     * @param actionRequest the action request for GraphQL context
     * @throws Exception if update fails
     */
    void updateInventoryStatus(long inventoryId, boolean isDraft, ActionRequest actionRequest) throws Exception;
}
