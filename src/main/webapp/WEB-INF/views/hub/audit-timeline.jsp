<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lịch sử bàn giao kiện hàng</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-4">
    <h1 class="h3">Lịch sử bàn giao kiện hàng</h1>
    <p class="text-muted">Đơn #<c:out value="${timeline.shipmentId}"/></p>
    <div class="table-responsive">
        <table class="table table-striped align-middle">
            <thead><tr><th>Sự kiện</th><th>Thời gian</th><th>Bưu cục</th><th>Vai trò người thực hiện</th><th>Tham chiếu bàn giao</th></tr></thead>
            <tbody>
            <c:forEach items="${timeline.entries}" var="entry">
                <tr>
                    <td><c:out value="${entry.eventType}"/></td>
                    <td><c:out value="${entry.occurredAt}"/></td>
                    <td><c:out value="${entry.hubCode}"/></td>
                    <td><c:out value="${entry.actorRole}"/></td>
                    <td>Bao hàng <c:out value="${entry.containerId}"/> / Bàn giao <c:out value="${entry.handoverId}"/> / Chuyến <c:out value="${entry.tripId}"/></td>
                </tr>
            </c:forEach>
            <c:if test="${empty timeline.entries}"><tr><td colspan="5" class="text-muted">Chưa có sự kiện vận hành nào bạn được phép xem.</td></tr></c:if>
            </tbody>
        </table>
    </div>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
