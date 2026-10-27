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
<main class="container py-4">
    <header class="mb-4">
        <h1 class="h3 mb-1">Bảng điều hành</h1>
        <p class="text-muted mb-0">Phạm vi: <c:out value="${dashboard.scopeLabel}"/></p>
    </header>
    <div class="row row-cols-1 row-cols-md-3 g-3" aria-label="Chỉ số vận hành trong phạm vi">
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Đang nhận tại hub</h2><p class="display-6 mb-0"><c:out value="${dashboard.inboundCount}"/></p></div></div></div>
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Bao chờ xuất</h2><p class="display-6 mb-0"><c:out value="${dashboard.outboundCount}"/></p></div></div></div>
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Tồn chờ phân loại</h2><p class="display-6 mb-0"><c:out value="${dashboard.sortingBacklogCount}"/></p></div></div></div>
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Phiếu bàn giao đang mở</h2><p class="display-6 mb-0"><c:out value="${dashboard.activeHandoverCount}"/></p></div></div></div>
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Chuyến đang chạy</h2><p class="display-6 mb-0"><c:out value="${dashboard.activeTripCount}"/></p></div></div></div>
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Tuyến bị chặn</h2><p class="display-6 mb-0"><c:out value="${dashboard.blockedRouteCount}"/></p></div></div></div>
        <div class="col w-100"><div class="card h-100"><div class="card-body d-flex align-items-center justify-content-between gap-3"><h2 class="h6 mb-0">Sự cố cần xử lý</h2><p class="display-6 mb-0"><c:out value="${dashboard.actionableIncidentCount}"/></p></div></div></div>
    </div>
    <section class="mt-4">
        <h2 class="h5">Việc cần xử lý</h2>
        <p class="text-muted">Tối đa 25 việc mỗi loại trong phạm vi của bạn.</p>
        <ul class="list-group">
            <c:forEach items="${workItems}" var="item">
                <li class="list-group-item"><a href="${pageContext.request.contextPath}/manager/operations/work/${item.kind}/${item.id}"><c:out value="${item.summary}"/> #<c:out value="${item.id}"/></a></li>
            </c:forEach>
            <c:if test="${empty workItems}"><li class="list-group-item text-muted">Không có việc cần xử lý trong phạm vi này.</li></c:if>
        </ul>
    </section>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
