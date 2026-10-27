<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phân công lấy hàng | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
</head>
<body class="bg-ute-canvas">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-4" style="max-width: 960px">
    <c:if test="${not empty message}"><div class="alert alert-success" role="status"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
    <h1 class="h3 mb-3">Đơn chờ phân công lấy hàng</h1>
    <c:if test="${empty shippers}"><div class="alert alert-warning" role="status">Bưu cục chưa có shipper đang hoạt động.</div></c:if>
    <c:choose>
        <c:when test="${empty pickups}"><p class="card-ute p-4 text-ute-muted">Không có đơn chờ lấy hàng.</p></c:when>
        <c:otherwise>
            <c:forEach items="${pickups}" var="p">
                <section class="card-ute p-4 mb-3">
                    <h2 class="h6 font-monospace"><c:out value="${p.trackingNumber}"/></h2>
                    <p class="mb-1"><c:out value="${p.senderName}"/> · <c:out value="${p.senderPhone}"/></p>
                    <p class="mb-2 text-ute-muted"><c:out value="${p.senderAddress}"/> · chờ từ <c:out value="${p.waitingSince}"/></p>
                    <form class="row g-2 mb-2" method="post" action="${pageContext.request.contextPath}/hub/pickups/${p.shipmentId}/assign">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <div class="col-sm-8">
                            <label class="visually-hidden" for="shipper-${p.shipmentId}">Shipper</label>
                            <select class="form-select" id="shipper-${p.shipmentId}" name="shipperId" required>
                                <c:forEach items="${shippers}" var="s"><option value="${s.userId}"><c:out value="${s.fullName}"/></option></c:forEach>
                            </select>
                        </div>
                        <div class="col-sm-4"><button class="btn btn-ute-accent w-100" type="submit">Phân công</button></div>
                    </form>
                    <form class="row g-2" method="post" action="${pageContext.request.contextPath}/hub/pickups/${p.shipmentId}/service-failure">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <div class="col-sm-8">
                            <label class="visually-hidden" for="note-${p.shipmentId}">Lý do lỗi dịch vụ</label>
                            <input class="form-control" id="note-${p.shipmentId}" name="note" maxlength="200" required placeholder="Lý do lỗi dịch vụ UTEExpress">
                        </div>
                        <div class="col-sm-4"><button class="btn btn-outline-danger w-100" type="submit">Hủy do lỗi dịch vụ</button></div>
                    </form>
                </section>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
