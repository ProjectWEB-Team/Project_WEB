<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Bảng điều hành</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="ute-staff flex-grow-1">
    <h1>Bảng điều hành</h1>
    <p class="ute-staff-sub">Phạm vi: <c:out value="${dashboard.scopeLabel}"/></p>
    <div class="ute-kpi-grid" aria-label="Chỉ số vận hành trong phạm vi">
        <div class="ute-kpi"><h2>Đang nhận tại hub</h2><p><c:out value="${dashboard.inboundCount}"/> kiện</p></div>
        <div class="ute-kpi"><h2>Bao chờ xuất</h2><p><c:out value="${dashboard.outboundCount}"/> bao</p></div>
        <div class="ute-kpi"><h2>Tồn chờ phân loại</h2><p><c:out value="${dashboard.sortingBacklogCount}"/> kiện</p></div>
        <div class="ute-kpi"><h2>Phiếu bàn giao đang mở</h2><p><c:out value="${dashboard.activeHandoverCount}"/> phiếu</p></div>
        <div class="ute-kpi"><h2>Chuyến đang chạy</h2><p><c:out value="${dashboard.activeTripCount}"/> chuyến</p></div>
        <div class="ute-kpi"><h2>Tuyến bị chặn</h2><p><c:out value="${dashboard.blockedRouteCount}"/> tuyến</p></div>
        <div class="ute-kpi"><h2>Sự cố cần xử lý</h2><p><c:out value="${dashboard.actionableIncidentCount}"/> việc</p></div>
    </div>
    <section>
        <h2 class="ute-staff-h2">Việc cần xử lý</h2>
        <p class="ute-staff-sub">Tối đa 25 việc mỗi loại trong phạm vi của bạn.</p>
        <div class="ute-work-card">
            <p class="ute-work-head">${empty workItems ? 0 : workItems.size()} việc đang chờ xử lý</p>
            <c:forEach items="${workItems}" var="item">
                <div class="ute-work-row">
                    <strong><c:out value="${item.summary}"/> &middot; #<c:out value="${item.id}"/></strong>
                    <a class="btn ute-btn-view" href="${pageContext.request.contextPath}/manager/operations/work/${item.kind}/${item.id}">Mở xử lý</a>
                </div>
            </c:forEach>
            <c:if test="${empty workItems}"><p class="ute-work-empty">Không có việc cần xử lý trong phạm vi này.</p></c:if>
        </div>
    </section>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
