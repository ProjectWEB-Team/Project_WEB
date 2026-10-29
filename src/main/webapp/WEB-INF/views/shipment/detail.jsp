<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
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
<main class="ute-app flex-grow-1">
  <div class="ute-wrap">
    <div class="ute-detail-bar">
        <div class="d-flex gap-2">
            <a class="btn ute-btn-ghost" href="${pageContext.request.contextPath}/user/shipments">&larr; Quay lại Đơn đã gửi</a>
            <a class="btn ute-btn-ghost" href="${pageContext.request.contextPath}/user/shipments/create">+ Tạo đơn khác</a>
        </div>
        <a class="btn ute-btn-ghost ute-btn-warm" href="${pageContext.request.contextPath}/user/shipment-fees/${shipment.id}">Xem phí hoàn và phụ thu &rarr;</a>
    </div>
    <c:if test="${not empty notice}"><div class="alert alert-success mb-0" role="status"><c:out value="${notice}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-danger mb-0" role="alert"><c:out value="${error}"/></div></c:if>
    <section class="ute-app-card ute-detail-top">
        <div>
            <h1 class="ute-detail-title">Vận đơn <c:out value="${shipment.trackingNumber}"/> <u:statusbadge status="${shipment.currentStatus}"/></h1>
            <p class="ute-detail-fee">Cước phí vận chuyển: <strong><fmt:formatNumber value="${shipment.shippingFee}" pattern="#,##0" var="shippingFormatted"/>${fn:replace(shippingFormatted, ',', '.')} đ</strong> (<c:choose><c:when test="${shipment.paymentStatus == 'PAID'}">Đã thanh toán</c:when><c:when test="${shipment.paymentStatus == 'UNPAID'}">Chưa thanh toán</c:when><c:otherwise><c:out value="${shipment.paymentStatus}"/></c:otherwise></c:choose>)</p>
            <p class="ute-staff-meta">Tạo lúc: <c:set var="cat" value="${shipment.createdAt}"/>${fn:substring(cat, 8, 10)}/${fn:substring(cat, 5, 7)}/${fn:substring(cat, 0, 4)} · ${fn:substring(cat, 11, 16)}<c:if test="${not empty shipment.estimatedDelivery}"> &middot; Dự kiến giao trước: <c:set var="eta" value="${shipment.estimatedDelivery}"/>${fn:substring(eta, 8, 10)}/${fn:substring(eta, 5, 7)}/${fn:substring(eta, 0, 4)}</c:if></p>
        <c:if test="${shipment.currentStatus == 'CANCELLED'}">
            <c:forEach items="${shipment.events}" var="event">
                <c:if test="${event.eventType == 'SHIPMENT_CANCELLED'}">
                    <div class="alert alert-warning mt-3 mb-0" role="status">
                        <c:choose>
                            <c:when test="${fn:contains(event.description, 'PAYMENT_TIMEOUT')}">Đơn đã tự hủy vì quá hạn thanh toán trực tuyến.</c:when>
                            <c:when test="${fn:contains(event.description, 'PICKUP_SERVICE_FAILURE')}">Đơn đã bị hủy do lỗi dịch vụ lấy hàng của UTEExpress.</c:when>
                            <c:when test="${fn:contains(event.description, 'PICKUP_TIMEOUT')}">Đơn đã tự hủy vì shipper không lấy được hàng trong thời hạn.</c:when>
                            <c:when test="${fn:contains(event.description, 'DROPOFF_TIMEOUT')}">Đơn tự hủy vì chưa được mang tới bưu cục đúng hạn.</c:when>
                            <c:when test="${fn:contains(event.description, 'SENDER_REQUEST')}">Bạn đã hủy đơn này.</c:when>
                            <c:otherwise>Đơn hàng đã bị hủy.</c:otherwise>
                        </c:choose>
                        Cước đã thanh toán sẽ được hoàn vào ví.
                    </div>
                </c:if>
            </c:forEach>
        </c:if>
        </div>
        <div class="ute-detail-next">
            <h2>Bước tiếp theo</h2>
        <c:choose>
            <c:when test="${shipment.currentStatus == 'WAITING_FOR_PAYMENT'}">
                <p>Thanh toán cước để đơn chuyển sang chờ bàn giao. Quá hạn đơn sẽ tự hủy.</p>
                <c:if test="${vnpayEnabled}">
                    <form method="post" action="${pageContext.request.contextPath}/user/shipments/${shipment.id}/vnpay" class="mb-2">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <button class="btn btn-ute-accent" type="submit">Thanh toán qua VNPay</button>
                    </form>
                </c:if>
                <form method="post" action="${pageContext.request.contextPath}/user/shipments/${shipment.id}/confirm-payment">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <button class="btn ${vnpayEnabled ? 'btn-outline-secondary btn-sm' : 'btn-ute-accent'}" type="submit">Xác nhận thanh toán (demo)</button>
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
                            <c:otherwise>Cước tiền mặt thu khi bàn giao kiện hàng.</c:otherwise>
                        </c:choose>
                    </p>
                </c:if>
            </c:when>
            <c:otherwise><p class="mb-0 text-ute-muted">Không có thao tác nào lúc này.</p></c:otherwise>
        </c:choose>
        <c:if test="${shipment.currentStatus == 'CREATED' or shipment.currentStatus == 'WAITING_FOR_HANDOVER'}">
            <form class="ute-detail-cancel" method="post" action="${pageContext.request.contextPath}/user/shipments/${shipment.id}/cancel">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <button class="btn btn-ute-danger-soft" type="submit">Hủy đơn</button>
            </form>
        </c:if>
        </div>
    </section>
    <div class="ute-detail-parties">
        <section class="ute-app-card"><h2>Thông tin Người gửi</h2><p class="ute-party-name"><c:out value="${shipment.senderName}"/> &middot; <c:out value="${shipment.senderPhone}"/></p><p class="mb-0"><c:out value="${shipment.senderAddress}"/></p></section>
        <section class="ute-app-card"><h2>Thông tin Người nhận</h2><p class="ute-party-name"><c:out value="${shipment.receiverName}"/> &middot; <c:out value="${shipment.receiverPhone}"/></p><p class="mb-0"><c:out value="${shipment.receiverAddress}"/></p></section>
    </div>
    <section class="ute-app-card">
        <div class="ute-app-card-head">
            <h2>Lịch sử sự kiện vận đơn (${shipment.events.size()} sự kiện)</h2>
            <p>Mọi thay đổi thanh toán, lấy hàng và bàn giao đều được ghi lại.</p>
        </div>
        <div class="table-responsive">
        <table class="table ute-list-table align-middle">
            <thead><tr><th>Mã sự kiện</th><th>Nội dung chi tiết sự kiện</th><th>Thời điểm ghi nhận</th></tr></thead>
            <tbody>
            <c:forEach items="${shipment.events}" var="event">
                <tr><td><strong><c:out value="${event.eventType}"/></strong></td><td><c:out value="${event.description}"/></td><td><c:set var="at" value="${event.occurredAt}"/>${fn:substring(at, 8, 10)}/${fn:substring(at, 5, 7)}/${fn:substring(at, 0, 4)} · ${fn:substring(at, 11, 16)}</td></tr>
            </c:forEach>
            </tbody>
        </table>
        </div>
    </section>
  </div>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
