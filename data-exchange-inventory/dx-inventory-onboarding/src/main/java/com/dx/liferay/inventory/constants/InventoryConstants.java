package com.dx.liferay.inventory.constants;

/**
 * Constants class for DX Inventory system object names and identifiers.
 *
 * Contains object definitions, relationship keys, and portlet identifiers
 * used throughout the inventory onboarding module.
 */
public class InventoryConstants {

    public static final String DX_INVENTORY_OBJECT_NAME = "C_InventoryOnboarding";
    public static final String DX_INVENTORY_ATTRIBUTE_OBJECT_NAME = "C_DatasetInventoryAttributeDetails";
    public static final String DX_INVENTORY_PARENT_OBJECT_NAME= "C_InventoryDetails";
    public static final String DX_INVENTORY_DATASET_RELATIONSHIP_ID= "r_inventoryDatasetRelationship_c_inventoryDetailsId";
    public static final String DX_INVENTORY_ATTRIBUTE_RELATIONSHIP_ID = "r_inventoryAttributeRelationship_c_inventoryOnboardingId";
    public static final String DXINVENTORYONBOARDING = "com_dx_liferay_inventory_DxInventoryOnboardingPortlet";

    public static final String VALIDATE_FILE_RESOURCE_ID = "validateFile";
    public static final String PROCESS_FILE_RESOURCE_ID = "processFile";
    public static final String FETCH_DATA_RESOURCE_ID = "fetchData";
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    public static final int DEFAULT_HEADER_ROW_INDEX = 1;


    public static final int DEFAULT_HEADER_ROW_COUNT = 2;
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String DEFAULT_ENCODING = "UTF-8";
}
