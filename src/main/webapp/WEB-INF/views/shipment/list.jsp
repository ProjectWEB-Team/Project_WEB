<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đơn đã gửi | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
</head>
<body class="bg-ute-canvas">
<main class="container py-4" style="max-width: 960px">
    <c:if test="${not empty message}"><div class="alert alert-success" role="status"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h1 class="h3 mb-0">Đơn đã gửi</h1>
        <a class="btn btn-ute-accent btn-sm" href="${pageContext.request.contextPath}/user/shipments/create">Tạo đơn mới</a>
    </div>
    <form class="d-flex gap-2 mb-3" method="get">
        <label class="visually-hidden" for="status">Trạng thái</label>
        <select class="form-select form-select-sm w-auto" id="status" name="status">
            <option value="">Tất cả trạng thái</option>
            <c:forEach items="${statuses}" var="s">
                <option value="${s}" <c:if test="${s == status}">selected</c:if>><c:out value="${s}"/></option>
            </c:forEach>
        </select>
        <button class="btn btn-outline-secondary btn-sm" type="submit">Lọc</button>
    </form>
    <section class="card-ute p-0">
        <c:choose>
            <c:when test="${shipments.totalElements == 0}"><p class="p-4 mb-0 text-ute-muted">Chưa có đơn nào.</p></c:when>
            <c:otherwise>
                <table class="table mb-0 align-middle">
                    <thead><tr><th>Mã vận đơn</th><th>Người nhận</th><th>Trạng thái</th><th class="text-end">Cước</th><th>Tạo lúc</th></tr></thead>
                    <tbody>
                    <c:forEach items="${shipments.content}" var="s">
                        <tr>
                            <td><a class="font-monospace" href="${pageContext.request.contextPath}/user/shipments/${s.id}"><c:out value="${s.trackingNumber}"/></a></td>
                            <td><c:out value="${s.receiverName}"/></td>
                            <td><span class="badge text-bg-primary"><c:out value="${s.currentStatus}"/></span></td>
                            <td class="text-end"><fmt:formatNumber value="${s.shippingFee}" pattern="#,##0"/></td>
                            <td class="small text-ute-muted"><c:out value="${s.createdAt}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </section>
    <nav class="d-flex justify-content-between mt-3" aria-label="Phân trang">
        <c:if test="${shipments.hasPrevious()}"><a class="btn btn-outline-secondary btn-sm" href="?status=${status}&page=${shipments.number - 1}">Trang trước</a></c:if>
        <span class="small text-ute-muted">Trang ${shipments.number + 1} / ${shipments.totalPages == 0 ? 1 : shipments.totalPages}</span>
        <c:if test="${shipments.hasNext()}"><a class="btn btn-outline-secondary btn-sm" href="?status=${status}&page=${shipments.number + 1}">Trang sau</a></c:if>
    </nav>
</main>
</body>
</html>
