<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="u" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đơn đang đến | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
</head>
<body class="bg-ute-canvas">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-4" style="max-width: 960px">
    <c:if test="${not empty message}"><div class="alert alert-success" role="status"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
    <h1 class="h3 mb-3">Đơn đang đến</h1>
    <p class="text-ute-muted">Thông tin được che. Mở từng đơn và nhập đủ mã vận đơn để xem chi tiết.</p>
    <section class="card-ute p-0">
        <c:choose>
            <c:when test="${shipments.totalElements == 0}"><p class="p-4 mb-0 text-ute-muted">Chưa có đơn nào gửi đến bạn.</p></c:when>
            <c:otherwise>
                <table class="table mb-0 align-middle">
                    <thead><tr><th>Mã vận đơn</th><th>Người gửi</th><th>Trạng thái</th><th>Tạo lúc</th></tr></thead>
                    <tbody>
                    <c:forEach items="${shipments.content}" var="s">
                        <tr>
                            <td><a class="font-monospace" href="${pageContext.request.contextPath}/user/incoming/${s.id}"><c:out value="${s.maskedTrackingNumber}"/></a></td>
                            <td><c:out value="${s.maskedSenderName}"/></td>
                            <td><u:statusbadge status="${s.currentStatus}"/></td>
                            <td class="small text-ute-muted"><c:out value="${s.createdAt}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </section>
    <nav class="d-flex justify-content-between mt-3" aria-label="Phân trang">
        <c:if test="${shipments.hasPrevious()}"><a class="btn btn-outline-secondary btn-sm" href="?page=${shipments.number - 1}">Trang trước</a></c:if>
        <c:if test="${shipments.hasNext()}"><a class="btn btn-outline-secondary btn-sm" href="?page=${shipments.number + 1}">Trang sau</a></c:if>
    </nav>
</main>
</body>
</html>
