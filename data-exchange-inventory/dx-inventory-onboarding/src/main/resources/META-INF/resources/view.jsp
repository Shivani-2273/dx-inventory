<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet_2_0" %>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="com.liferay.portal.kernel.util.ResourceBundleUtil" %>
<%@ page import="com.liferay.portal.kernel.util.PortalUtil" %>
<%@ page import="com.liferay.object.model.ObjectEntry" %>
<%@ page import="com.liferay.object.service.ObjectEntryLocalServiceUtil" %>
<%@ page import="com.liferay.portal.kernel.workflow.WorkflowConstants" %>
<%@ page import="com.liferay.portal.kernel.exception.PortalException" %>
<%@ page import="com.liferay.portal.kernel.service.RoleLocalServiceUtil" %>
<%@ page import="com.liferay.portal.kernel.model.Role" %>
<%@ page import="java.util.List" %>

<%@ include file="/init.jsp" %>

<portlet:actionURL name="/addDataset" var="addDatasetURL" />
<portlet:resourceURL id="uploadTemplate" var="uploadTemplateURL" />
<portlet:resourceURL id="validateFile" var="validateFileURL" />
<portlet:resourceURL id="processFile" var="processFileURL" />
<portlet:resourceURL id="fetchData" var="fetchDataURL" />
<portlet:actionURL name="/updateDataset" var="updateDatasetURL" />


<%
	String sampleDocumentUrl = (String) request.getAttribute("sampleDocumentUrl");
	ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", themeDisplay.getLocale(), getClass());

	HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
	String inventoryId = originalRequest.getParameter("inventoryId");
	long userId = themeDisplay.getUserId();
	List<Role> roles = RoleLocalServiceUtil.getUserRoles(userId);
	boolean isDataSteward = roles.stream().anyMatch(role -> "Data Steward".equals(role.getName()));

	ObjectEntry inventoryEntry = null;
	if (inventoryId != null && !inventoryId.trim().isEmpty()) {
		try {
			inventoryEntry = ObjectEntryLocalServiceUtil.getObjectEntry(Long.parseLong(inventoryId));
		} catch (PortalException e) {
			throw new RuntimeException(e);
		}
	}

    String userMode = "";
	if(inventoryEntry != null){
		if(!"pending".equalsIgnoreCase(WorkflowConstants.getStatusLabel(inventoryEntry.getStatus()))){
			if(inventoryEntry.getUserId() == userId){
				userMode ="update";
			}
			else {
				userMode = "review";
            }
		}else{
			userMode = "review";
		}
	}

	boolean isUpdateMode = "update".equals(userMode) && inventoryId != null;
	String submitButtonKey = isUpdateMode ? "update" : "submit";

%>

<div class="inventory-onboarding-container">
	<!-- Header -->
	<div class="row mb-4">
		<div class="col-md-8">
			<h2 class="text-primary"><liferay-ui:message key="inventory.onboarding" /></h2>
		</div>
		<div class="col-md-4 text-right">
			<button type="button" class="btn btn-outline-primary" id="excelTemplateBtn">
				<i class="fa fa-plus"></i> <liferay-ui:message key="add.dataset.excel.template" />
			</button>
		</div>
	</div>
	<!-- Main Form -->
	<form id="datasetForm" action="<%= isUpdateMode ? updateDatasetURL : addDatasetURL %>" method="post">
		<input type="hidden" id="actionType" name="<portlet:namespace />actionType" value="submit" />
		<input type="hidden" id="userMode" value="<%= userMode %>">
		<% if (isUpdateMode) { %>
		<input type="hidden" name="<portlet:namespace />inventoryId" value="<%= inventoryId %>" />
		<% } %>
		<div class="row">
			<div class="col-md-3">
				<div class="card">
					<div class="card-body">
						<div class="d-flex justify-content-between align-items-center mb-3">
							<input type="hidden" id="totalDatasets" name="<portlet:namespace />totalDatasets" value="">

							<span class="text-primary cursor-pointer" id="addDatasetManuallyBtn">
                                <i class="fa fa-plus-circle"></i> <liferay-ui:message key="add.dataset.manually" />
                            </span>
						</div>
						<div class="dataset-list">

						</div>
					</div>
				</div>
			</div>

			<div class="col-md-9">
				<div class="card">
					<div class="card-body" id="formsContainer">

					</div>
				</div>
			</div>
		</div>
	</form>
</div>

<!-- Dataset Form Template (SINGLE SOURCE OF TRUTH) -->
<div id="datasetFormTemplate" style="display: none;">
	<script class='template-dataset-form' type='text/htmltemplate'>
		<div class="dataset-form" id="datasetForm_{{datasetId}}" data-dataset="{{datasetId}}" style="{{displayStyle}}">
			<div class="dataset-details-section">
				<h5 class="mb-4"><liferay-ui:message key="data.inventory.details.dataset" /> {{datasetId}}</h5>

				<div class="mb-4">
					<h6><liferay-ui:message key="dataset.details" /></h6>
					<div class="row">
						<div class="col-md-4">
							<div class="form-group">
								<label for="datasetName_{{datasetId}}"><liferay-ui:message key="dataset.name" /> <span class="text-danger">*</span></label>
								<input type="text" class="form-control" id="datasetName_{{datasetId}}" name="<portlet:namespace />datasetName_{{datasetId}}" required>
							</div>
						</div>
						<div class="col-md-4">
							<div class="form-group">
								<label for="datasetDescription_{{datasetId}}"><liferay-ui:message key="dataset.description" />  <span class="text-danger">*</span></label>
								<input type="text" class="form-control" id="datasetDescription_{{datasetId}}" name="<portlet:namespace />datasetDescription_{{datasetId}}" required>
							</div>
						</div>
						<div class="col-md-4">
							<div class="form-group">
								<label for="datasetClassification_{{datasetId}}"><liferay-ui:message key="dataset.classification" /> <span class="text-danger">*</span></label>
								<select class="form-control" id="datasetClassification_{{datasetId}}" name="<portlet:namespace />datasetClassification_{{datasetId}}" required>
									<option value=""><liferay-ui:message key="choose.option" /></option>
									<option value="Open"><liferay-ui:message key="classification.open" /></option>
									<option value="Sensitive"><liferay-ui:message key="classification.sensitive" /></option>
									<option value="Secret"><liferay-ui:message key="classification.secret" /></option>
									<option value="Confidential"><liferay-ui:message key="classification.confidential" /></option>
								</select>
							</div>
						</div>
					</div>
				</div>

				<!-- Attributes Section -->
				<div class="mb-4">
					<h6><liferay-ui:message key="attributes.details" /></h6>
					<div id="attributesContainer_{{datasetId}}">
						<div class="single-attribute-field mb-3" data-index="1" data-dataset="{{datasetId}}">
							<div class="row">
								<div class="col-md-5">
									<div class="form-group">
										<label><liferay-ui:message key="attribute" /> 1 <span class="text-danger">*</span></label>
										<input type="text" class="form-control" name="<portlet:namespace />attributeName_{{datasetId}}_1" placeholder="<liferay-ui:message key="attribute.name.placeholder" />" required>
									</div>
								</div>
								<div class="col-md-5">
									<div class="form-group">
										<label><liferay-ui:message key="attribute" />  1 <liferay-ui:message key="attribute.description" /> <span class="text-danger">*</span></label>
										<input type="text" class="form-control" name="<portlet:namespace />attributeDescription_{{datasetId}}_1" placeholder="<liferay-ui:message key="attribute.description.placeholder" />" required>
									</div>
								</div>
								<div class="col-md-2 d-flex align-items-end">
									<div class="form-group w-100">
										<button type="button" class="btn btn-outline-danger remove-attribute w-100" style="display:none;">
											<i class="fa fa-trash"></i>
										</button>
									</div>
								</div>
							</div>
						</div>
					</div>
					<div class="mb-3">
						<button type="button" class="btn btn-link text-primary p-0 addAttributeBtn" data-dataset="{{datasetId}}">
							<i class="fa fa-plus-circle"></i> <liferay-ui:message key="add.new.attribute" />
						</button>
					</div>
				</div>

				<!-- Data Prioritization Details - Benefits -->
				<div class="mb-4">
					<h6><liferay-ui:message key="data.prioritization.benefits" /></h6>
					<div class="row">
						<div class="col-md-3">
							<div class="form-group">
								<label for="userDemand_{{datasetId}}"><liferay-ui:message key="user.demand" /> <span class="text-danger">*</span></label>
								<select class="form-control" id="userDemand_{{datasetId}}" name="<portlet:namespace />userDemand_{{datasetId}}" required>
									<option value=""><liferay-ui:message key="choose.option" /></option>
									<option value="1">1</option>
									<option value="2">2</option>
									<option value="3">3</option>
									<option value="4">4</option>
									<option value="5">5</option>
								</select>
							</div>
						</div>
						<div class="col-md-3">
							<div class="form-group">
								<label for="economicImpact_{{datasetId}}"><liferay-ui:message key="economic.impact" /> <span class="text-danger">*</span></label>
								<select class="form-control" id="economicImpact_{{datasetId}}" name="<portlet:namespace />economicImpact_{{datasetId}}" required>
									<option value=""><liferay-ui:message key="choose.option" /></option>
									<option value="1">1</option>
									<option value="2">2</option>
									<option value="3">3</option>
									<option value="4">4</option>
									<option value="5">5</option>
								</select>
							</div>
						</div>
						<div class="col-md-3">
							<div class="form-group">
								<label for="betterServices_{{datasetId}}"><liferay-ui:message key="better.services" /> <span class="text-danger">*</span></label>
								<select class="form-control" id="betterServices_{{datasetId}}" name="<portlet:namespace />betterServices_{{datasetId}}" required>
									<option value=""><liferay-ui:message key="choose.option" /></option>
									<option value="1">1</option>
									<option value="2">2</option>
									<option value="3">3</option>
									<option value="4">4</option>
									<option value="5">5</option>
								</select>
							</div>
						</div>
						<div class="col-md-3">
							<div class="form-group">
								<label for="betterGovernance_{{datasetId}}"><liferay-ui:message key="better.governance" />  <span class="text-danger">*</span></label>
								<select class="form-control" id="betterGovernance_{{datasetId}}" name="<portlet:namespace />betterGovernance_{{datasetId}}" required>
									<option value=""><liferay-ui:message key="choose.option" /></option>
									<option value="1">1</option>
									<option value="2">2</option>
									<option value="3">3</option>
									<option value="4">4</option>
									<option value="5">5</option>
								</select>
							</div>
						</div>
					</div>
				</div>

				<!-- Data Prioritization Details - Quality & Readiness -->
				<div class="mb-4">
					<h6><liferay-ui:message key="data.prioritization.quality.readiness" /></h6>
					<div class="row">
						<div class="col-md-3">
							<div class="form-group">
								<label for="definedOwner_{{datasetId}}"><liferay-ui:message key="defined.owner" /> <span class="text-danger">*</span></label>
								<select class="form-control" id="definedOwner_{{datasetId}}" name="<portlet:namespace />definedOwner_{{datasetId}}" required>
									<option value=""><liferay-ui:message key="choose.option" /></option>
									<option value="Yes"><liferay-ui:message key="yes" /></option>
									<option value="No"><liferay-ui:message key="no" /></option>
								</select>
							</div>
						</div>
						<div class="col-md-3">
							<div class="form-group">
								<label for="existingMetadata_{{datasetId}}">Existing Metadata <span class="text-danger">*</span></label>
								<select class="form-control" id="existingMetadata_{{datasetId}}" name="<portlet:namespace />existingMetadata_{{datasetId}}" required>
									<option value=""><liferay-ui:message key="choose.option" /></option>
									<option value="Yes"><liferay-ui:message key="yes" /></option>
									<option value="No"><liferay-ui:message key="no" /></option>
								</select>
							</div>
						</div>
						<div class="col-md-3">
							<div class="form-group">
								<label for="alreadyPublished_{{datasetId}}">Already Published <span class="text-danger">*</span></label>
								<select class="form-control" id="alreadyPublished_{{datasetId}}" name="<portlet:namespace />alreadyPublished_{{datasetId}}" required>
									<option value=""><liferay-ui:message key="choose.option" /></option>
									<option value="Yes"><liferay-ui:message key="yes" /></option>
									<option value="No"><liferay-ui:message key="no" /></option>
								</select>
							</div>
						</div>
						<div class="col-md-3">
							<div class="form-group">
								<label for="openFormat_{{datasetId}}">Open Format <span class="text-danger">*</span></label>
								<select class="form-control" id="openFormat_{{datasetId}}" name="<portlet:namespace />openFormat_{{datasetId}}" required>
									<option value=""><liferay-ui:message key="choose.option" /></option>
									<option value="Yes"><liferay-ui:message key="yes" /></option>
									<option value="No"><liferay-ui:message key="no" /></option>
								</select>
							</div>
						</div>
					</div>
				</div>

				<!-- Dataset Release Plan -->
				<div class="mb-4">
					<h6><liferay-ui:message key="dataset.release.plan" /></h6>
					<div class="row">
						<div class="col-md-4">
							<div class="form-group">
								<label for="releaseYear_{{datasetId}}"><liferay-ui:message key="release.year" /> <span class="text-danger">*</span></label>
								<select class="form-control" id="releaseYear_{{datasetId}}" name="<portlet:namespace />releaseYear_{{datasetId}}" required>
									<option value=""><liferay-ui:message key="choose.option" /></option>
									<option value="2021">2021</option>
									<option value="2022">2022</option>
									<option value="2023">2023</option>
									<option value="2024">2024</option>
									<option value="2025">2025</option>
									<option value="2026">2026</option>
								</select>
							</div>
						</div>
						<div class="col-md-4">
							<div class="form-group">
								<label for="releaseMonth_{{datasetId}}"><liferay-ui:message key="release.month" /> <span class="text-danger">*</span></label>
								<select class="form-control" id="releaseMonth_{{datasetId}}" name="<portlet:namespace />releaseMonth_{{datasetId}}" required>
									<option value=""><liferay-ui:message key="choose.option" /></option>
									<option value="Jan"><liferay-ui:message key="month.jan" /></option>
									<option value="Feb"><liferay-ui:message key="month.feb" /></option>
									<option value="Mar"><liferay-ui:message key="month.mar" /></option>
									<option value="Apr"><liferay-ui:message key="month.apr" /></option>
									<option value="May"><liferay-ui:message key="month.may" /></option>
									<option value="Jun"><liferay-ui:message key="month.jun" /></option>
									<option value="Jul"><liferay-ui:message key="month.jul" /></option>
									<option value="Aug"><liferay-ui:message key="month.aug" /></option>
									<option value="Sep"><liferay-ui:message key="month.sep" /></option>
									<option value="Oct"><liferay-ui:message key="month.oct" /></option>
									<option value="Nov"><liferay-ui:message key="month.nov" /></option>
									<option value="Dec"><liferay-ui:message key="month.dec" /></option>
								</select>
							</div>
						</div>
					</div>
				</div>

				<!-- Buttons -->
				<div class="d-flex justify-content-between align-items-center mt-4">
					<button type="button" class="btn btn-outline-secondary align-items-center saveAsDraftBtn" data-dataset="{{datasetId}}">
						<i class="fa fa-file-text-o mr-2"></i> <liferay-ui:message key="save.as.draft" />
					</button>
					<div class="d-flex gap-3">
						<button type="button" class="btn btn-outline-secondary px-4 cancelBtn" data-dataset="{{datasetId}}"><liferay-ui:message key="cancel" /></button>
<%--						<button type="submit" class="btn btn-primary px-5 submitBtn" data-dataset="{{datasetId}}"><liferay-ui:message key="submit" /></button>--%>

						<button type="submit" class="btn btn-primary px-5 submitBtn" data-dataset="{{datasetId}}"><liferay-ui:message key="<%= submitButtonKey %>" /></button>
					</div>
				</div>
			</div>
		</div>
	</script>
</div>

<!-- Enhanced Excel Template Modal -->
<div class="modal fade" id="excelTemplateModal" tabindex="-1" role="dialog">
	<div class="modal-dialog modal-lg" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<h5 class="modal-title"><liferay-ui:message key="modal.use.template" /></h5>
				<button type="button" class="close" data-dismiss="modal">
					<span>&times;</span>
				</button>
			</div>
			<div class="modal-body">
				<p class="text-muted mb-4"><liferay-ui:message key="modal.description" /></p>

				<div class="row">
					<div class="col-md-6">
						<div class="template-step">
							<div class="step-number">1</div>
							<h6><liferay-ui:message key="modal.download.template" /></h6>
							<p class="text-muted"><liferay-ui:message key="modal.step1.description" /></p>

							<div class="template-download text-center p-3 border rounded">
								<div class="file-icon mb-2">
									<i class="fas fa-file-excel fa-3x text-success"></i>
								</div>
								<div class="file-info">
									<div class="file-name font-weight-bold">Inventory_Onboarding_Template.xlsx</div>
								</div>
								<% if (sampleDocumentUrl != null && !sampleDocumentUrl.trim().isEmpty()) { %>
								<a href="<%= sampleDocumentUrl %>"
								   class="btn btn-outline-primary btn-sm mt-2"
								   id="downloadTemplateBtn"
								   download>
									<i class="fa fa-download"></i> <liferay-ui:message key="download" />
								</a>
								<% } else { %>
								<button type="button" class="btn btn-outline-secondary btn-sm mt-2" disabled>
									<i class="fa fa-download"></i> <liferay-ui:message key="download.not.configured" />
								</button>
								<small class="text-danger d-block mt-1"><liferay-ui:message key="system.settings.configure" /></small>
								<% } %>
							</div>
						</div>
					</div>

					<div class="col-md-6">
						<div class="template-step">
							<div class="step-number">2</div>
							<h6><liferay-ui:message key="modal.upload.template" /></h6>
							<p class="text-muted"><liferay-ui:message key="modal.step2.description" /></p>
							<!-- File Upload Section -->
							<div class="mb-3">
								<label class="small mb-2"><liferay-ui:message key="upload.file" /> <span class="text-danger">*</span></label>
								<div class="upload-area text-center p-4 border border-dashed rounded" id="uploadArea">
									<div class="upload-placeholder">
										<i class="fa fa-cloud-upload fa-3x text-muted mb-3"></i>
										<p class="mb-2"><strong><liferay-ui:message key="drop.file.here" /> </strong></p>
										<p class="mb-3 text-muted"><liferay-ui:message key="or" /> </p>
										<p class="mt-2 mb-0">
											<small class="text-muted"><liferay-ui:message key="file.support.info" /> </small>
										</p>
										<input type="file" id="templateFileInput" accept=".csv,.xlsx,.xls" >
									</div>
									<div id="uploadedFileInfo" style="display: none;">
										<div class="file-icon mb-2">
											<i class="fas fa-file-csv fa-3x text-info"></i>
										</div>
										<div class="file-info">
											<div class="file-name font-weight-bold" id="uploadedFileName"></div>
											<div class="file-size text-muted small" id="uploadedFileSize"></div>
										</div>
										<div class="mt-2">
											<button type="button" class="btn btn-outline-success btn-sm mr-2" id="changeFileBtn">
												<i class="fa fa-edit"></i> <liferay-ui:message key="change.file" />
											</button>
											<button type="button" class="btn btn-outline-danger btn-sm" id="removeUploadedFile">
												<i class="fa fa-trash"></i> <liferay-ui:message key="remove.file" />
											</button>
										</div>
									</div>
								</div>
							</div>

							<!-- Validation Results -->
							<div id="validationResults" class="mt-3" style="display: none;">
								<div class="alert alert-info">
									<h6><i class="fa fa-info-circle"></i><liferay-ui:message key="file.validation.results" /> </h6>
									<div id="validationDetails"></div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
			<div class="modal-footer">
				<div class="d-flex justify-content-between w-100">
					<button type="button" class="btn btn-outline-secondary" id="validateFileBtn" disabled>
						<i class="fa fa-check-circle"></i> <liferay-ui:message key="validate.file" />
					</button>
					<div>
						<button type="button" class="btn btn-secondary" data-dismiss="modal"><liferay-ui:message key="cancel" /> </button>
						<button type="button" class="btn btn-primary" id="processTemplateBtn" disabled><liferay-ui:message key="proceed" /> </button>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>


<%if ("review".equals(userMode) && isDataSteward) { %>
<%@ include file="compliance-score.jsp" %>
<% } %>

<script src="https://cdnjs.cloudflare.com/ajax/libs/underscore.js/1.13.6/underscore-min.js"></script>
<script src="<%=request.getContextPath()%>/js/index.js"></script>
<script src="<%=request.getContextPath()%>/js/file-upload.js"></script>
<script src="<%=request.getContextPath()%>/js/review-update.js"></script>


<script>
	window.portletNamespace = '<portlet:namespace />';
	window.portletURLs = {
		uploadTemplate: '${uploadTemplateURL}',
		validateFile: '${validateFileURL}',
		processFile: '${processFileURL}',
		fetchData: '${fetchDataURL}'
	};

	window.sampleDocumentUrl = '<%= sampleDocumentUrl != null ? sampleDocumentUrl : "" %>';

	// Localized strings for JavaScript
	window.localizedStrings = {
		'attribute': '<%= LanguageUtil.get(resourceBundle, "attribute") %>',
		'attributeDescription': '<%= LanguageUtil.get(resourceBundle, "attribute.description") %>',
		'attributeNamePlaceholder': '<%= LanguageUtil.get(resourceBundle, "attribute.name.placeholder") %>',
		'attributeDescriptionPlaceholder': '<%= LanguageUtil.get(resourceBundle, "attribute.description.placeholder") %>',
		'dataset': '<%= LanguageUtil.get(resourceBundle, "dataset") %>',
	};

	// Validation messages for JavaScript
	window.validationMessages = {
		'invalidFileType': '<%= LanguageUtil.get(resourceBundle, "invalid.file.type") %>',
		'fileSizeLimit': '<%= LanguageUtil.get(resourceBundle, "file.size.limit") %>',
		'selectFile': '<%= LanguageUtil.get(resourceBundle, "select.file") %>',
		'validatingFiles': '<%= LanguageUtil.get(resourceBundle, "validating.files") %>',
		'errorValidatingFile': '<%= LanguageUtil.get(resourceBundle, "error.validating.file") %>',
		'requestTimeOut': '<%= LanguageUtil.get(resourceBundle, "request.time.out") %>',
		'fileValidationSuccess': '<%= LanguageUtil.get(resourceBundle, "file.validation.success") %>',
		'errorParsingValidation': '<%= LanguageUtil.get(resourceBundle, "error.parsing.validation") %>',
		'validationPassed': '<%= LanguageUtil.get(resourceBundle, "validation.passed") %>',
		'columns': '<%= LanguageUtil.get(resourceBundle, "columns") %>',
		'dataRows': '<%= LanguageUtil.get(resourceBundle, "data.rows") %>',
		'dataValidation': '<%= LanguageUtil.get(resourceBundle, "data.validation") %>',
		'encoding': '<%= LanguageUtil.get(resourceBundle, "encoding") %>',
		'fileStructureIssue': '<%= LanguageUtil.get(resourceBundle, "file.structure.issue") %>',
		'dataValidationIssue': '<%= LanguageUtil.get(resourceBundle, "data.validation.issue") %>',
		'validationFailed': '<%= LanguageUtil.get(resourceBundle, "validation.failed") %>',
		'fixIssue': '<%= LanguageUtil.get(resourceBundle, "fix.issue") %>',
		'moreErrors': '<%= LanguageUtil.get(resourceBundle, "more.errors") %>',
		'validateFileFirst': '<%= LanguageUtil.get(resourceBundle, "validate.file.first") %>',
		'processingFile': '<%= LanguageUtil.get(resourceBundle, "processing.file") %>',
		'processingTimeout': '<%= LanguageUtil.get(resourceBundle, "processing.timeout") %>',
		'errorParsingFile': '<%= LanguageUtil.get(resourceBundle, "error.parsing.file") %>',
		'successfullyLoaded': '<%= LanguageUtil.get(resourceBundle, "successfully.loaded") %>',
		'datasetFromFile': '<%= LanguageUtil.get(resourceBundle, "dataset.from.file") %>',
		'fileEmpty': '<%= LanguageUtil.get(resourceBundle, "file.empty") %>',
		'errorProcessingFile': '<%= LanguageUtil.get(resourceBundle, "error.processing.file") %>',
		'errorParsingFileData': '<%= LanguageUtil.get(resourceBundle, "error.parsing.file.data") %>',
		'datasetLoaded': '<%= LanguageUtil.get(resourceBundle, "dataset.loaded") %>'
	};
</script>
<script>
	window.showToast = function(message, type, title) {
		const liferayType = {
			'success': 'success',
			'error': 'danger',
			'info': 'info',
			'warning': 'warning'
		}[type] || 'info';

		if (Liferay?.Util?.openToast) {
			Liferay.Util.openToast({
				message: message,
				title: title || 'Notification',
				type: liferayType,
				autoClose: 5000
			});
		} else {
			alert(`${title || 'Notification'}: ${message}`);
		}
	};
</script>