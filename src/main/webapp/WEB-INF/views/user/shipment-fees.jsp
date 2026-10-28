<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phí hoàn và phụ thu | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
</head>
<body class="bg-ute-canvas">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="ute-app flex-grow-1">
  <div class="ute-wrap">
    <div class="ute-detail-bar">
        <div class="d-flex gap-2">
            <a class="btn ute-btn-ghost" href="${pageContext.request.contextPath}/user/shipments/${fees.shipmentId()}">&larr; Quay lại chi tiết vận đơn</a>
            <a class="btn ute-btn-ghost" href="${pageContext.request.contextPath}/user/shipments">Danh sách đơn đã gửi</a>
        </div>
    </div>
    <div class="ute-app-head">
        <div>
            <h1>Phí hoàn hàng và Phụ thu cân nặng (Vận đơn <c:out value="${fees.trackingNumber()}"/>)</h1>
            <p>Hiển thị các khoản phí phát sinh khi kiện hàng bị hoàn trả về gốc hoặc chênh lệch khối lượng khi cân đo thực tế tại bưu cục.</p>
        </div>
    </div>
    <c:set var="totalDue" value="${fees.forwardDue() + fees.returnDue() + fees.surchargeDue()}"/>
    <fmt:formatNumber value="${fees.forwardDue()}" pattern="#,##0" var="forwardFormatted"/>
    <fmt:formatNumber value="${fees.returnDue()}" pattern="#,##0" var="returnFormatted"/>
    <fmt:formatNumber value="${fees.surchargeDue()}" pattern="#,##0" var="surchargeFormatted"/>
    <fmt:formatNumber value="${totalDue}" pattern="#,##0" var="totalFormatted"/>
    <c:choose>
        <c:when test="${totalDue == 0}">
            <div id="shipment-fees-empty" class="ute-fee-alert" role="status">Hiện không có phí hoàn hoặc phụ thu cần thanh toán. Bạn có thể quay lại chi tiết để xem trạng thái đơn.</div>
        </c:when>
        <c:otherwise>
            <div class="ute-fee-alert" role="status">Còn khoản chưa thanh toán: tổng <strong>${fn:replace(totalFormatted, ',', '.')} đ</strong>.</div>
        </c:otherwise>
    </c:choose>
    <section class="ute-app-card" aria-labelledby="fee-breakdown-title">
        <div class="ute-app-card-head"><h2 id="fee-breakdown-title">Bảng bóc tách các khoản phí phát sinh cần thanh toán &mdash; Vận đơn <c:out value="${fees.trackingNumber()}"/></h2></div>
        <div class="ute-fee-tiles">
            <div class="ute-fee-tile"><small>Cước chiều đi còn thiếu</small><strong>${fn:replace(forwardFormatted, ',', '.')} đ</strong><span>Cước chiều đi chưa thanh toán khi hoàn hàng</span></div>
            <div class="ute-fee-tile"><small>Phí chuyển hoàn về gốc</small><strong>${fn:replace(returnFormatted, ',', '.')} đ</strong><span>Phí hoàn hàng về người gửi</span></div>
            <div class="ute-fee-tile"><small>Phụ thu vượt cân tại hub</small><strong>${fn:replace(surchargeFormatted, ',', '.')} đ</strong><span>Chênh lệch khối lượng cân thực tế</span></div>
            <div class="ute-fee-tile is-total"><small>Tổng cần thanh toán</small><strong id="shipment-fees-total">${fn:replace(totalFormatted, ',', '.')} đ</strong><span>Thanh toán để mở khóa nhận lại hàng hoàn</span></div>
        </div>
    </section>
    <c:if test="${totalDue != 0}">
    <div class="ute-fee-pay">
        <c:forEach items="${['RETURN','WEIGHT_SURCHARGE']}" var="kind">
            <c:if test="${kind == 'RETURN' ? fees.forwardDue() + fees.returnDue() > 0 : fees.surchargeDue() > 0}">
                <section class="ute-app-card" aria-labelledby="payment-title-${kind}">
                    <div class="ute-app-card-head">
                        <h2 id="payment-title-${kind}">${kind == 'RETURN' ? 'Thanh toán các khoản hoàn hàng' : 'Thanh toán phụ thu cân nặng'}</h2>
                        <p>Chọn phương thức thanh toán cho khoản này.</p>
                    </div>
                    <div class="ute-fee-buttons">
                        <form method="post" action="${pageContext.request.contextPath}/user/shipment-fees/${fees.shipmentId()}/wallet">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <input type="hidden" name="obligation" value="${kind}">
                            <button type="submit" class="btn btn-ute-primary w-100">Thanh toán bằng Ví</button>
                        </form>
                        <form method="post" action="${pageContext.request.contextPath}/user/shipment-fees/${fees.shipmentId()}/demo-vnpay">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <input type="hidden" name="obligation" value="${kind}">
                            <button type="submit" class="btn ute-btn-warm-outline w-100">Thanh toán qua VNPAY</button>
                            <div class="form-check mt-2">
                                <input id="confirm-demo-${kind}" class="form-check-input" type="checkbox" name="confirmDemo" value="true" required>
                                <label class="form-check-label small" for="confirm-demo-${kind}">Tôi xác nhận thanh toán VNPay mô phỏng, không phát sinh giao dịch ngân hàng thật.</label>
                            </div>
                        </form>
                    </div>
                </section>
            </c:if>
        </c:forEach>
    </div>
    </c:if>
  </div>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
