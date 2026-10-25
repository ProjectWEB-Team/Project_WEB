<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!-- Sidebar Navigation for Internal Operations (6 Roles) -->
<div class="card-ute p-3 mb-4">
    <div class="d-flex align-items-center gap-2 mb-3 pb-2 border-bottom">
        <i class="bi bi-person-badge fs-4 text-ute-primary"></i>
        <div>
            <h6 class="mb-0 fw-bold text-ute-dark">Menu Tác Nghiệp</h6>
            <small class="text-ute-muted">6 Phân quyền hệ thống</small>
        </div>
    </div>

    <div class="accordion accordion-flush" id="sidebarAccordion">
        <!-- Phân hệ 1: Bưu cục & Linehaul (Thành viên 1) -->
        <div class="accordion-item border-0 mb-2">
            <h2 class="accordion-header" id="headingOne">
                <button class="accordion-button collapsed py-2 px-2 rounded-2 bg-light text-ute-primary fw-semibold" type="button" data-bs-toggle="collapse" data-bs-target="#collapseOne">
                    <i class="bi bi-buildings me-2"></i>1. Vận hành Bưu cục & Hub
                </button>
            </h2>
            <div id="collapseOne" class="accordion-collapse collapse" data-bs-parent="#sidebarAccordion">
                <div class="accordion-body p-2">
                    <ul class="nav flex-column small">
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/hub/inbound"><i class="bi bi-qr-code-scan me-2 text-ute-primary"></i>Quét Inbound Hub</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/hub/sorter"><i class="bi bi-diagram-2 me-2 text-ute-primary"></i>Phân loại Khay/Bao</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/hub/outbound"><i class="bi bi-box-arrow-up-right me-2 text-ute-primary"></i>Đóng bao Outbound</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/driver"><i class="bi bi-truck me-2 text-ute-primary"></i>Chuyến xe Linehaul</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/hub/incidents"><i class="bi bi-shield-exclamation me-2 text-danger"></i>Biên bản Sự cố</a></li>
                    <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/hub/delivery-assignments">Phân công giao / hoàn hàng</a></li>
<li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/hub/return-collection">Bàn giao hàng hoàn tại hub</a></li>
</ul>
                </div>
            </div>
        </div>

        <!-- Phân hệ 2: Khách hàng & Shipper (Thành viên 2) -->
        <div class="accordion-item border-0 mb-2">
            <h2 class="accordion-header" id="headingTwo">
                <button class="accordion-button collapsed py-2 px-2 rounded-2 bg-light text-ute-accent fw-semibold" type="button" data-bs-toggle="collapse" data-bs-target="#collapseTwo">
                    <i class="bi bi-person-walking me-2"></i>2. Cổng Khách hàng & Shipper
                </button>
            </h2>
            <div id="collapseTwo" class="accordion-collapse collapse" data-bs-parent="#sidebarAccordion">
                <div class="accordion-body p-2">
                    <ul class="nav flex-column small">
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/user/shipments/create"><i class="bi bi-plus-circle me-2 text-ute-accent"></i>Tạo vận đơn gửi</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/user/shipments"><i class="bi bi-boxes me-2 text-ute-accent"></i>Quản lý đơn hàng</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/user/incoming"><i class="bi bi-inbox me-2 text-ute-accent"></i>Đơn đang đến</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/hub/pickups"><i class="bi bi-person-check me-2 text-ute-accent"></i>Phân công lấy hàng</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/user/wallet"><i class="bi bi-wallet2 me-2 text-ute-accent"></i>Ví điện tử</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/shipper/tasks"><i class="bi bi-bicycle me-2 text-ute-accent"></i>Ca Shipper Lấy/Giao</a></li>
                    </ul>
                </div>
            </div>
        </div>

        <!-- Phân hệ 3: Quản trị & Đối soát (Thành viên 3) -->
        <div class="accordion-item border-0">
            <h2 class="accordion-header" id="headingThree">
                <button class="accordion-button collapsed py-2 px-2 rounded-2 bg-light text-secondary fw-semibold" type="button" data-bs-toggle="collapse" data-bs-target="#collapseThree">
                    <i class="bi bi-cash-coin me-2"></i>3. Quản trị & Tài chính Đối soát
                </button>
            </h2>
            <div id="collapseThree" class="accordion-collapse collapse" data-bs-parent="#sidebarAccordion">
                <div class="accordion-body p-2">
                    <ul class="nav flex-column small">
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/admin/hubs"><i class="bi bi-geo-alt me-2 text-secondary"></i>Cây bưu cục 3 cấp</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/admin/pricing"><i class="bi bi-tag me-2 text-secondary"></i>Biểu phí cước vùng</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/manager/reconciliation"><i class="bi bi-wallet2 me-2 text-secondary"></i>Đối soát ca Shipper</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/manager/daily-report"><i class="bi bi-file-earmark-bar-graph me-2 text-secondary"></i>Báo cáo ca ngày</a></li>
                        <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/admin/withdrawals"><i class="bi bi-bank me-2 text-secondary"></i>Duyệt lệnh rút tiền</a></li>
                    <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/manager/overdue-returns">Hàng hoàn quá hạn</a></li>
</ul>
                </div>
            </div>
        </div>
    </div>
</div>
