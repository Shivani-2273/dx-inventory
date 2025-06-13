<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.liferay.portal.kernel.util.PortalUtil" %>
<%@ page import="com.liferay.portal.kernel.model.User" %>
<%@ include file="/init.jsp" %>

<portlet:renderURL var="initiateFormURL">
    <portlet:param name="mvcPath" value="/view.jsp" />
</portlet:renderURL>

<%
    // Get current user
    User currentUser = PortalUtil.getUser(request);
    long currentUserId = (currentUser != null) ? currentUser.getUserId() : 0;

    // Get inventory data from request attribute
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> allInventoryList = (List<Map<String, Object>>) request.getAttribute("inventoryList");

    // Filter inventories based on user permissions
    List<Map<String, Object>> inventoryList = new ArrayList<>();

    if (allInventoryList != null) {
        for (Map<String, Object> inventory : allInventoryList) {
            String status = (String) inventory.get("status");
            Long lastUpdatedUserId = (Long) inventory.get("userId");

            if ("draft".equalsIgnoreCase(status)) {
                // Only show draft entries if current user was the last to update them
                if (lastUpdatedUserId != null && lastUpdatedUserId.equals(currentUserId)) {
                    inventoryList.add(inventory);
                }
            } else {
                // Show all non-draft entries
                inventoryList.add(inventory);
            }
        }
    }

    boolean hasInventories = inventoryList != null && !inventoryList.isEmpty();
    int inventoryCount = inventoryList != null ? inventoryList.size() : 0;
%>

<div class="inventory-container">
    <div class="container-fluid">

        <% if (!hasInventories) { %>
        <!-- Empty State View (when no inventories exist) -->
        <div id="emptyStateView" class="main-content">
            <div class="page-header">
                <h1 class="page-title">Inventory Onboarding</h1>
                <div class="action-header">
                    <a href="${initiateFormURL}" class="btn btn-primary-custom" id="initiateRequestBtn">
                        <i class="fa fa-plus"></i> Initiate An Inventory Onboarding Request
                    </a>
                </div>
            </div>

            <p class="page-description">
                This service enables the customers to request for issuing/ renewing permits for new & existing firms to practice poll studies activity. All firms that want to practice the poll studies activity in the Emirate of Dubai are eligible to apply for this service after fulfilling all the requirements of the Licensing Authority.
            </p>

            <div class="empty-state">
                <div class="empty-state-icon">
                    <i class="far fa-file-alt"></i>
                </div>
                <h3 class="empty-state-title">No Existing Inventory Found</h3>
                <p class="empty-state-text">
                    Initiate a request to download the template
                </p>
            </div>
        </div>

        <% } else { %>
        <!-- Table View -->
        <div id="tableView" class="main-content">
            <div class="page-header">
                <h1 class="page-title">Inventory Onboarding</h1>
                <div class="action-header">
                    <a href="${initiateFormURL}" class="btn btn-primary-custom">
                        <i class="fa fa-plus"></i> Submit a Request
                    </a>
                </div>
            </div>

            <p class="page-description">
                This service enables the customers to request for issuing/ renewing permits for new & existing firms to practice poll studies activity. All firms that want to practice the poll studies activity in the Emirate of Dubai are eligible to apply for this service after fulfilling all the requirements of the Licensing Authority.
            </p>

            <div class="table-section">
                <div class="section-header">
                    <div class="d-flex align-items-center">
                        <h2 class="section-title">Submitted Inventories</h2>
                        <span class="inventory-count"><%= inventoryCount %> Inventories</span>
                    </div>
                    <div class="search-container">
                        <input type="text" class="search-input" placeholder="Search" id="searchInput">
                        <i class="fa fa-search search-icon"></i>
                    </div>
                </div>

                <div class="inventory-table">
                    <div class="table">
                        <table class="table table-hover mb-0">
                            <thead>
                            <tr>
                                <th>Name</th>
                                <th>No. of Datasets</th>
                                <th>Status</th>
                                <th>Compliance Score</th>
                                <th>Pending Actions</th>
                                <th></th>
                            </tr>
                            </thead>
                            <tbody id="inventoryTableBody">
                            <% for (Map<String, Object> inventory : inventoryList) { %>
                            <tr>
                                <td>
                                    <div class="inventory-name"><%= inventory.get("name") %></div>
                                    <div class="inventory-date"><%= inventory.get("submittedDate") %></div>
                                </td>
                                <td>
                                    <%
                                        Integer datasetCount = (Integer) inventory.get("datasetCount");
                                        if (datasetCount != null && datasetCount > 0) {
                                    %>
                                    <span class="dataset-count badge-info"><%= datasetCount %> Datasets</span>
                                    <% } else { %>
                                    <span class="text-muted">-</span>
                                    <% } %>

                                </td>
                                <td>
                                    <span class="status-badge">
                                        <i class="fa fa-check-circle"></i> <%= inventory.get("status") %>
                                    </span>
                                </td>
                                <td>
                                    <!-- Display compliance score here when implemented -->
                                    <span class="text-muted">Not Scored Yet</span>
                                </td>
                                <td>
                                    <a href="#" class="action-link">Validate <i class="fa fa-external-link-alt"></i></a>
                                    <span class="text-muted">-</span>
                                </td>
                                <td>
                                    <div class="action-menu-container">
                                        <button class="action-menu-btn" type="button" onclick="toggleActionMenu(<%= inventory.get("id") %>)">
                                            <i class="fa fa-ellipsis-v"></i>
                                        </button>
                                        <div class="action-menu-dropdown" id="actionMenu-<%= inventory.get("id") %>">
                                            <%
                                                String status = (String) inventory.get("status");
                                                if ("draft".equalsIgnoreCase(status)) {
                                                    // For draft entries: show Update option
                                            %>
                                            <a href="#" class="action-menu-item" onclick="requestInventoryUpdate(<%= inventory.get("id") %>)">
                                                <i class="fa fa-edit"></i> Update Inventory
                                            </a>
                                            <% } else {
                                                // For non-draft entries: show Review option
                                            %>
                                            <a href="#" class="action-menu-item" onclick="reviewSubmission(<%= inventory.get("id") %>)">
                                                <i class="fa fa-eye"></i> Review Submission
                                            </a>
                                            <% } %>
                                        </div>
                                    </div>
                                </td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                        <div id="noResultsMessage" class="no-results" style="display: none;">
                            <i class="fa fa-search" style="font-size: 24px; margin-bottom: 10px;"></i>
                            <div>No inventories found matching your search.</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <% } %>

    </div>
</div>

<script>
    const initiateFormURL = '<%= initiateFormURL.toString() %>';
    $(document).ready(function() {

        // Close action menus when clicking outside
        $(document).on('click', function(e) {
            if (!$(e.target).closest('.action-menu-container').length) {
                $('.action-menu-dropdown').removeClass('show');
            }
        });

        // Prevent menu from closing when clicking inside it
        $('.action-menu-dropdown').on('click', function(e) {
            e.stopPropagation();
        });
    });


    // Toggle action menu dropdown
    function toggleActionMenu(inventoryId) {
        const menuId = 'actionMenu-' + inventoryId;
        const $menu = $('#' + menuId);

        // Close all other menus
        $('.action-menu-dropdown').not($menu).removeClass('show');

        // Toggle current menu
        $menu.toggleClass('show');
    }

    function reviewSubmission(inventoryId) {
        window.location.href = initiateFormURL + '&inventoryId=' + inventoryId;
    }

    function requestInventoryUpdate(inventoryId) {
        window.location.href = initiateFormURL + '&inventoryId=' + inventoryId;
    }

</script>