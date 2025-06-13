$(document).ready(function() {
    let attributeCounters = {};
    let datasetCounter = 0;
    let currentActiveDataset = 1;
    let datasetFormTemplate;
    let fieldMetadata = null;
    const mode = document.getElementById('userMode').value;


    // Initialize template function
    var templateSettings = {
        interpolate: /\{\{(.+?)\}\}/g
    };
    datasetFormTemplate = _.template($('#datasetFormTemplate .template-dataset-form').html(), templateSettings);

    // Generate first dataset automatically on page load
    generateFirstDataset();

    // Event handlers for manual dataset management
    $('#addDatasetManuallyBtn').click(function() {
        addNewDataset();
    });

    $(document).on('click', '.dataset-item', function() {
        const datasetId = $(this).data('dataset');
        switchToDataset(datasetId);
    });

    $(document).on('click', '.dataset-delete', function(e) {
        e.stopPropagation();
        const datasetId = $(this).closest('.dataset-item').data('dataset');
        const datasetName = $(`#datasetName_${datasetId}`).val() || `Dataset ${datasetId}`;

        // Show confirmation modal instead of directly deleting
        showDeleteDatasetModal(datasetId, datasetName);
    });

    $(document).on('click', '.addAttributeBtn', function() {
        const datasetId = $(this).data('dataset');
        addAttributeToDataset(datasetId);
    });

    $(document).on('click', '.saveAsDraftBtn', function(e) {
        $('#actionType').val('draft');
        $('#datasetForm').submit();
    });

    $(document).on('click', '.submitBtn', function(e) {
        $('#actionType').val('submit');
    });

    $(document).on('click', '.remove-attribute', function() {
        const datasetId = $(this).closest('.dataset-form').data('dataset');
        $(this).closest('.single-attribute-field').remove();
        updateRemoveButtons(datasetId);
        updateAttributeLabels(datasetId);
    });

    $('#excelTemplateBtn').click(function() {
        if (hasExistingFormData() && mode !== 'update') {
            showOverwriteWarningModal();
        } else {
            $('#excelTemplateModal').modal('show');
        }
    });


    function hasExistingFormData() {
        let hasData = false;

        $('.dataset-item').each(function() {
            const datasetId = $(this).data('dataset');

            // Check if any text inputs have values
            $(`#datasetForm_${datasetId} input[type="text"]`).each(function() {
                if ($(this).val() && $(this).val().trim() !== '') {
                    hasData = true;
                    return false; // Break inner loop
                }
            });

            // Check if any selects have values
            $(`#datasetForm_${datasetId} select`).each(function() {
                if ($(this).val() && $(this).val() !== '') {
                    hasData = true;
                    return false; // Break inner loop
                }
            });

            if (hasData) {
                return false; // Break outer loop
            }
        });

        return hasData;
    }


    function showDeleteDatasetModal(datasetId, datasetName) {
        const displayName = datasetName || `Dataset ${datasetId}`;

        const modalHtml = `
        <div class="modal fade" id="deleteDatasetModal" tabindex="-1" role="dialog">
            <div class="modal-dialog modal-md" role="document">
                <div class="modal-content">
                    <div class="modal-body text-center py-4">
                        <div class="mb-4">
                            <div class="delete-icon mb-3">
                                <div class="icon-circle">
                                    <i class="fa fa-trash fa-2x text-white"></i>
                                </div>
                            </div>
                            <h4 class="mb-3">Delete This Dataset</h4>
                            <p class="text-muted mb-0">Are you sure you want to delete "${displayName}"? This action cannot be undone.</p>
                        </div>
                        
                        <div class="d-flex justify-content-center gap-3">
                            <button type="button" class="btn btn-outline-secondary px-4" id="cancelDelete">
                                Cancel
                            </button>
                            <button type="button" class="btn btn-danger px-4" id="confirmDelete">
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;

        // Add modal to body
        $('body').append(modalHtml);

        $('#deleteDatasetModal').modal({
            backdrop: 'static',
            keyboard: false
        });

        // Handle Cancel button
        $('#cancelDelete').on('click', function() {
            $('#deleteDatasetModal').modal('hide');
        });

        // Handle Delete button
        $('#confirmDelete').on('click', function() {
            $('#deleteDatasetModal').modal('hide');

            // Perform the actual delete after modal closes
            setTimeout(() => {
                deleteDataset(datasetId);
            }, 300);
        });

        // Clean up modal when hidden
        $('#deleteDatasetModal').on('hidden.bs.modal', function() {
            $(this).remove();
        });
    }

    // Function to show overwrite warning modal
    function showOverwriteWarningModal() {
        const modalHtml = `
        <div class="modal fade" id="overwriteWarningModal" tabindex="-1" role="dialog">
            <div class="modal-dialog modal-md" role="document">
                <div class="modal-content">
                    <div class="modal-body text-center py-4">
                        <div class="mb-4">
                            <div class="warning-icon mb-3">
                                <i class="fa fa-exclamation-triangle fa-3x text-warning"></i>
                            </div>
                            <h4 class="mb-2">This data will be overwritten</h4>
                            <p class="text-muted mb-0">This action cannot be undone.</p>
                        </div>
                        
                        <div class="d-flex justify-content-center gap-3">
                            <button type="button" class="btn btn-outline-secondary px-4" id="cancelOverwrite">
                                Cancel
                            </button>
                            <button type="button" class="btn btn-primary px-4" id="proceedOverwrite">
                                Proceed
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;

        // Add modal to body
        $('body').append(modalHtml);

        // Show modal
        $('#overwriteWarningModal').modal({
            backdrop: 'static',
            keyboard: false
        });

        // Handle Cancel button
        $('#cancelOverwrite').on('click', function() {
            $('#overwriteWarningModal').modal('hide');
        });

        // Handle Proceed button
        $('#proceedOverwrite').on('click', function() {
            $('#overwriteWarningModal').modal('hide');

            // Clear all datasets and open Excel modal
            setTimeout(() => {
                window.DatasetManager.clearAllDatasets();
                window.DatasetManager.generateFirstDataset(); // Add one empty dataset
                $('#excelTemplateModal').modal('show');
            }, 300);
        });

        // Clean up modal when hidden
        $('#overwriteWarningModal').on('hidden.bs.modal', function() {
            $(this).remove();
        });
    }

    // Dataset management functions
    function generateFirstDataset() {
        console.log('🔍 generateFirstDataset started');

        datasetCounter = 1;
        attributeCounters[1] = 1;

        // Add first dataset to sidebar
        const datasetItemHtml =
            '<div class="dataset-item active" data-dataset="1">' +
            '<span> '+ window.localizedStrings.dataset +' 1</span>' +
            '<i class="fa fa-trash text-danger dataset-delete" style="display:none;"></i>' +
            '</div>';
        $('.dataset-list').append(datasetItemHtml);

        // Generate first dataset form using template
        const firstFormHtml = datasetFormTemplate({
            datasetId: 1,
            displayStyle: 'display: block;'
        });
        $('#formsContainer').append(firstFormHtml);

        updateRemoveButtons(1);
        updateDatasetCount();
    }

    function addNewDataset() {
        datasetCounter++;
        attributeCounters[datasetCounter] = 1;

        // Add sidebar item
        const datasetItemHtml =
            '<div class="dataset-item" data-dataset="' + datasetCounter + '">' +
            '<span> ' + window.localizedStrings.dataset + ' '+ datasetCounter + '</span>' +
            '<i class="fa fa-trash text-danger dataset-delete"></i>' +
            '</div>';
        $('.dataset-list').append(datasetItemHtml);

        // Generate form using template
        const newFormHtml = datasetFormTemplate({
            datasetId: datasetCounter,
            displayStyle: 'display: none;'
        });
        $('#formsContainer').append(newFormHtml);

        switchToDataset(datasetCounter);
        updateDeleteButtonVisibility();
        updateDatasetCount();

        return datasetCounter;
    }

    function switchToDataset(datasetId) {
        $('.dataset-item').removeClass('active');
        $('.dataset-item[data-dataset="' + datasetId + '"]').addClass('active');
        $('.dataset-form').hide();
        $('#datasetForm_' + datasetId).show();
        currentActiveDataset = datasetId;
    }

    function deleteDataset(datasetId) {
        if ($('.dataset-item').length <= 1) {
            return;
        }

        $('.dataset-item[data-dataset="' + datasetId + '"]').remove();
        $('#datasetForm_' + datasetId).remove();
        delete attributeCounters[datasetId];

        if (currentActiveDataset === datasetId) {
            const firstDataset = $('.dataset-item').first().data('dataset');
            switchToDataset(firstDataset);
        }
        updateDeleteButtonVisibility();
        updateDatasetCount();
    }

    function updateDeleteButtonVisibility() {
        if ($('.dataset-item').length > 1) {
            $('.dataset-delete').show();
        } else {
            $('.dataset-delete').hide();
        }
    }

    function addAttributeToDataset(datasetId) {
        if (!attributeCounters[datasetId]) {
            attributeCounters[datasetId] = 1;
        }

        attributeCounters[datasetId]++;
        const attributeCount = attributeCounters[datasetId];
        const namespace = window.portletNamespace;

        const attributeHtml =
            '<div class="single-attribute-field mb-3" data-index="' + attributeCount + '" data-dataset="' + datasetId + '">' +
            '<div class="row">' +
            '<div class="col-md-5">' +
            '<div class="form-group">' +
            '<label> ' + window.localizedStrings.attribute + ' ' + attributeCount + ' <span class="text-danger">*</span></label>' +
            '<input type="text" class="form-control" name="' + namespace + 'attributeName_' + datasetId + '_' + attributeCount + '" placeholder="' + window.localizedStrings.attributeNamePlaceholder +'">' +
            '</div>' +
            '</div>' +
            '<div class="col-md-5">' +
            '<div class="form-group">' +
            '<label> '+ window.localizedStrings.attribute + ' ' + attributeCount + ' ' + window.localizedStrings.attributeDescription  + '  <span class="text-danger">*</span></label>' +
            '<input type="text" class="form-control" name="' + namespace + 'attributeDescription_' + datasetId + '_' + attributeCount + '" placeholder="' + window.localizedStrings.attributeDescriptionPlaceholder +'">' +
            '</div>' +
            '</div>' +
            '<div class="col-md-2 d-flex align-items-end">' +
            '<div class="form-group w-100">' +
            '<button type="button" class="btn btn-outline-danger remove-attribute w-100">' +
            '<i class="fa fa-trash"></i>' +
            '</button>' +
            '</div>' +
            '</div>' +
            '</div>' +
            '</div>';

        $('#attributesContainer_' + datasetId).append(attributeHtml);
        updateRemoveButtons(datasetId);
    }

    function updateRemoveButtons(datasetId) {
        const attributeFields = $('#attributesContainer_' + datasetId + ' .single-attribute-field');
        if (attributeFields.length === 1) {
            attributeFields.find('.remove-attribute').hide();
        } else {
            attributeFields.find('.remove-attribute').show();
        }
    }

    function updateDatasetCount() {
        const totalDatasets = $('.dataset-item').length;
        $('#totalDatasets').val(totalDatasets);

        // Log all current dataset IDs for debugging
        const datasetIds = [];
        $('.dataset-item').each(function() {
            datasetIds.push($(this).data('dataset'));
        });
    }

    function updateAttributeLabels(datasetId) {
        $('#attributesContainer_' + datasetId + ' .single-attribute-field').each(function(index) {
            const newIndex = index + 1;
            $(this).attr('data-index', newIndex);

            $(this).find('input[name*="attributeName_"]').attr('name', window.portletNamespace + 'attributeName_' + datasetId + '_' + newIndex);
            $(this).find('input[name*="attributeDescription_"]').attr('name', window.portletNamespace + 'attributeDescription_' + datasetId + '_' + newIndex);

            $(this).find('label').each(function() {
                const labelText = $(this).text();
                if (labelText.includes(window.localizedStrings.attribute)) {
                    if (labelText.includes(window.localizedStrings.attributeDescription)) {
                        $(this).html(window.localizedStrings.attribute + ' ' + newIndex + ' ' +window.localizedStrings.attributeDescription + ' <span class="text-danger">*</span>');
                    } else {
                        $(this).html(window.localizedStrings.attribute + ' '+ newIndex + ' <span class="text-danger">*</span>');
                    }
                }
            });
        });
        attributeCounters[datasetId] = $('#attributesContainer_' + datasetId + ' .single-attribute-field').length;
    }

    // Functions for file upload integration
    function clearAllDatasets() {
        $('.dataset-item').remove();
        $('#formsContainer').empty();
        datasetCounter = 0;
        attributeCounters = {};
        currentActiveDataset = 1;
    }


    function populateDatasetAttributes(datasetId, attributes) {
        // Clear existing attributes except the first one
        $(`#attributesContainer_${datasetId} .single-attribute-field:not(:first)`).remove();

        attributes.forEach(function(attribute, index) {
            const attributeIndex = index + 1;

            // Add new attribute field if needed (except for first one)
            if (index > 0) {
                addAttributeToDataset(datasetId);
            }

            // Use metadata to get correct field names
            const attributeNameValue = fieldMetadata.attributesField ?
                attribute[fieldMetadata.attributesField] :
                getFirstAvailableValue(attribute);

            const attributeDescValue = fieldMetadata.attributeDescriptionField ?
                attribute[fieldMetadata.attributeDescriptionField] :
                getAttributeDescriptionValue(attribute);

            // Populate the form fields
            const nameInput = $(`input[name="${window.portletNamespace}attributeName_${datasetId}_${attributeIndex}"]`);
            const descInput = $(`input[name="${window.portletNamespace}attributeDescription_${datasetId}_${attributeIndex}"]`);

            if (nameInput.length && attributeNameValue) {
                nameInput.val(attributeNameValue);
            }

            if (descInput.length && attributeDescValue) {
                descInput.val(attributeDescValue);
            }
        });

        // Update remove buttons and labels
        updateRemoveButtons(datasetId);
        updateAttributeLabels(datasetId);
    }

    function populateDatasetFields(datasetId, dataset) {
        // Check if form exists
        const form = $(`#datasetForm_${datasetId}`);
        if (form.length === 0) {
            console.error('Dataset form not found for ID:', datasetId);
            return;
        }

        if (!fieldMetadata) {
            console.error('No field metadata available - falling back to direct mapping');
            return;
        }


        // Dataset name field
        if (fieldMetadata.datasetNameField && dataset[fieldMetadata.datasetNameField]) {
            const field = $(`#datasetName_${datasetId}`);
            if (field.length > 0) {
                field.val(dataset[fieldMetadata.datasetNameField]);
            } else {
                console.error('Dataset name form field not found');
            }
        } else {
            console.warn('Dataset name field not found in metadata or data');
        }

        // Handle all regular fields dynamically
        if (fieldMetadata.regularFields && fieldMetadata.regularFields.length > 0) {
            for (let i = 0; i < fieldMetadata.regularFields.length; i++) {
                const fieldName = fieldMetadata.regularFields[i];
                if (dataset[fieldName]) {
                    populateRegularField(datasetId, fieldName, dataset[fieldName]);
                } else {
                    console.warn(`Regular field '${fieldName}' not found in dataset data`);
                }
            }
        } else {
            console.warn('No regular fields found in metadata');
        }

        // Handle attributes
        if (dataset.attributes && dataset.attributes.length > 0) {
            setTimeout(function() {
                populateDatasetAttributes(datasetId, dataset.attributes);
            }, 300);
        } else {
            console.warn('No attributes found in dataset');
        }
    }


    // Helper functions for attribute mapping
    function getFirstAvailableValue(obj) {
        const values = Object.values(obj);
        return values.find(val => val && val.trim().length > 0) || '';
    }

    function getAttributeDescriptionValue(attribute) {
        const values = Object.values(attribute);
        if (values.length >= 2) {
            return values[1];
        }
        return '';
    }


    // Add this function to store field metadata
    function setFieldMetadata(metadata) {
        fieldMetadata = metadata;
    }

    function populateRegularField(datasetId, fieldName, value) {
        // Skip the "#" field
        if (fieldName === '#' || fieldName.trim() === '#') {
            return true;
        }

        // Get the position of this field in the regularFields array
        if (!fieldMetadata || !fieldMetadata.regularFields) {
            console.error('No field metadata or regularFields available');
            return false;
        }

        const fieldIndex = fieldMetadata.regularFields.indexOf(fieldName);

        if (fieldIndex === -1) {
            return false;
        }

        // Adjust the mapping
        const formFieldIndex = fieldIndex - 1;

        // Map adjusted position to form field selector
        const formFieldSelectors = [
            `#datasetDescription_${datasetId}`,
            `#datasetClassification_${datasetId}`,
            `#userDemand_${datasetId}`,
            `#economicImpact_${datasetId}`,
            `#betterServices_${datasetId}`,
            `#betterGovernance_${datasetId}`,
            `#definedOwner_${datasetId}`,
            `#existingMetadata_${datasetId}`,
            `#alreadyPublished_${datasetId}`,
            `#openFormat_${datasetId}`,
            `#releaseYear_${datasetId}`,
            `#releaseMonth_${datasetId}`
        ];

        if (formFieldIndex >= 0 && formFieldIndex < formFieldSelectors.length) {
            const selector = formFieldSelectors[formFieldIndex];
            const field = $(selector);

            if (field.length > 0) {
                field.val(value);
                return true;
            } else {
                $(`#datasetForm_${datasetId} input, #datasetForm_${datasetId} select`).each(function() {
                    const id = $(this).attr('id');
                    if (id) console.log(`  - ${id}`);
                });
            }
        } else if (formFieldIndex < 0) {
            return true;
        } else {
            console.warn(`Adjusted position ${formFieldIndex} exceeds available form fields. You may need to add more selectors.`);
        }
        return false;
    }

    function populateFormsWithData(datasets, metadata) {
        console.log("called")
        // Store the metadata globally
        if (metadata) {
            fieldMetadata = metadata;
        }

        // Clear existing forms first
        clearAllDatasets();

        // Process datasets with proper timing
        datasets.forEach(function(datasetData, index) {
            const datasetId = index + 1;

            // Add new dataset if needed
            if (index === 0) {
                // Generate first dataset
                generateFirstDataset();

                // wait for form to be ready before populating
                setTimeout(function() {
                    populateDatasetFields(datasetId, datasetData);
                }, 500);
            } else {
                // Add additional datasets
                const newDatasetId = addNewDataset();

                // Wait longer for additional datasets
                setTimeout(function() {
                    populateDatasetFields(datasetId, datasetData);
                }, 500 + (200 * index));
            }

            window.ReviewMode.updateSidebarDatasetName(datasetId,datasetData['Dataset Name']);
        });

        // Switch to first dataset after all processing
        setTimeout(function() {
            if (datasets.length > 0) {
                switchToDataset(1);
            }
            // Update counters
            updateDatasetCount();
        }, 1000 + (200 * datasets.length));
    }


    // Export functions for file upload integration
    window.DatasetManager = {
        addNewDataset: addNewDataset,
        switchToDataset: switchToDataset,
        clearAllDatasets: clearAllDatasets,
        generateFirstDataset: generateFirstDataset,
        populateFormsWithData: populateFormsWithData,
        populateDatasetFields: populateDatasetFields,
        setFieldMetadata: setFieldMetadata,
        addAttributeToDataset: addAttributeToDataset,
        updateRemoveButtons: updateRemoveButtons,
        updateAttributeLabels: updateAttributeLabels
    };

    // Expose variables that might be needed by file upload script
    window.datasetFormTemplate = datasetFormTemplate;
    window.attributeCounters = attributeCounters;
    window.datasetCounter = datasetCounter;
});