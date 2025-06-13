$(document).ready(function() {
    // Check if we're in review mode
    const urlParams = new URLSearchParams(window.location.search);
    const inventoryId = urlParams.get('inventoryId');
    const mode = document.getElementById('userMode').value;

    if (inventoryId && (mode === 'review' || mode === 'update')) {
        ReviewMode.initialize(inventoryId, mode);
    }
});

window.ReviewMode = {
    currentMode: 'review',
    currentInventoryId: null,

    initialize: function(inventoryId,mode = 'review') {
        this.currentMode = mode;
        this.currentInventoryId = inventoryId
        this.setupUI();
        this.fetchAndPopulateData(inventoryId);
    },

    setupUI: function() {
        // $('#excelTemplateBtn, #addDatasetManuallyBtn').hide();
        // Update page title
        const pageTitle = this.currentMode === 'review'
            ? 'Review Inventory Submission'
            : 'Update Inventory Submission';
        $('h2.text-primary').text(pageTitle);
    },

    fetchAndPopulateData: function(inventoryId) {
        $.ajax({
            url: window.portletURLs.fetchData,
            type: 'GET',
            data: {
                inventoryId: inventoryId,
                namespace: window.portletNamespace
            },
            timeout: 30000,
            success: (response) => {
                this.handleDataResponse(response, inventoryId);
            },
            error: (xhr, status, error) => {
                this.handleDataError(xhr, status, error);
            }
        });
    },

    /**
     * Handle successful data response
     */
    handleDataResponse: function(response, inventoryId) {
        try {
            const result = typeof response === 'string' ? JSON.parse(response) : response;

            if (result.success && result.inventory) {
                // Add mode-specific header
                this.addModeHeader(result.inventory, inventoryId);

                if (result.inventory.datasets && result.inventory.datasets.length > 0) {
                    this.populateDatasets(result.inventory.datasets);
                } else {
                    console.error("no datasets");
                }
            } else {
                this.showErrorMessage(result.error || 'Failed to load inventory data');
            }
        } catch (e) {
            console.error('Error parsing response:', e);
            this.showErrorMessage('Error processing inventory data');
        }
    },


    addModeHeader: function(inventory, inventoryId) {
        const isReviewMode = this.currentMode === 'review';
        const headerClass = isReviewMode ? 'alert-info' : 'alert-warning';
        const iconClass = isReviewMode ? 'fa-eye' : 'fa-edit';
        const modeTitle = isReviewMode ? 'Review Mode' : 'Update Mode';
        const modeDescription = isReviewMode
            ? 'Viewing inventory submission in read-only mode'
            : 'Edit and update inventory submission';

        const headerHtml = `
            <div class="${headerClass} mb-4" id="modeHeader">
                <h5><i class="fa ${iconClass}"></i> ${modeTitle}</h5>
                <div class="row">
                    <div class="col-md-6">
                        <p class="mb-1"><strong>Inventory:</strong> ${inventory.inventoryName || 'N/A'}</p>
                        <p class="mb-0"><strong>ID:</strong> ${inventoryId}</p>
                    </div>
                    <div class="col-md-6">
                        <p class="mb-1"><strong>Datasets:</strong> ${inventory.datasetCount || 0}</p>
                        <p class="mb-0"><strong>Mode:</strong> ${isReviewMode ? 'Read-only' : 'Editable'}</p>
                    </div>
                </div>
                <div class="mt-2">
                    <small class="text-muted">${modeDescription}</small>
                </div>
            </div>
        `;

        $('#formsContainer').html(headerHtml);
    },

    /**
     * Handle data fetch error
     */
    handleDataError: function(xhr, status, error) {
        console.error('Error fetching inventory data:', error);

        let errorMessage = 'Error loading inventory data: ';
        if (status === 'timeout') {
            errorMessage += 'Request timed out';
        } else if (xhr.responseText) {
            try {
                const errorResponse = JSON.parse(xhr.responseText);
                errorMessage += errorResponse.error || error;
            } catch (e) {
                errorMessage += error;
            }
        } else {
            errorMessage += error;
        }

        this.showErrorMessage(errorMessage);
    },

    /**
     * Populate datasets using existing DatasetManager
     */
    populateDatasets: function(datasets) {
        // Convert backend data format to match DatasetManager expected format
        const formattedDatasets = datasets.map(dataset => ({
            datasetId: dataset.datasetId,
            datasetName: dataset.datasetName,
            datasetDescription: dataset.datasetDescription,
            datasetClassification: dataset.datasetClassification,
            userDemand: dataset.userDemand,
            economicImpact: dataset.economicImpact,
            betterServices: dataset.betterServices,
            betterGovernance: dataset.betterGovernance,
            definedOwner: dataset.definedOwner,
            existingMetadata: dataset.existingMetadata,
            alreadyPublished: dataset.alreadyPublished,
            openFormat: dataset.openFormat,
            releaseYear: dataset.releaseYear,
            releaseMonth: dataset.releaseMonth,
            attributes: dataset.attributes || []
        }));

        // Use existing DatasetManager to create and populate forms
        if (window.DatasetManager) {
            // Clear existing forms first
            window.DatasetManager.clearAllDatasets();

            // Create forms for each dataset
            formattedDatasets.forEach((datasetData, index) => {
                const datasetId = index + 1;

                if (index === 0) {
                    // Generate first dataset using existing function
                    window.DatasetManager.generateFirstDataset();
                } else {
                    // Add additional datasets using existing function
                    window.DatasetManager.addNewDataset();
                }
                this.updateSidebarDatasetName(datasetId, datasetData.datasetName);

                // Populate the dataset fields with a delay
                setTimeout(() => {
                    this.populateDatasetForm(datasetId, datasetData);
                }, 500 + (200 * index));
            });

            // Apply mode-specific styling after population
            setTimeout(() => {
                if (this.currentMode === 'review') {
                    this.makeFormsReadOnly();
                } else {
                    this.enableUpdateMode();
                }
            }, 1000 + (200 * formattedDatasets.length));

        } else {
            console.error('DatasetManager not available');
            this.showErrorMessage('Form management system not available');
        }
    },

    /**
     * Update sidebar dataset name with actual dataset name
     */
    updateSidebarDatasetName: function(datasetId, datasetName) {
        const displayName = datasetName && datasetName.trim() ? datasetName.trim() : `Dataset ${datasetId}`;
        const truncatedName = displayName.length > 20 ? displayName.substring(0, 20) + '...' : displayName;

        $(`.dataset-item[data-dataset="${datasetId}"] span`).text(truncatedName);

        // Add tooltip for full name if truncated
        if (displayName.length > 20) {
            $(`.dataset-item[data-dataset="${datasetId}"]`).attr('title', displayName);
        }
    },

    /**
     * Populate individual dataset form
     */
    populateDatasetForm: function(datasetId, datasetData) {

        if (datasetData.datasetId) {
            const actualIdInput = `<input type="hidden" name="${window.portletNamespace}actualDatasetId_${datasetId}" value="${datasetData.datasetId}" />`;
            $(`#datasetForm_${datasetId}`).prepend(actualIdInput);
        }

        // Populate basic fields
        $(`#datasetName_${datasetId}`).val(datasetData.datasetName || '');
        $(`#datasetDescription_${datasetId}`).val(datasetData.datasetDescription || '');
        $(`#datasetClassification_${datasetId}`).val(datasetData.datasetClassification || '');

        // Populate prioritization fields
        $(`#userDemand_${datasetId}`).val(datasetData.userDemand || '');
        $(`#economicImpact_${datasetId}`).val(datasetData.economicImpact || '');
        $(`#betterServices_${datasetId}`).val(datasetData.betterServices || '');
        $(`#betterGovernance_${datasetId}`).val(datasetData.betterGovernance || '');

        // Populate quality fields
        $(`#definedOwner_${datasetId}`).val(datasetData.definedOwner || '');
        $(`#existingMetadata_${datasetId}`).val(datasetData.existingMetadata || '');
        $(`#alreadyPublished_${datasetId}`).val(datasetData.alreadyPublished || '');
        $(`#openFormat_${datasetId}`).val(datasetData.openFormat || '');

        // Populate release fields
        $(`#releaseYear_${datasetId}`).val(datasetData.releaseYear || '');
        $(`#releaseMonth_${datasetId}`).val(datasetData.releaseMonth || '');

        // Populate attributes
        if (datasetData.attributes && datasetData.attributes.length > 0) {
            setTimeout(() => {
                this.populateAttributesWithIds(datasetId, datasetData.attributes);
            }, 300);
        }
    },

    /**
     * Populate attributes for a dataset
     */
    populateAttributesWithIds: function(datasetId, attributes) {
        // Clear existing attributes except the first one
        $(`#attributesContainer_${datasetId} .single-attribute-field:not(:first)`).remove();

        attributes.forEach((attribute, index) => {
            const attributeIndex = index + 1;

            // Add new attribute field if needed (except for first one)
            if (index > 0 && window.DatasetManager) {
                window.DatasetManager.addAttributeToDataset(datasetId);
            }

            // Add hidden input for actual attribute ID
            if (attribute.attributeId) {
                const actualAttributeIdInput = `<input type="hidden" name="${window.portletNamespace}actualAttributeId_${datasetId}_${attributeIndex}" value="${attribute.attributeId}" />`;
                $(`#attributesContainer_${datasetId} .single-attribute-field[data-index="${attributeIndex}"]`).prepend(actualAttributeIdInput);
            }

            // Populate the attribute fields
            const nameInput = $(`input[name="${window.portletNamespace}attributeName_${datasetId}_${attributeIndex}"]`);
            const descInput = $(`input[name="${window.portletNamespace}attributeDescription_${datasetId}_${attributeIndex}"]`);

            if (nameInput.length && attribute.attributeName) {
                nameInput.val(attribute.attributeName);
            }

            if (descInput.length && attribute.attributeDescription) {
                descInput.val(attribute.attributeDescription);
            }
        });

        // Update UI elements using existing functions
        if (window.DatasetManager) {
            window.DatasetManager.updateRemoveButtons(datasetId);
            window.DatasetManager.updateAttributeLabels(datasetId);
        }
    },

    /**
     * Make all forms read-only
     */
    makeFormsReadOnly: function() {
        // Disable all form inputs
        $('#formsContainer input, #formsContainer select')
            .prop('readonly', true)
            .prop('disabled', true)
            .addClass('form-control-plaintext bg-light');

        // Hide action buttons
        $('.addAttributeBtn, .remove-attribute, .saveAsDraftBtn, .cancelBtn, .submitBtn').hide();
        $('.dataset-delete, #addDatasetManuallyBtn').hide();

        // Style labels
        $('#formsContainer label').addClass('font-weight-bold text-dark');

        // Add read-only indicators
        $('.dataset-form').each(function() {
            $(this).prepend(`
                <div class="alert alert-secondary mb-3">
                    <i class="fa fa-lock"></i> <strong>Read-Only Mode</strong> - This form is in review mode
                </div>
            `);
        });
    },

    enableUpdateMode: function() {
        // Keep forms editable - don't disable inputs
        $('#formsContainer input, #formsContainer select')
            .prop('readonly', false)
            .prop('disabled', false)

        // Keep attribute management buttons visible in update mode
        $('.addAttributeBtn, .remove-attribute, .dataset-delete, #addDatasetManuallyBtn, #excelTemplateBtn').show();

        // Add update mode indicators
        $('.dataset-form').each(function() {
            $(this).prepend(`
                <div class="alert alert-warning mb-3">
                    <i class="fa fa-edit"></i> <strong>Update Mode</strong> - You can modify the form data
                </div>
            `);
        });
    },

    showErrorMessage: function(message) {
        $('#formsContainer').html(`
            <div class="alert alert-danger text-center">
                <i class="fa fa-exclamation-triangle fa-3x mb-3"></i>
                <h5>Error Loading Data</h5>
                <p>${message}</p>
            </div>
        `);
    }

};