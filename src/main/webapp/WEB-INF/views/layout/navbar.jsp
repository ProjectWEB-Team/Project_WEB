<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="u" tagdir="/WEB-INF/tags" %>
<%-- Shared navigation. Staff roles (hub, manager, admin) get a side rail on wide screens; everyone else gets a top bar.
     Pages with their own <head> (no layout/header.jsp) still get the whole skin from here. --%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="me" value="${pageContext.request.userPrincipal}" />
<c:set var="displayName" value="${not empty userDisplayName ? userDisplayName : (not empty me ? me.name : '')}" />
<c:set var="rail" value="${pageContext.request.isUserInRole('HUB_STAFF') or pageContext.request.isUserInRole('MANAGER') or pageContext.request.isUserInRole('ADMIN')}" />
<c:if test="${empty uteAssetsLoaded}">
    <link href="https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/assets/css/ute-express.css">
    <link rel="stylesheet" href="${ctx}/assets/css/ute-modern.css">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" defer></script>
</c:if>
<a class="ute-skip" href="#main">Bỏ qua điều hướng</a>
<nav id="${rail ? 'uteSidebar' : 'uteNavigation'}" class="navbar navbar-expand-lg ute-nav${rail ? ' ute-nav-rail' : ''}" aria-label="Điều hướng chính">
    <div class="ute-nav-inner">
        <a class="navbar-brand" href="${ctx}/" aria-label="UTEexpress — Trang chủ"><jsp:include page="/WEB-INF/views/layout/logo.jsp" /></a>
        <c:if test="${rail}"><p class="ute-rail-role"><c:choose><c:when test="${pageContext.request.isUserInRole('ADMIN')}">Quản trị hệ thống</c:when><c:when test="${pageContext.request.isUserInRole('MANAGER')}">Quản lý vận hành</c:when><c:otherwise>Nhân viên bưu cục</c:otherwise></c:choose></p></c:if>
        <button class="navbar-toggler ute-menu-btn" type="button" data-bs-toggle="collapse" data-bs-target="#uteNav" aria-controls="uteNav" aria-expanded="false" aria-label="Mở menu">Menu</button>
        <div class="collapse navbar-collapse" id="uteNav">
            <ul class="navbar-nav">
                <u:navlink href="/tracking">Tra cứu</u:navlink>
                <u:navlink href="/pricing">Bảng giá</u:navlink>
                <c:if test="${empty me}">
                    <li class="nav-item"><a class="nav-link" href="${ctx}/#services-title">Dịch vụ</a></li>
                    <li class="nav-item"><a class="nav-link" href="${ctx}/#how-title">Cách gửi hàng</a></li>
                    <li class="nav-item"><a class="nav-link" href="${ctx}/#faq">Hỏi đáp</a></li>
                    <li class="nav-item"><a class="nav-link" href="#ute-contact">Liên hệ</a></li>
                </c:if>
                <c:if test="${pageContext.request.isUserInRole('USER')}">
                    <u:navlink href="/user/shipments/create">Tạo đơn</u:navlink>
                    <u:navlink href="/user/shipments">Đơn của tôi</u:navlink>
                    <u:navlink href="/user/incoming">Đơn đang đến</u:navlink>
                    <u:navlink href="/user/wallet">Ví</u:navlink>
                    <c:if test="${emailLoginEnabled}"><u:navlink href="/user/email">Email của tôi</u:navlink></c:if>
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
                    <u:navlink href="/admin/pricing">Quản lý giá</u:navlink>
                    <u:navlink href="/admin/network">Mạng lưới</u:navlink>
                    <u:navlink href="/admin/withdrawals">Duyệt rút tiền</u:navlink>
                </c:if>
            </ul>
            <div class="ute-nav-actions">
                <c:choose>
                    <c:when test="${not empty me}">
                        <c:choose>
                            <c:when test="${rail}">
                                <div class="ute-sidebar-account">
                                    <div class="ute-sidebar-account-user">
                                        <span class="ute-user-avatar${pageContext.request.isUserInRole('HUB_STAFF') ? '' : ' is-dark'}"><c:out value="${fn:toUpperCase(fn:substring(displayName, 0, 2))}" /></span>
                                        <div class="ute-user-meta">
                                            <span class="ute-user-name" title="<c:out value='${me.name}' />"><c:out value="${displayName}" /></span>
                                            <span class="ute-user-role">
                                                <c:choose>
                                                    <c:when test="${pageContext.request.isUserInRole('ADMIN')}">Quản trị viên hệ thống</c:when>
                                                    <c:when test="${pageContext.request.isUserInRole('MANAGER')}">Quản lý điều hành</c:when>
                                                    <c:when test="${pageContext.request.isUserInRole('HUB_STAFF')}">Nhân viên bưu cục Hub</c:when>
                                                    <c:when test="${pageContext.request.isUserInRole('SHIPPER')}">Nhân viên giao nhận</c:when>
                                                    <c:when test="${pageContext.request.isUserInRole('LINEHAUL_DRIVER')}">Tài xế trung chuyển</c:when>
                                                    <c:otherwise>Thành viên UTEExpress</c:otherwise>
                                                </c:choose>
                                            </span>
                                        </div>
                                    </div>
                                    <form method="post" action="${ctx}/logout" class="m-0 w-100">
                                        <c:if test="${not empty _csrf}"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"></c:if>
                                        <button type="submit" class="btn btn-sm btn-ute-logout-rail w-100">Đăng xuất phiên làm việc</button>
                                    </form>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="ute-user-pill">
                                    <span class="ute-user-avatar"><c:out value="${fn:toUpperCase(fn:substring(displayName, 0, 2))}" /></span>
                                    <div class="ute-user-meta">
                                        <span class="ute-user-name" title="<c:out value='${me.name}' />"><c:out value="${displayName}" /></span>
                                        <span class="ute-user-role">
                                            <c:choose>
                                                <c:when test="${pageContext.request.isUserInRole('USER')}">Khách hàng</c:when>
                                                <c:otherwise>Tài khoản</c:otherwise>
                                            </c:choose>
                                        </span>
                                    </div>
                                    <span class="ute-pill-divider" aria-hidden="true"></span>
                                    <form method="post" action="${ctx}/logout" class="m-0 d-inline">
                                        <c:if test="${not empty _csrf}"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"></c:if>
                                        <button type="submit" class="btn-ute-logout-pill">Đăng xuất</button>
                                    </form>
                                </div>
                            </c:otherwise>
                        </c:choose>
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
<c:if test="${rail}">
    <div class="ute-sidebar-toolbar" hidden>
        <button id="uteSidebarToggle" class="btn btn-outline-secondary" type="button" aria-controls="uteSidebar" aria-expanded="true">Thu gọn menu</button>
    </div>
    <script src="${ctx}/assets/js/sidebar-nav.js"></script>
</c:if>
<script>document.addEventListener('DOMContentLoaded', function () { var m = document.querySelector('main'); if (m && !m.id) m.id = 'main'; });</script>
