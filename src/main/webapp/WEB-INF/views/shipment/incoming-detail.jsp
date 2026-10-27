<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
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
    <a class="btn btn-outline-secondary btn-sm mb-3" href="${pageContext.request.contextPath}/user/incoming">Danh sách đơn đến</a>
    <section class="card-ute p-4 mb-3">
        <h1 class="h3">Vận đơn <span class="font-monospace"><c:out value="${shipment.trackingNumber}"/></span></h1>
        <p class="mb-1">Trạng thái: <u:statusbadge status="${shipment.currentStatus}"/></p>
        <p class="mb-1">Người gửi: <c:out value="${shipment.senderName}"/></p>
        <p class="mb-1">Người nhận: <c:out value="${shipment.receiverName}"/></p>
        <c:if test="${shipment.unlocked}">
            <p class="mb-1">Địa chỉ nhận: <c:out value="${shipment.receiverAddress}"/></p>
            <p class="mb-0">Số tiền cần thanh toán khi nhận: <strong><fmt:formatNumber value="${shipment.amountToPay}" pattern="#,##0"/> VND</strong></p>
        </c:if>
    </section>
    <c:if test="${not shipment.unlocked}">
        <section class="card-ute p-4 mb-3">
            <form method="post" action="${pageContext.request.contextPath}/user/incoming/${shipment.id}/unlock" class="row g-2 align-items-end">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <div class="col-sm-8">
                    <label class="form-label" for="trackingNumber">Nhập đủ mã vận đơn để xem chi tiết</label>
                    <input class="form-control font-monospace" id="trackingNumber" name="trackingNumber" required maxlength="50" autocomplete="off">
                </div>
                <div class="col-sm-4"><button class="btn btn-ute-accent w-100" type="submit">Mở khóa</button></div>
            </form>
        </section>
    </c:if>
    <section class="card-ute p-4">
        <h2 class="h5 text-ute-primary">Hành trình</h2>
        <ul class="list-group list-group-flush">
            <c:forEach items="${shipment.events}" var="event">
                <li class="list-group-item px-0"><c:out value="${event.label}"/> <span class="text-ute-muted small"><c:out value="${event.occurredAt}"/></span></li>
            </c:forEach>
        </ul>
    </section>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
