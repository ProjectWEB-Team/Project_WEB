<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="rq" value="${pageContext.request}" />

<!-- Sidebar: only the work areas the signed-in role can open -->
<div class="card-ute p-3 mb-4">
    <div class="d-flex align-items-center gap-2 mb-3 pb-2 border-bottom">
        <i class="bi bi-person-badge fs-4 text-ute-primary"></i>
        <div>
            <h6 class="mb-0 fw-bold text-ute-dark">Menu Tác Nghiệp</h6>
            <small class="text-ute-muted">Theo vai trò của bạn</small>
        </div>
    </div>

    <div id="sidebarAccordion">
        <c:if test="${empty rq.userPrincipal}">
            <ul class="nav flex-column small">
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/tracking"><i class="bi bi-search me-2 text-ute-primary"></i>Tra cứu vận đơn</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/login"><i class="bi bi-box-arrow-in-right me-2 text-ute-primary"></i>Đăng nhập</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/register"><i class="bi bi-person-plus me-2 text-ute-primary"></i>Đăng ký</a></li>
            </ul>
        </c:if>
        <c:if test="${rq.isUserInRole('USER')}">
            <ul class="nav flex-column small">
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/user/shipments/create"><i class="bi bi-plus-circle me-2 text-ute-accent"></i>Tạo vận đơn gửi</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/user/shipments"><i class="bi bi-boxes me-2 text-ute-accent"></i>Quản lý đơn hàng</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/user/incoming"><i class="bi bi-inbox me-2 text-ute-accent"></i>Đơn đang đến</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/user/wallet"><i class="bi bi-wallet2 me-2 text-ute-accent"></i>Ví điện tử</a></li>
            </ul>
        </c:if>
        <c:if test="${rq.isUserInRole('HUB_STAFF')}">
            <ul class="nav flex-column small">
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/hub/pickups"><i class="bi bi-person-check me-2 text-ute-primary"></i>Phân công lấy hàng</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/hub/inbound"><i class="bi bi-qr-code-scan me-2 text-ute-primary"></i>Nhận hàng vào hub</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/hub/sorter"><i class="bi bi-diagram-2 me-2 text-ute-primary"></i>Phân loại</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/hub/outbound"><i class="bi bi-box-arrow-up-right me-2 text-ute-primary"></i>Đóng bao, bàn giao</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/hub/delivery-assignments"><i class="bi bi-signpost-split me-2 text-ute-primary"></i>Phân công giao / hoàn hàng</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/hub/return-collection"><i class="bi bi-arrow-return-left me-2 text-ute-primary"></i>Bàn giao hàng hoàn tại hub</a></li>
            </ul>
        </c:if>
        <c:if test="${rq.isUserInRole('SHIPPER')}">
            <ul class="nav flex-column small">
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/shipper/tasks"><i class="bi bi-bicycle me-2 text-ute-accent"></i>Nhiệm vụ lấy hàng</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/shipper/delivery-tasks"><i class="bi bi-truck me-2 text-ute-accent"></i>Nhiệm vụ giao hàng</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/shipper/return-tasks"><i class="bi bi-arrow-return-left me-2 text-ute-accent"></i>Giao hàng hoàn</a></li>
            </ul>
        </c:if>
        <c:if test="${rq.isUserInRole('LINEHAUL_DRIVER')}">
            <ul class="nav flex-column small">
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/driver"><i class="bi bi-truck me-2 text-ute-primary"></i>Chuyến xe trung chuyển</a></li>
            </ul>
        </c:if>
        <c:if test="${rq.isUserInRole('MANAGER') or rq.isUserInRole('ADMIN')}">
            <ul class="nav flex-column small">
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/manager/operations"><i class="bi bi-speedometer2 me-2 text-secondary"></i>Bảng điều hành</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/manager/reconciliation"><i class="bi bi-wallet2 me-2 text-secondary"></i>Đối soát ca Shipper</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/manager/incidents/missing"><i class="bi bi-shield-exclamation me-2 text-secondary"></i>Hàng thất lạc</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/manager/overdue-returns"><i class="bi bi-hourglass-split me-2 text-secondary"></i>Hàng hoàn quá hạn</a></li>
            </ul>
        </c:if>
        <c:if test="${rq.isUserInRole('ADMIN')}">
            <ul class="nav flex-column small">
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/admin/users"><i class="bi bi-people me-2 text-secondary"></i>Người dùng và nhân viên</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/admin/network"><i class="bi bi-diagram-3 me-2 text-secondary"></i>Mạng lưới bưu cục, tuyến</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/admin/pricing"><i class="bi bi-tag me-2 text-secondary"></i>Bảng giá cước</a></li>
                <li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${ctx}/admin/withdrawals"><i class="bi bi-bank me-2 text-secondary"></i>Duyệt lệnh rút tiền</a></li>
            </ul>
        </c:if>
    </div>
</div>
