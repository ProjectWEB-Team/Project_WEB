<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Vận đơn <c:out value="${shipment.trackingNumber}"/> | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
</head>
<body class="bg-ute-canvas">
<main class="container py-4" style="max-width: 860px">
    <a class="btn btn-outline-secondary btn-sm mb-3" href="${pageContext.request.contextPath}/user/shipments">Đơn đã gửi</a>
    <a class="btn btn-outline-secondary btn-sm mb-3" href="${pageContext.request.contextPath}/user/shipments/create">Tạo đơn khác</a>
    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
    <section class="card-ute p-4 mb-3">
        <h1 class="h3">Vận đơn <span class="font-monospace"><c:out value="${shipment.trackingNumber}"/></span></h1>
        <p class="mb-1">Trạng thái: <span class="badge text-bg-primary"><c:out value="${shipment.currentStatus}"/></span></p>
        <p class="mb-1">Cước phí: <strong><fmt:formatNumber value="${shipment.shippingFee}" pattern="#,##0"/> VND</strong> (<c:out value="${shipment.paymentStatus}"/>)</p>
        <p class="text-ute-muted mb-0">Tạo lúc: <c:out value="${shipment.createdAt}"/></p>
    </section>
    <section class="card-ute p-4 mb-3">
        <h2 class="h5 text-ute-primary">Bước tiếp theo</h2>
        <c:choose>
            <c:when test="${shipment.currentStatus == 'WAITING_FOR_PAYMENT'}">
                <p>Thanh toán cước trực tuyến để đơn chuyển sang chờ bàn giao.</p>
                <form method="post" action="${pageContext.request.contextPath}/user/shipments/${shipment.id}/confirm-payment">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <button class="btn btn-ute-accent" type="submit">Xác nhận thanh toán (demo)</button>
                </form>
            </c:when>
            <c:when test="${shipment.currentStatus == 'WAITING_FOR_HANDOVER'}">
                <c:choose>
                    <c:when test="${shipment.pickupMethod == 'HOME_PICKUP'}"><p class="mb-1">Shipper sẽ liên hệ để lấy hàng tại địa chỉ người gửi.</p></c:when>
                    <c:otherwise><p class="mb-1">Vui lòng mang kiện hàng ra bưu cục để bàn giao.</p></c:otherwise>
                </c:choose>
                <c:if test="${shipment.paymentStatus != 'PAID'}">
                    <p class="mb-0 text-ute-muted">
                        <c:choose>
                            <c:when test="${shipment.feePayer == 'RECEIVER'}">Cước phí sẽ được thu từ người nhận khi giao hàng.</c:when>
                            <c:otherwise>Cước phí tiền mặt sẽ được thu khi bàn giao kiện hàng, không thu lúc tạo đơn.</c:otherwise>
                        </c:choose>
                    </p>
                </c:if>
            </c:when>
            <c:otherwise><p class="mb-0 text-ute-muted">Không có thao tác nào lúc này.</p></c:otherwise>
        </c:choose>
        <c:if test="${shipment.currentStatus == 'CREATED' or shipment.currentStatus == 'WAITING_FOR_HANDOVER'}">
            <form class="mt-3" method="post" action="${pageContext.request.contextPath}/user/shipments/${shipment.id}/cancel">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <button class="btn btn-outline-danger btn-sm" type="submit">Hủy đơn</button>
            </form>
        </c:if>
    </section>
    <section class="card-ute p-4">
        <h2 class="h5 text-ute-primary">Lịch sử vận đơn</h2>
        <ul class="list-group list-group-flush">
            <c:forEach items="${shipment.events}" var="event">
                <li class="list-group-item px-0"><strong><c:out value="${event.eventType}"/></strong><br><c:out value="${event.description}"/> <span class="text-ute-muted small"><c:out value="${event.occurredAt}"/></span></li>
            </c:forEach>
        </ul>
    </section>
<p><a href="${pageContext.request.contextPath}/user/shipment-fees/${shipment.id}">Thanh toán phụ thu / phí hoàn</a></p>
</main>
</body>
</html>
