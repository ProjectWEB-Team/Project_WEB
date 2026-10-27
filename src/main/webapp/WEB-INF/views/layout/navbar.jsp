<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="u" tagdir="/WEB-INF/tags" %>
<%-- Shared navigation. Staff roles (hub, manager, admin) get a side rail on wide screens; everyone else gets a top bar.
     Pages with their own <head> (no layout/header.jsp) still get the whole skin from here. --%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="me" value="${pageContext.request.userPrincipal}" />
<c:set var="rail" value="${pageContext.request.isUserInRole('HUB_STAFF') or pageContext.request.isUserInRole('MANAGER') or pageContext.request.isUserInRole('ADMIN')}" />
<c:if test="${empty uteAssetsLoaded}">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/assets/css/ute-express.css">
    <link rel="stylesheet" href="${ctx}/assets/css/ute-modern.css">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" defer></script>
</c:if>
<a class="ute-skip" href="#main">Bỏ qua điều hướng</a>
<c:if test="${uteTopBar}">
    <div class="ute-topbar"><div class="ute-topbar-inner">
        <span>Tổng đài 24/7: <a href="tel:1900UTEEXPRESS">1900-UTE-EXPRESS</a> · <a href="mailto:support@uteexpress.edu.vn">support@uteexpress.edu.vn</a></span>
        <span><a href="${ctx}/tracking">Tra cứu vận đơn</a> · <a href="${ctx}/#faq">Câu hỏi thường gặp</a></span>
    </div></div>
</c:if>
<nav class="navbar navbar-expand-lg ute-nav${rail ? ' ute-nav-rail' : ''}" aria-label="Điều hướng chính">
    <div class="ute-nav-inner">
        <a class="navbar-brand" href="${ctx}/">UTE<span>EXPRESS</span></a>
        <button class="navbar-toggler ute-menu-btn" type="button" data-bs-toggle="collapse" data-bs-target="#uteNav" aria-controls="uteNav" aria-expanded="false" aria-label="Mở menu">Menu</button>
        <div class="collapse navbar-collapse" id="uteNav">
            <ul class="navbar-nav">
                <u:navlink href="/tracking">Tra cứu</u:navlink>
                <c:if test="${pageContext.request.isUserInRole('USER')}">
                    <u:navlink href="/user/shipments/create">Tạo đơn</u:navlink>
                    <u:navlink href="/user/shipments">Đơn của tôi</u:navlink>
                    <u:navlink href="/user/incoming">Đơn đang đến</u:navlink>
                    <u:navlink href="/user/wallet">Ví</u:navlink>
                </c:if>
                <c:if test="${pageContext.request.isUserInRole('HUB_STAFF')}">
                    <u:navlink href="/hub/pickups">Lấy hàng</u:navlink>
                    <u:navlink href="/hub/inbound">Nhận hàng</u:navlink>
                    <u:navlink href="/hub/sorter">Phân loại</u:navlink>
                    <u:navlink href="/hub/outbound">Đóng bao</u:navlink>
                    <u:navlink href="/hub/delivery-assignments">Phân công giao</u:navlink>
                    <u:navlink href="/hub/return-collection">Hàng hoàn</u:navlink>
                </c:if>
                <c:if test="${pageContext.request.isUserInRole('SHIPPER')}">
                    <u:navlink href="/shipper/tasks">Lấy hàng</u:navlink>
                    <u:navlink href="/shipper/delivery-tasks">Giao hàng</u:navlink>
                    <u:navlink href="/shipper/return-tasks">Hàng hoàn</u:navlink>
                </c:if>
                <c:if test="${pageContext.request.isUserInRole('LINEHAUL_DRIVER')}">
                    <u:navlink href="/driver">Chuyến xe</u:navlink>
                </c:if>
                <c:if test="${pageContext.request.isUserInRole('MANAGER') or pageContext.request.isUserInRole('ADMIN')}">
                    <u:navlink href="/manager/operations">Vận hành</u:navlink>
                    <u:navlink href="/manager/reconciliation">Đối soát</u:navlink>
                    <u:navlink href="/manager/incidents/missing">Thất lạc</u:navlink>
                    <u:navlink href="/manager/overdue-returns">Hoàn quá hạn</u:navlink>
                </c:if>
                <c:if test="${pageContext.request.isUserInRole('ADMIN')}">
                    <u:navlink href="/admin/users">Người dùng</u:navlink>
                    <u:navlink href="/admin/pricing">Bảng giá</u:navlink>
                    <u:navlink href="/admin/network">Mạng lưới</u:navlink>
                    <u:navlink href="/admin/withdrawals">Duyệt rút tiền</u:navlink>
                </c:if>
            </ul>
            <div class="ute-nav-actions">
                <c:choose>
                    <c:when test="${not empty me}">
                        <span class="ute-nav-user"><c:out value="${me.name}" /></span>
                        <form method="post" action="${ctx}/logout" class="m-0">
                            <c:if test="${not empty _csrf}"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"></c:if>
                            <button type="submit" class="ute-link-btn">Đăng xuất</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <a class="ute-link-btn" href="${ctx}/login">Đăng nhập</a>
                        <a class="btn btn-ute-accent btn-sm" href="${ctx}/register">Đăng ký</a>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</nav>
<script>document.addEventListener('DOMContentLoaded', function () { var m = document.querySelector('main'); if (m && !m.id) m.id = 'main'; });</script>
