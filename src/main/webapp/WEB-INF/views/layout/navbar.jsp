<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Shared top navigation: role links and logout. No JavaScript, so it also works on pages without the Bootstrap bundle. --%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="me" value="${pageContext.request.userPrincipal}" />
<%-- Pages with their own <head> (no layout/header.jsp) still get the full skin from here. --%>
<c:if test="${empty uteAssetsLoaded}">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/assets/css/ute-express.css">
    <link rel="stylesheet" href="${ctx}/assets/css/ute-modern.css">
</c:if>
<nav class="navbar navbar-expand ute-nav py-2" aria-label="Điều hướng chính">
    <div class="container-fluid flex-wrap gap-2 px-lg-4">
        <a class="navbar-brand fw-bold" href="${ctx}/">UTE<span>EXPRESS</span></a>
        <ul class="navbar-nav flex-wrap me-auto">
            <li class="nav-item"><a class="nav-link" href="${ctx}/tracking">Tra cứu</a></li>
            <c:if test="${pageContext.request.isUserInRole('USER')}">
                <li class="nav-item"><a class="nav-link" href="${ctx}/user/shipments">Đơn của tôi</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/user/shipments/create">Tạo đơn</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/user/incoming">Đơn đang đến</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/user/wallet">Ví</a></li>
            </c:if>
            <c:if test="${pageContext.request.isUserInRole('HUB_STAFF')}">
                <li class="nav-item"><a class="nav-link" href="${ctx}/hub/pickups">Lấy hàng</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/hub/inbound">Nhận hàng</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/hub/sorter">Phân loại</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/hub/outbound">Đóng bao</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/hub/delivery-assignments">Phân công giao</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/hub/return-collection">Hàng hoàn</a></li>
            </c:if>
            <c:if test="${pageContext.request.isUserInRole('SHIPPER')}">
                <li class="nav-item"><a class="nav-link" href="${ctx}/shipper/tasks">Lấy hàng</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/shipper/delivery-tasks">Giao hàng</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/shipper/return-tasks">Hàng hoàn</a></li>
            </c:if>
            <c:if test="${pageContext.request.isUserInRole('LINEHAUL_DRIVER')}">
                <li class="nav-item"><a class="nav-link" href="${ctx}/driver">Chuyến xe</a></li>
            </c:if>
            <c:if test="${pageContext.request.isUserInRole('MANAGER') or pageContext.request.isUserInRole('ADMIN')}">
                <li class="nav-item"><a class="nav-link" href="${ctx}/manager/operations">Vận hành</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/manager/reconciliation">Đối soát</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/manager/incidents/missing">Thất lạc</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/manager/overdue-returns">Hoàn quá hạn</a></li>
            </c:if>
            <c:if test="${pageContext.request.isUserInRole('ADMIN')}">
                <li class="nav-item"><a class="nav-link" href="${ctx}/admin/users">Người dùng</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/admin/pricing">Bảng giá</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/admin/network">Mạng lưới</a></li>
                <li class="nav-item"><a class="nav-link" href="${ctx}/admin/withdrawals">Duyệt rút tiền</a></li>
            </c:if>
        </ul>
        <div class="d-flex align-items-center gap-2">
            <c:choose>
                <c:when test="${not empty me}">
                    <span class="text-white small"><c:out value="${me.name}" /></span>
                    <form method="post" action="${ctx}/logout" class="m-0">
                        <c:if test="${not empty _csrf}"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"></c:if>
                        <button type="submit" class="btn btn-outline-light btn-sm">Đăng xuất</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <a class="btn btn-outline-light btn-sm" href="${ctx}/login">Đăng nhập</a>
                    <a class="btn btn-light btn-sm" href="${ctx}/register">Đăng ký</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</nav>
