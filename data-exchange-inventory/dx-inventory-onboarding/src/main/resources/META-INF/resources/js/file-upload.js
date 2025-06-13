$(document).ready(function() {
    let uploadedFile = null;
    let validationPassed = false;

    // Initialize file upload functionality
    initializeFileUpload();

    function initializeFileUpload() {
        setupFileDropArea();
        setupEventHandlers();
    }

    function setupFileDropArea() {
        const uploadArea = $('#uploadArea');
        const fileInput = $('#templateFileInput');

        uploadArea.on('click', function() {
            if (!$(this).hasClass('disabled')) {
                fileInput.click();
            }
        });

        // Drag and drop
        uploadArea.on('dragover', function(e) {
            e.preventDefault();
            e.stopPropagation();
            $(this).addClass('border-primary');
        });

        uploadArea.on('dragleave', function(e) {
            e.preventDefault();
            e.stopPropagation();
            $(this).removeClass('border-primary');
        });

        uploadArea.on('drop', function(e) {
            e.preventDefault();
            e.stopPropagation();
            $(this).removeClass('border-primary');

            const files = e.originalEvent.dataTransfer.files;
            if (files.length > 0) {
                handleFileSelection(files[0]);
            }
        });

        // File input change
        fileInput.on('change', function(e) {
            if (e.target.files.length > 0) {
                handleFileSelection(e.target.files[0]);
            }
        });
    }

    function setupEventHandlers() {
        // Remove uploaded file
        $('#removeUploadedFile').on('click', function() {
            removeUploadedFile();
        });

        // Change file button
        $('#changeFileBtn').on('click', function() {
            $('#templateFileInput').click();
        });

        // Validate file button
        $('#validateFileBtn').on('click', function() {
            validateUploadedFile();
        });

        // Process template button
        $('#processTemplateBtn').on('click', function() {
            if (validationPassed && uploadedFile) {
                processFileAndPopulateForms();
            }
        });
    }

    function handleFileSelection(file) {
        // Validate file type
        const allowedTypes = ['.xlsx', '.xls'];
        const fileName = file.name.toLowerCase();
        const fileExtension = '.' + fileName.split('.').pop();

        if (!allowedTypes.includes(fileExtension)) {
            showErrorMessage(window.validationMessages.invalidFileType);
            return;
        }

        // Validate file size (10MB limit)
        if (file.size > 10 * 1024 * 1024) {
            showErrorMessage(window.validationMessages.fileSizeLimit);
            return;
        }

        // Store file and update UI
        uploadedFile = file;
        displayUploadedFile(file);
        updateValidateButtonState();
        resetValidation();
    }

    function displayUploadedFile(file) {
        const fileName = file.name;
        const fileSize = formatFileSize(file.size);

        $('#uploadedFileName').text(fileName);
        $('#uploadedFileSize').text(fileSize);

        $('.upload-placeholder').hide();
        $('#uploadedFileInfo').show();
    }

    function removeUploadedFile() {
        uploadedFile = null;
        validationPassed = false;

        $('#templateFileInput').val('');
        $('#uploadedFileInfo').hide();
        $('.upload-placeholder').show();
        $('#validationResults').hide();

        updateValidateButtonState();
        updateProcessButtonState();
    }

    function updateValidateButtonState() {
        const hasFile = uploadedFile !== null;
        $('#validateFileBtn').prop('disabled', !hasFile);
    }

    function updateProcessButtonState() {
        $('#processTemplateBtn').prop('disabled', !validationPassed);
    }

    function resetValidation() {
        validationPassed = false;
        $('#validationResults').hide();
        updateProcessButtonState();
    }

    function validateUploadedFile() {
        if (!uploadedFile) {
            showErrorMessage(window.validationMessages.selectFile);
            return;
        }

        showLoadingState(window.validationMessages.validatingFiles);

        // Create FormData for file upload
        const formData = new FormData();
        formData.append('file', uploadedFile);

        const userLocale = window.navigator.language || 'en-US';
        const language = userLocale.split('-')[0];
        formData.append('language', language);
        formData.append('namespace', window.portletNamespace);

        // Send validation request
        $.ajax({
            url: window.portletURLs.validateFile,
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            timeout: 30000,
            success: function(response) {
                handleValidationResponse(response);
            },
            error: function(xhr, status, error) {
                console.error('Validation AJAX error:', {
                    status: status,
                    error: error,
                    responseText: xhr.responseText
                });

                let errorMessage =  window.validationMessages.errorValidatingFile +': ';
                if (status === 'timeout') {
                    errorMessage += window.validationMessages.requestTimeOut;
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

                showErrorMessage(errorMessage);
                hideLoadingState();
            }
        });
    }

    function handleValidationResponse(response) {
        hideLoadingState();

        try {
            const result = typeof response === 'string' ? JSON.parse(response) : response;
            if (result.success) {
                validationPassed = true;
                displayValidationResults(result);
                updateProcessButtonState();
                showSuccessMessage(window.validationMessages.fileValidationSuccess);
            } else {
                validationPassed = false;
                displayValidationErrors(result.errors);
                updateProcessButtonState();
            }
        } catch (e) {
            console.error('Error parsing validation response:', e);
            showErrorMessage(window.validationMessages.errorParsingValidation);
        }
    }

    function displayValidationResults(details) {
        let html = '<div class="validation-success">';
        html += '<h6 class="text-success"><i class="fa fa-check-circle"></i> '+ window.validationMessages.validationPassed + '</h6>';
        html += '<ul class="list-unstyled mb-0">';

        if (details.columnCount) {
            html += `<li><i class="fa fa-check text-success"></i> <strong> ` + window.validationMessages.columns+ `:</strong> ${details.columnCount} (matches template)</li>`;
        }
        if (details.rowCount) {
            html += `<li><i class="fa fa-check text-success"></i> <strong>` + window.validationMessages.dataRows + `:</strong> ${details.rowCount}</li>`;
        }
        if (details.dataTypes) {
            html += `<li><i class="fa fa-check text-success"></i> <strong>` + window.validationMessages.dataValidation + `:</strong> ${details.dataTypes}</li>`;
        }
        if (details.encoding) {
            html += `<li><i class="fa fa-check text-success"></i> <strong>` + window.validationMessages.encoding + `:</strong> ${details.encoding}</li>`;
        }

        html += '</ul></div>';

        $('#validationDetails').html(html);
        $('#validationResults').removeClass('alert-danger').addClass('alert-success').show();
    }

    function displayValidationErrors(errors) {
        let html = '<div class="validation-errors">';
        html += '<h6 class="text-danger"><i class="fa fa-exclamation-triangle"></i>' + window.validationMessages.validationFailed  +'</h6>';

        if (errors && errors.length > 0) {
            html += '<div class="mt-3">';
            html += '<p><strong>' +window.validationMessages.fixIssue + ':</strong></p>';
            html += '<ul class="list-unstyled mb-0">';

            // Group errors by type for better display
            const structuralErrors = [];
            const dataErrors = [];

            errors.forEach(function(error) {
                if (error.includes('Row ') && error.includes('Column ')) {
                    dataErrors.push(error);
                } else {
                    structuralErrors.push(error);
                }
            });

            // Display structural errors first
            if (structuralErrors.length > 0) {
                html += '<li class="mb-2"><strong>' + window.validationMessages.fileStructureIssue +':</strong></li>';
                structuralErrors.forEach(function(error) {
                    html += `<li class="ml-3"><i class="fa fa-times text-danger"></i> ${error}</li>`;
                });
            }

            // Display data validation errors
            if (dataErrors.length > 0) {
                html += '<li class="mb-2 mt-2"><strong>' + window.validationMessages.dataValidationIssue + ':</strong></li>';

                // Limit displayed data errors to prevent overwhelming the user
                const maxDisplayErrors = 10;
                const displayErrors = dataErrors.slice(0, maxDisplayErrors);

                displayErrors.forEach(function(error) {
                    html += `<li class="ml-3"><i class="fa fa-times text-danger"></i> ${error}</li>`;
                });

                if (dataErrors.length > maxDisplayErrors) {
                    html += `<li class="ml-3 text-muted">... and ${dataErrors.length - maxDisplayErrors} ` + window.validationMessages.moreErrors +`</li>`;
                }
            }

            html += '</ul></div>';
        } else {
           console.error("validation error occurred.")
        }

        html += '</div>';

        $('#validationDetails').html(html);
        $('#validationResults').removeClass('alert-success').addClass('alert-danger').show();
    }

    function processFileAndPopulateForms() {
        if (!validationPassed || !uploadedFile) {
            showErrorMessage(window.validationMessages.validateFileFirst);
            return;
        }

        showLoadingState(window.validationMessages.processingFile);

        const formData = new FormData();
        formData.append('file', uploadedFile);
        formData.append('namespace', window.portletNamespace);

        $.ajax({
            url: window.portletURLs.processFile,
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            timeout: 60000, // 60 second timeout for processing
            success: function(response) {
                handleProcessResponse(response);
            },
            error: function(xhr, status, error) {
                console.error('Processing error:', error);

                let errorMessage = window.validationMessages.errorProcessingFile + ': ';
                if (status === 'timeout') {
                    errorMessage += window.validationMessages.processingTimeout;
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

                showErrorMessage(errorMessage);
                hideLoadingState();
            }
        });
    }

    function handleProcessResponse(response) {
        hideLoadingState();

        try {
            const result = typeof response === 'string' ? JSON.parse(response) : response;

            if (result.success && result.datasets) {

                // EXTRACT AND STORE FIELD METADATA
                if (result.fieldMetadata) {

                    // Store metadata globally so populateDatasetFields can access it
                    if (window.DatasetManager) {
                        window.DatasetManager.setFieldMetadata(result.fieldMetadata);
                    }

                    // Also store it in a global variable for backward compatibility
                    window.fieldMetadata = result.fieldMetadata;
                } else {
                    console.warn('No field metadata received in response');
                }

                if (window.DatasetManager && result.datasets.length > 0) {
                        const mode = document.getElementById('userMode').value;
                        const isUpdateMode = mode === 'update';
                        if (isUpdateMode) {
                            // In update mode: merge datasets instead of replacing
                            handleDatasetMerging(result.datasets, result.fieldMetadata);
                         } else {
                            // Pass both datasets and metadata
                            window.DatasetManager.populateFormsWithData(result.datasets, result.fieldMetadata);
                            $('#excelTemplateModal').modal('hide');
                            showSuccessMessage(window.validationMessages.successfullyLoaded + ` ${result.datasets.length} ` + window.validationMessages.datasetFromFile);

                            // Show a summary of what was loaded
                            setTimeout(() => {
                                showDatasetSummary(result.datasets);
                            }, 1000);
                        }

                } else {
                        showErrorMessage(window.validationMessages.fileEmpty);
                }
            }
             else {
                const errorMsg = result.error || window.validationMessages.errorProcessingFile;
                showErrorMessage(errorMsg);
            }
        } catch (e) {
            showErrorMessage(window.validationMessages.errorParsingFileData);
        }
    }

    function handleDatasetMerging(excelDatasets, fieldMetadata) {
        // Get existing datasets from the form
        const existingDatasets = getExistingDatasetsFromForm();

        // Check for duplicates
        const duplicates = findDuplicateDatasets(existingDatasets, excelDatasets);

        if (duplicates.length > 0) {
            // Show duplicate confirmation dialog
            showDuplicateConfirmationDialog(duplicates, excelDatasets, existingDatasets, fieldMetadata);
        } else {
            // No duplicates, proceed with merging
            mergeDatasets(existingDatasets, excelDatasets, fieldMetadata);
        }
    }

    function getExistingDatasetsFromForm() {
        const existingDatasets = [];

        $('.dataset-item').each(function() {
            const datasetId = $(this).data('dataset');
            const datasetName = $(`#datasetName_${datasetId}`).val() || `Dataset ${datasetId}`;

            // Extract all form data for this dataset
            const datasetData = {
                formDatasetId: datasetId,
                datasetName: datasetName,
                datasetDescription: $(`#datasetDescription_${datasetId}`).val() || '',
                datasetClassification: $(`#datasetClassification_${datasetId}`).val() || '',
                userDemand: $(`#userDemand_${datasetId}`).val() || '',
                economicImpact: $(`#economicImpact_${datasetId}`).val() || '',
                betterServices: $(`#betterServices_${datasetId}`).val() || '',
                betterGovernance: $(`#betterGovernance_${datasetId}`).val() || '',
                definedOwner: $(`#definedOwner_${datasetId}`).val() || '',
                existingMetadata: $(`#existingMetadata_${datasetId}`).val() || '',
                alreadyPublished: $(`#alreadyPublished_${datasetId}`).val() || '',
                openFormat: $(`#openFormat_${datasetId}`).val() || '',
                releaseYear: $(`#releaseYear_${datasetId}`).val() || '',
                releaseMonth: $(`#releaseMonth_${datasetId}`).val() || '',
                attributes: getAttributesFromForm(datasetId)
            };

            existingDatasets.push(datasetData);
        });

        return existingDatasets;
    }

    function getAttributesFromForm(datasetId) {
        const attributes = [];

        $(`#attributesContainer_${datasetId} .single-attribute-field`).each(function() {
            const attributeIndex = $(this).data('index');
            const attributeName = $(`input[name="${window.portletNamespace}attributeName_${datasetId}_${attributeIndex}"]`).val();
            const attributeDescription = $(`input[name="${window.portletNamespace}attributeDescription_${datasetId}_${attributeIndex}"]`).val();

            if (attributeName || attributeDescription) {
                attributes.push({
                    attributeName: attributeName || '',
                    attributeDescription: attributeDescription || ''
                });
            }
        });

        return attributes;
    }

    function findDuplicateDatasets(existingDatasets, excelDatasets) {

        const duplicates = [];
        excelDatasets.forEach(excelDataset => {
            const excelName = (excelDataset['Dataset Name'] || '').trim().toLowerCase();

            existingDatasets.forEach(existingDataset => {
                const existingName = (existingDataset.datasetName || '').trim().toLowerCase();
                if (excelName && existingName && excelName === existingName) {

                    duplicates.push({
                        name: excelDataset['Dataset Name'],
                        existing: existingDataset,
                        excel: excelDataset
                    });
                }
            });
        });

        return duplicates;
    }


    function showDuplicateConfirmationDialog(duplicates, excelDatasets, existingDatasets, fieldMetadata) {
        const duplicateNames = duplicates.map(d => d.name).join(', ');

        // Create and show modal
        const modalHtml = `
        <div class="modal fade" id="duplicateConfirmModal" tabindex="-1" role="dialog">
            <div class="modal-dialog modal-lg" role="document">
                <div class="modal-content">
                    <div class="modal-header bg-warning text-white">
                        <h5 class="modal-title">
                            <i class="fa fa-exclamation-triangle"></i> Dataset Name Conflicts Detected
                        </h5>
                        <button type="button" class="close text-white" data-dismiss="modal">
                            <span>&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <div class="alert alert-warning">
                            <h6><i class="fa fa-exclamation-triangle"></i> Some datasets will be overwritten</h6>
                            <p class="mb-2"><strong>Conflicting datasets:</strong> ${duplicateNames}</p>
                            <p class="mb-0">These datasets exist in both your current draft and the Excel file. The Excel data will overwrite the existing draft data for these datasets.</p>
                            <p class="mt-2 mb-0"><strong>This action cannot be undone.</strong></p>
                        </div>
                        
                        <div class="row">
                            <div class="col-md-6">
                                <h6>Current Draft Datasets:</h6>
                                <ul class="list-group list-group-flush">
                                    ${existingDatasets.map(d => `<li class="list-group-item">${d.datasetName}</li>`).join('')}
                                </ul>
                            </div>
                            <div class="col-md-6">
                                <h6>Excel File Datasets:</h6>
                                <ul class="list-group list-group-flush">
                                    ${excelDatasets.map(d => `<li class="list-group-item">${d['Dataset Name'] || 'Unnamed Dataset'}</li>`).join('')}
                                </ul>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal" id="cancelMerge">Cancel</button>
                        <button type="button" class="btn btn-warning" id="proceedWithMerge">
                            <i class="fa fa-check"></i> Proceed with Merge
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;

        // Add modal to body
        $('body').append(modalHtml);

        // Show modal
        $('#duplicateConfirmModal').modal('show');

        // Handle proceed button
        $('#proceedWithMerge').on('click', function() {
            $('#duplicateConfirmModal').modal('hide');
            window.DatasetManager.clearAllDatasets();
            mergeDatasets([], excelDatasets, fieldMetadata);
        });

        // Clean up modal when hidden
        $('#duplicateConfirmModal').on('hidden.bs.modal', function() {
            $(this).remove();
        });

        $('#cancelMerge').on('click', function (){
            $('#duplicateConfirmModal').modal('hide');
            $('#excelTemplateModal').modal('hide');
        });
    }

    function mergeDatasets(existingDatasets, excelDatasets, fieldMetadata) {
        // Store field metadata
        if (window.DatasetManager) {
            window.DatasetManager.setFieldMetadata(fieldMetadata);
        }

        // Process Excel datasets one by one
        excelDatasets.forEach((excelDataset, index) => {
            const excelName = (excelDataset.datasetName || '').trim().toLowerCase();
            let targetDatasetId = null;

            // Find if this dataset name already exists
            $('.dataset-item').each(function() {
                const datasetId = $(this).data('dataset');
                const existingName = ($(`#datasetName_${datasetId}`).val() || '').trim().toLowerCase();

                if (existingName === excelName && excelName) {
                    targetDatasetId = datasetId;
                    return false; // Break loop
                }
            });

            if (targetDatasetId) {
                // Update existing dataset
                setTimeout(() => {
                    if (window.DatasetManager) {
                        window.DatasetManager.populateDatasetFields(targetDatasetId, excelDataset);
                    }
                }, 300 + (100 * index));
            } else {
                // Add new dataset
                setTimeout(() => {
                    const newDatasetId = window.DatasetManager.addNewDataset();
                    setTimeout(() => {
                        window.DatasetManager.populateDatasetFields(newDatasetId, excelDataset);
                        setTimeout(() => {
                            const datasetName = excelDataset['Dataset Name'] || `Dataset ${newDatasetId}`;
                            window.ReviewMode.updateSidebarDatasetName(newDatasetId, datasetName);
                        }, 200)
                    }, 200);
                }, 300 + (100 * index));
            }
        });

        // Close modal and show success
        $('#excelTemplateModal').modal('hide');

        const duplicatesCount = excelDatasets.filter(excel => {
            const excelName = (excel.datasetName || '').trim().toLowerCase();
            return existingDatasets.some(existing =>
                (existing.datasetName || '').trim().toLowerCase() === excelName && excelName
            );
        }).length;

        const newCount = excelDatasets.length - duplicatesCount;

        setTimeout(() => {
            showMergedDatasetSummary(duplicatesCount, newCount);
        }, 1000);
    }

    function showMergedDatasetSummary(updatedCount, newCount) {
        const totalDatasets = $('.dataset-item').length;

        let summary = `Dataset Merge Complete:\n\n`;
        summary += `• Total datasets: ${totalDatasets}\n`;
        summary += `• New from Excel: ${newCount}\n`;
        summary += `• Updated from Excel: ${updatedCount}\n\n`;
        summary += `Current datasets:\n`;

        // Get current dataset names from the form
        $('.dataset-item').each(function() {
            const datasetId = $(this).data('dataset');
            const name = $(`#datasetName_${datasetId}`).val() || `Dataset ${datasetId}`;
            const attrCount = $(`#attributesContainer_${datasetId} .single-attribute-field`).length || 0;
            summary += `• ${name} (${attrCount} attributes)\n`;
        });

        if (window.showToast) {
            window.showToast(summary, 'info', 'Merge Summary');
        } else {
            alert(summary);
        }
    }

    function showDatasetSummary(datasets) {
        if (datasets && datasets.length > 0) {
            let summary = `Loaded ${datasets.length} dataset(s):\n\n`;
            datasets.forEach((dataset, index) => {
                const name = dataset.datasetName || `Dataset ${index + 1}`;
                const attrCount = dataset.attributes ? dataset.attributes.length : 0;
                summary += `• ${name} (${attrCount} attributes)\n`;
            });

            if (window.showToast) {
                window.showToast(summary, 'info', window.validationMessages.datasetLoaded);
            } else {
                alert(summary);
            }
        }
    }

    // Utility functions
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    function showLoadingState(message) {
        $('#validateFileBtn, #processTemplateBtn').prop('disabled', true);

        // Show loading indicator if available
        if ($('#loadingIndicator').length === 0) {
            const loadingHtml = `
                <div id="loadingIndicator" class="text-center mt-3">
                    <div class="spinner-border spinner-border-sm text-primary" role="status">
                        <span class="sr-only">Loading...</span>
                    </div>
                    <span class="ml-2">${message}</span>
                </div>
            `;
            $('#validationResults').after(loadingHtml);
        }
    }

    function hideLoadingState() {
        $('#loadingIndicator').remove();
        updateValidateButtonState();
        updateProcessButtonState();
    }

    function showSuccessMessage(message) {
        if (window.showToast) {
            window.showToast(message, 'success', 'Success');
        } else {
            alert(message);
        }
    }

    function showErrorMessage(message) {
        if (window.showToast) {
            window.showToast(message, 'error', 'Error');
        } else {
            alert('Error: ' + message);
        }
    }

    // Export file upload specific functions
    window.FileUploadHandler = {
        validateFile: validateUploadedFile,
        processFile: processFileAndPopulateForms,
        clearUpload: removeUploadedFile,
        isValidationPassed: () => validationPassed,
        getUploadedFile: () => uploadedFile
    };
});