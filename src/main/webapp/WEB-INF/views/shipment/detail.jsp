<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="u" tagdir="/WEB-INF/tags" %>
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
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-4" style="max-width: 860px">
    <a class="btn btn-outline-secondary btn-sm mb-3" href="${pageContext.request.contextPath}/user/shipments">Đơn đã gửi</a>
    <a class="btn btn-outline-secondary btn-sm mb-3" href="${pageContext.request.contextPath}/user/shipments/create">Tạo đơn khác</a>
    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
    <section class="card-ute p-4 mb-3">
        <h1 class="h3">Vận đơn <span class="font-monospace"><c:out value="${shipment.trackingNumber}"/></span></h1>
        <p class="mb-1">Trạng thái: <u:statusbadge status="${shipment.currentStatus}"/></p>
        <p class="mb-1">Cước phí: <strong><fmt:formatNumber value="${shipment.shippingFee}" pattern="#,##0" var="shippingFormatted"/>${fn:replace(shippingFormatted, ',', '.')}đ</strong> (<c:out value="${shipment.paymentStatus}"/>)</p>
        <p class="text-ute-muted mb-0">Tạo lúc: <c:out value="${shipment.createdAt}"/></p>
        <c:if test="${not empty shipment.estimatedDelivery}"><p class="mb-0">Dự kiến giao trước: <strong><c:out value="${shipment.estimatedDelivery}"/></strong></p></c:if>
        <c:if test="${shipment.currentStatus == 'CANCELLED'}">
            <c:forEach items="${shipment.events}" var="event">
                <c:if test="${event.eventType == 'SHIPMENT_CANCELLED'}">
                    <div class="alert alert-warning mt-3 mb-0" role="status">
                        <c:choose>
                            <c:when test="${fn:contains(event.description, 'PAYMENT_TIMEOUT')}">Đơn đã tự hủy vì quá hạn thanh toán trực tuyến.</c:when>
                            <c:when test="${fn:contains(event.description, 'PICKUP_SERVICE_FAILURE')}">Đơn đã bị hủy do lỗi dịch vụ lấy hàng của UTEExpress.</c:when>
                            <c:when test="${fn:contains(event.description, 'PICKUP_TIMEOUT')}">Đơn đã tự hủy vì shipper không lấy được hàng trong thời hạn.</c:when>
                            <c:when test="${fn:contains(event.description, 'DROPOFF_TIMEOUT')}">Đơn đã tự hủy vì kiện hàng chưa được mang tới bưu cục trong thời hạn.</c:when>
                            <c:when test="${fn:contains(event.description, 'SENDER_REQUEST')}">Bạn đã hủy đơn này.</c:when>
                            <c:otherwise>Đơn hàng đã bị hủy.</c:otherwise>
                        </c:choose>
                        Nếu cước đã được thanh toán trước, tiền sẽ được hoàn vào ví của bạn.
                    </div>
                </c:if>
            </c:forEach>
        </c:if>
    </section>
    <section class="card-ute p-4 mb-3">
        <h2 class="h5 text-ute-primary">Bước tiếp theo</h2>
        <c:choose>
            <c:when test="${shipment.currentStatus == 'WAITING_FOR_PAYMENT'}">
                <p>Thanh toán cước trực tuyến để đơn chuyển sang chờ bàn giao. Đơn chưa thanh toán sẽ tự hủy khi quá hạn thanh toán.</p>
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
    <section class="card-ute p-4 mb-3" aria-labelledby="shipment-addresses-title">
        <h2 id="shipment-addresses-title" class="h5 text-ute-primary">Thông tin gửi và nhận</h2>
        <div class="row g-3">
            <div class="col-md-6"><h3 class="h6">Người gửi</h3><p class="mb-1"><c:out value="${shipment.senderName}"/> · <c:out value="${shipment.senderPhone}"/></p><p class="mb-0 text-ute-muted"><c:out value="${shipment.senderAddress}"/></p></div>
            <div class="col-md-6"><h3 class="h6">Người nhận</h3><p class="mb-1"><c:out value="${shipment.receiverName}"/> · <c:out value="${shipment.receiverPhone}"/></p><p class="mb-0 text-ute-muted"><c:out value="${shipment.receiverAddress}"/></p></div>
        </div>
    </section>
    <section class="card-ute p-4">
        <h2 class="h5 text-ute-primary">Lịch sử vận đơn</h2>
        <ul class="list-group list-group-flush">
            <c:forEach items="${shipment.events}" var="event">
                <li class="list-group-item px-0"><strong><c:out value="${event.eventType}"/></strong><br><c:out value="${event.description}"/> <span class="text-ute-muted small"><c:out value="${event.occurredAt}"/></span></li>
            </c:forEach>
        </ul>
    </section>
    <a class="btn btn-outline-primary mt-3" href="${pageContext.request.contextPath}/user/shipment-fees/${shipment.id}">Xem phí hoàn và phụ thu</a>
</main>
</body>
</html>
