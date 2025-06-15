<%@ include file="/init.jsp" %>

<portlet:actionURL name="/updateInventoryScore" var="updateInventoryScoreURL" />


<div class="review-form-container">
    <div class="card shadow-sm">
        <!-- Header -->
        <div class="card-header">
            <div class="d-flex align-items-center justify-content-center">
                <div class="text-center">
                    <h4 class="mb-0"><liferay-ui:message key="rate.the.compliance" /></h4>
                    <small class="opacity-75"><liferay-ui:message key="review.description.text" /></small>
                </div>
            </div>
        </div>

        <div class="card-body">
            <form id="reviewForm" action="${updateInventoryScoreURL}" method="post">
                <input type="hidden" name="<portlet:namespace />inventoryId" value="<%= inventoryId %>" />

                <!-- Tab Navigation -->
                <div class="review-navigation mb-4">
                    <div class="nav-pills-custom">
                        <button type="button" class="nav-pill active" data-target="inventory">
                            <i class="fa fa-database"></i> <liferay-ui:message key="inventory" />
                        </button>
                        <button type="button" class="nav-pill" data-target="prioritization">
                            <i class="fa fa-sort-amount-up"></i> <liferay-ui:message key="prioritization" />
                        </button>
                        <button type="button" class="nav-pill" data-target="classification">
                            <i class="fa fa-tags"></i> <liferay-ui:message key="classification" />
                        </button>
                        <button type="button" class="nav-pill" data-target="release-plan">
                            <i class="fa fa-calendar"></i> <liferay-ui:message key="release.plan" />
                        </button>
                    </div>
                </div>

                <div class="row">
                    <!-- Dynamic Tab Content Area - Only Compliance Rating -->
                    <div class="col-md-6">
                        <div class="tab-content-area" id="tabContentArea">
                            <!-- Compliance rating content will be dynamically loaded here -->
                        </div>
                    </div>

                    <!-- Request State Section - Static (Not Cloned) -->
                    <div class="col-md-6">
                        <div class="section-container">
                            <h5><liferay-ui:message key="request.state" /></h5>
                            <p class="text-muted mb-3">
                                <liferay-ui:message key="choose.your.action.for.submission" />
                            </p>
                            <div class="request-actions-container">
                                <div class="request-actions">
                                    <button type="button" class="action-btn approve-btn" data-action="approve">
                                        <i class="fa fa-check-circle"></i>
                                        <span><liferay-ui:message key="approve" /></span>
                                    </button>
                                    <button type="button" class="action-btn reject-btn" data-action="reject">
                                        <i class="fa fa-times-circle"></i>
                                        <span><liferay-ui:message key="reject" /></span>
                                    </button>
                                    <button type="button" class="action-btn clarification-btn" data-action="return-for-clarification">
                                        <i class="fa fa-question-circle"></i>
                                        <span><liferay-ui:message key="return.for.clarification" /></span>
                                    </button>
                                </div>
                            </div>
                            <input type="hidden" name="<portlet:namespace />overallRequestState" id="overallRequestState" value="" />
                        </div>
                    </div>
                </div>

                <!-- Overall Feedback Section -->
                <div class="overall-feedback-section mt-4">
                    <div class="row">
                        <div class="col-12">
                            <div class="feedback-container">
                                <h5><liferay-ui:message key="feedback" /> (<liferay-ui:message key="optional" />)</h5>
                                <div class="feedback-note mb-2">
                                    <small class="text-muted">
                                        <liferay-ui:message key="share.your.feedback" />
                                    </small>
                                </div>
                                <textarea class="form-control" name="<portlet:namespace />overallFeedback"
                                          id="overallFeedback" rows="4"></textarea>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Form Actions -->
                <div class="form-actions mt-4 d-flex justify-content-between align-items-center">
                    <button type="button" class="btn btn-outline-secondary" id="backBtn">
                        <i class="fa fa-arrow-left"></i>
                        <liferay-ui:message key="back" />
                    </button>

                    <div class="action-buttons">
                        <button type="button" class="btn btn-outline-secondary" id="cancelReviewBtn">
                            <liferay-ui:message key="cancel" />
                        </button>
                        <button type="submit" class="submit-btn" id="submitReviewBtn">
                            <liferay-ui:message key="submit" />
                        </button>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Hidden Template for Compliance Rating Only -->
<div id="sectionTemplate" style="display: none;">
    <div class="section-content-template">
        <div class="section-container">
            <h5><liferay-ui:message key="compliance.rating" /></h5>
            <p class="text-muted mb-3 section-description">
                <!-- Description will be updated dynamically -->
            </p>
            <div class="rating-buttons-container">
                <div class="rating-buttons" data-section="SECTION_NAME">
                    <button type="button" class="rating-btn" data-rating="0">0</button>
                    <button type="button" class="rating-btn" data-rating="1">1</button>
                    <button type="button" class="rating-btn" data-rating="2">2</button>
                </div>
            </div>
            <input type="hidden" name="<portlet:namespace />SECTION_NAMERating" id="SECTION_NAMERating" value="" />
        </div>
    </div>
</div>

<style>
    .review-form-container {
        max-width: 1200px;
        margin: 0 auto;
    }

    .nav-pills-custom {
        display: flex;
        gap: 0;
        background: #f8f9fa;
        border-radius: 25px;
        padding: 4px;
        width: 60%;
        margin: 0 auto;
    }

    .nav-pill {
        flex: 1;
        background: transparent;
        border: none;
        padding: 10px 15px;
        border-radius: 20px;
        font-weight: 500;
        color: #6c757d;
        transition: all 0.3s ease;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        font-size: 14px;
    }

    .nav-pill:hover {
        color: #495057;
        background: rgba(33, 150, 243, 0.1);
    }

    .nav-pill.active {
        background: #2196f3;
        color: white;
    }


    .tab-content-area {
        min-height: 220px;
        animation: fadeIn 0.3s ease-in-out;
    }

    .section-container {
        background: #f8f9fa;
        padding: 25px;
        border-radius: 12px;
        min-height: 200px;
    }


    /* Rating Section */
    .rating-buttons-container {
        text-align: center;
        margin-top: 20px;
    }

    .rating-buttons {
        display: flex;
        justify-content: center;
        gap: 15px;
        margin: 20px 0;
    }
    .submit-btn{
        background-color: #005cb9;
        color: white;
    }

    .rating-btn {
        width: 50px;
        height: 50px;
        border-radius: 8px;
        border: 2px solid #dee2e6;
        background: white;
        font-size: 18px;
        font-weight: bold;
        cursor: pointer;
        transition: all 0.3s ease;
        color: #6c757d;
    }


    .rating-btn:hover {
        border-color: #2196f3;
        color: #2196f3;
        transform: translateY(-2px);
    }

    .rating-btn.active {
        background-color: #2196f3;
        border-color: #2196f3;
        color: white;
        transform: translateY(-2px);
    }

    /* Request Actions */
    .request-actions-container {
        margin-top: 20px;
    }

    .request-actions {
        display: flex;
        flex-direction: row;
        gap: 12px;
    }

    .action-btn {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 10px 20px;
        border: 2px solid #dee2e6;
        border-radius: 8px;
        cursor: pointer;
        font-size: 14px;
    }

    .action-btn:hover {
        border-color: #2196f3;
        background: #f8f9fa;
    }

    .action-btn.selected {
        border-color: #2196f3;
        background: #e3f2fd;
    }

    .approve-btn i {
        color: #28a745;
        font-size: 16px;
    }

    .reject-btn i {
        color: #dc3545;
        font-size: 16px;
    }

    .clarification-btn i {
        color: #ffc107;
        font-size: 16px;
    }

    .action-btn.selected.approve-btn {
        border-color: #28a745;
        background: #d4edda;
    }

    .action-btn.selected.reject-btn {
        border-color: #dc3545;
        background: #f8d7da;
    }

    .action-btn.selected.clarification-btn {
        border-color: #ffc107;
        background: #fff3cd;
    }

    /* Overall Feedback Section */
    .overall-feedback-section {
        background: #f8f9fa;
        padding: 25px;
        border-radius: 12px;
    }

    .feedback-container {
        background: white;
        padding: 20px;
        border-radius: 8px;
        border: 1px solid #dee2e6;
    }

    .feedback-requirement-text {
        font-size: 14px;
        font-weight: normal;
        transition: all 0.3s ease;
    }

    .feedback-requirement-text.required {
        color: #dc3545;
        font-weight: bold;
    }

    .feedback-validation-message {
        margin-top: 8px;
    }

    #overallFeedback {
        border: 2px solid #dee2e6;
        border-radius: 8px;
        padding: 12px;
        font-size: 14px;
        width: 100%;
        resize: vertical;
        transition: border-color 0.3s ease;
    }



    /* Form Actions */
    .form-actions {
        background: #f8f9fa;
        padding: 20px;
        margin: 30px -20px -20px -20px;
        border-radius: 0 0 8px 8px;
        border-top: 1px solid #dee2e6;
    }

</style>
<script>
    // Replace the entire JavaScript section with this corrected version:

    document.addEventListener('DOMContentLoaded', function() {

        // Section descriptions for each tab
        const sectionDescriptions = {
            'inventory': '<liferay-ui:message key="inventory.rating.description" />',
            'prioritization': '<liferay-ui:message key="prioritization.rating.description" />',
            'classification': '<liferay-ui:message key="classification.rating.description" />',
            'release-plan': '<liferay-ui:message key="release.plan.rating.description" />'
        };

        // Store rating data
        window.reviewData = window.reviewData || {};
        let currentSection = 'inventory';

        // Function to save current section rating
        function saveCurrentSectionData() {
            console.log('Saving data for section:', currentSection);

            const contentArea = document.getElementById('tabContentArea');
            const ratingBtn = contentArea.querySelector('.rating-btn.active');

            window.reviewData[currentSection] = {
                rating: ratingBtn ? ratingBtn.getAttribute('data-rating') : ''
            };

            console.log('Saved data for', currentSection, ':', window.reviewData[currentSection]);
            console.log('All review data:', window.reviewData);
        }

        // Function to load section rating and restore visual state
        function loadSectionData(sectionName) {
            console.log('Loading data for section:', sectionName);
            console.log('Available data:', window.reviewData[sectionName]);

            if (!window.reviewData[sectionName] || !window.reviewData[sectionName].rating) {
                console.log('No data to restore for', sectionName);
                return;
            }

            const data = window.reviewData[sectionName];

            // Use multiple attempts with increasing delays to ensure DOM is ready
            let attempts = 0;
            const maxAttempts = 5;

            function attemptRestore() {
                attempts++;
                console.log(`Attempt ${attempts} to restore rating for ${sectionName}`);

                const contentArea = document.getElementById('tabContentArea');
                const allRatingBtns = contentArea.querySelectorAll('.rating-btn');

                console.log('Found rating buttons:', allRatingBtns.length);

                if (allRatingBtns.length === 0 && attempts < maxAttempts) {
                    console.log('No rating buttons found yet, retrying in 100ms...');
                    setTimeout(attemptRestore, 100 * attempts); // Increasing delay
                    return;
                }

                if (data.rating) {
                    console.log('Restoring rating', data.rating, 'for section', sectionName);

                    // Remove active from all rating buttons
                    allRatingBtns.forEach(btn => btn.classList.remove('active'));

                    // Find and activate the correct rating button
                    const ratingBtn = contentArea.querySelector(`.rating-btn[data-rating="${data.rating}"]`);
                    console.log('Found rating button:', ratingBtn);
                    console.log('All available rating buttons:');
                    allRatingBtns.forEach((btn, index) => {
                        console.log(`Button ${index}: data-rating="${btn.getAttribute('data-rating')}"`);
                    });

                    if (ratingBtn) {
                        ratingBtn.classList.add('active');
                        console.log('✅ Successfully restored visual state for rating:', data.rating);

                        // Update hidden input if it exists
                        const hiddenInput = document.getElementById(sectionName + 'Rating');
                        if (hiddenInput) {
                            hiddenInput.value = data.rating;
                            console.log('Updated hidden input value:', hiddenInput.value);
                        }
                    } else {
                        console.error('❌ Rating button STILL not found for rating:', data.rating);
                        console.log('Available buttons data-rating values:');
                        allRatingBtns.forEach(btn => {
                            console.log('- Button with data-rating:', btn.getAttribute('data-rating'));
                        });
                    }
                }
            }

            // Start the first attempt immediately
            setTimeout(attemptRestore, 50);
        }

        // Function to load section content (only compliance rating)
        function loadSectionContent(sectionName) {
            console.log('Loading content for section:', sectionName);

            const template = document.getElementById('sectionTemplate');
            const contentArea = document.getElementById('tabContentArea');

            const clonedContent = template.querySelector('.section-content-template').cloneNode(true);

            const htmlContent = clonedContent.outerHTML
                .replace(/SECTION_NAME/g, sectionName)
                .replace('<!-- Description will be updated dynamically -->', sectionDescriptions[sectionName] || '');

            contentArea.innerHTML = htmlContent;

            console.log('✅ Content injected for section:', sectionName);

            // Attach event listeners to rating buttons FIRST
            contentArea.querySelectorAll('.rating-btn').forEach(button => {
                button.addEventListener('click', function() {
                    const section = this.closest('.rating-buttons').getAttribute('data-section');
                    const rating = this.getAttribute('data-rating');

                    console.log('Rating button clicked:', rating, 'for section:', section);

                    // Remove active from all rating buttons
                    contentArea.querySelectorAll('.rating-btn').forEach(btn => {
                        btn.classList.remove('active');
                    });

                    // Add active to clicked button
                    this.classList.add('active');

                    // Update hidden input if it exists
                    const hiddenInput = document.getElementById(section + 'Rating');
                    if (hiddenInput) {
                        hiddenInput.value = rating;
                        console.log('Updated hidden input:', hiddenInput.value);
                    }

                    // Save data immediately
                    saveCurrentSectionData();
                });
            });

            console.log('✅ Event listeners attached for section:', sectionName);

            // THEN load previously saved data for this section
            // Use a small delay to ensure everything is properly rendered
            setTimeout(() => {
                loadSectionData(sectionName);
            }, 100);
        }

        // Tab Navigation
        document.querySelectorAll('.nav-pill').forEach(pill => {
            pill.addEventListener('click', function() {
                const target = this.getAttribute('data-target');

                console.log('Tab clicked:', target);
                console.log('Current section before switch:', currentSection);

                // Save current section data before switching
                saveCurrentSectionData();

                // Update current section
                currentSection = target;

                // Update active pill
                document.querySelectorAll('.nav-pill').forEach(p => p.classList.remove('active'));
                this.classList.add('active');

                // Load content for selected section
                loadSectionContent(target);
            });
        });

        // Request state buttons (static - no cloning)
        document.querySelectorAll('.action-btn').forEach(button => {
            button.addEventListener('click', function() {
                const action = this.getAttribute('data-action');

                document.querySelectorAll('.action-btn').forEach(btn => {
                    btn.classList.remove('selected');
                });

                this.classList.add('selected');
                document.getElementById('overallRequestState').value = action;
            });
        });

        // Form submission with proper hidden inputs creation
        document.getElementById('submitReviewBtn').addEventListener('click', function(e) {
            e.preventDefault();

            // Save current section data first
            saveCurrentSectionData();

            console.log('All review data before submission:', window.reviewData);

            // Create hidden inputs for ALL sections
            const sections = ['inventory', 'prioritization', 'classification', 'release-plan'];

            sections.forEach(section => {
                const rating = window.reviewData[section] ? window.reviewData[section].rating : '';

                console.log(`Processing ${section}: rating = ${rating}`);

                // Remove any existing hidden input for this section
                const existingInputs = document.querySelectorAll(`input[name*="${section}Rating"]`);
                existingInputs.forEach(input => input.remove());

                // Create new hidden input with correct name format
                const hiddenInput = document.createElement('input');
                hiddenInput.type = 'hidden';
                hiddenInput.name = '<portlet:namespace />' + section + 'Rating';
                hiddenInput.value = rating || '';

                console.log(`Created hidden input: name="${hiddenInput.name}" value="${hiddenInput.value}"`);

                // Append to form
                document.getElementById('reviewForm').appendChild(hiddenInput);
            });

            // Debug: Log all form data
            const formData = new FormData(document.getElementById('reviewForm'));
            console.log('Final form data:');
            for (let [key, value] of formData.entries()) {
                console.log(`${key}: "${value}"`);
            }

            // Submit the form
            //document.getElementById('reviewForm').submit();
        });

        // Cancel and back buttons
        document.getElementById('cancelReviewBtn').addEventListener('click', function() {
            window.history.back();
        });

        document.getElementById('backBtn').addEventListener('click', function() {
            window.history.back();
        });

        // Initialize with first tab
        loadSectionContent('inventory');

        // Debug: Show review data every few seconds
        setInterval(() => {
            if (Object.keys(window.reviewData).length > 0) {
                console.log('Current review data:', window.reviewData);
            }
        }, 5000);
    });
</script>