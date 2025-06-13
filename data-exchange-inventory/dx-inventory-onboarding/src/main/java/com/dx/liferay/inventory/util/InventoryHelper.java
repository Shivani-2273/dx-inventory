package com.dx.liferay.inventory.util;

import com.dx.liferay.inventory.constants.InventoryConstants;
import com.dx.liferay.inventory.service.InventoryService;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.petra.sql.dsl.DSLQueryFactoryUtil;
import com.liferay.petra.sql.dsl.Table;
import com.liferay.petra.sql.dsl.expression.Expression;
import com.liferay.petra.sql.dsl.query.DSLQuery;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component(
        immediate = true,
        service = InventoryHelper.class
)
public class InventoryHelper {

    private static final Log _log = LogFactoryUtil.getLog(InventoryHelper.class);


    public  String getInventoryName(ObjectEntry objectEntry) {
        Map<String, Serializable> values = objectEntry.getValues();

        Serializable i18nValue = values.get("inventoryName_i18n");
        if (i18nValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> i18nMap = (Map<String, String>) i18nValue;

            String englishName = i18nMap.get("en_US");
            if (englishName != null && !englishName.trim().isEmpty()) {
                return englishName.trim();
            }

            String arabicName = i18nMap.get("ar_SA");
            if (arabicName != null && !arabicName.trim().isEmpty()) {
                return arabicName.trim();
            }
        }

        return null;
    }

    public int getInventoryDatasetCount(ObjectEntry parentInventoryEntry) {
        List<ObjectEntry> datasets = getInventoryDatasetsList(parentInventoryEntry);
        return datasets.size();
    }


    public  String getInventoryStatus(ObjectEntry objectEntry) {
        int status = objectEntry.getStatus();
        String statusLabel = WorkflowConstants.getStatusLabel(status);
        return LanguageUtil.get(LocaleUtil.getDefault(), statusLabel);

    }

    public List<ObjectEntry> getInventoryDatasetsList(ObjectEntry parentInventoryEntry) {
        try {
            // Get the child object definition
            ObjectDefinition inventoryOnboardingDefinition = _objectDefinitionLocalService.fetchObjectDefinition(
                    parentInventoryEntry.getCompanyId(), InventoryConstants.DX_INVENTORY_OBJECT_NAME);

            // Get the relationship ObjectField
            ObjectField relationshipField = _objectFieldLocalService.getObjectField(inventoryOnboardingDefinition.getObjectDefinitionId(),
                    InventoryConstants.DX_INVENTORY_DATASET_RELATIONSHIP_ID
            );

            // Get the relationship Table
            Table<?> relationshipTable = _objectFieldLocalService.getTable(inventoryOnboardingDefinition.getObjectDefinitionId(),
                    relationshipField.getName()
            );

            // Get the relationship column
            Expression<Long> inventoryDetailsId = relationshipTable.getColumn(relationshipField.getDBColumnName(), Long.class);

            // Build DSL query to get dataset IDs
            DSLQuery datasetsQuery = DSLQueryFactoryUtil
                    .select(relationshipTable.getColumn("c_inventoryOnboardingId_", Long.class))
                    .from(relationshipTable)
                    .where(inventoryDetailsId.eq(parentInventoryEntry.getObjectEntryId()));

            List<Long> datasetIds = _objectEntryLocalService.dslQuery(datasetsQuery);
            List<ObjectEntry> datasets = new ArrayList<>();

            // Get actual ObjectEntry for each ID
            for (Long datasetId : datasetIds) {
                try {
                    ObjectEntry dataset = _objectEntryLocalService.getObjectEntry(datasetId);
                    datasets.add(dataset);
                } catch (Exception e) {
                    _log.error("Error getting ObjectEntry for ID: " + datasetId, e);
                }
            }

            return datasets;

        } catch (Exception e) {
            _log.error("Failed to get inventory datasets for inventory details ID: " + parentInventoryEntry.getObjectEntryId(), e);
            return new ArrayList<>();
        }
    }

    @Reference
    ObjectDefinitionLocalService _objectDefinitionLocalService;

    @Reference
    ObjectEntryLocalService _objectEntryLocalService;

    @Reference
    ObjectFieldLocalService _objectFieldLocalService;

}
