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
<main class="container shipment-form-container py-4">
    <nav class="d-flex flex-wrap gap-2 mb-4" aria-label="Điều hướng vận đơn">
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/user/shipments/${fees.shipmentId()}">← Quay lại chi tiết vận đơn</a>
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/user/shipments">Đơn đã gửi</a>
    </nav>
    <header class="mb-4">
        <p class="text-ute-primary fw-bold mb-1">UTEExpress</p>
        <h1 class="h3">Phí hoàn và phụ thu</h1>
        <p class="text-ute-muted mb-0">Vận đơn <strong class="font-monospace"><c:out value="${fees.trackingNumber()}"/></strong></p>
    </header>
    <p class="text-ute-muted">Trang này hiển thị các khoản phát sinh khi hoàn hàng hoặc cân lại kiện hàng. Cước vận chuyển ban đầu và trạng thái thanh toán nằm ở trang chi tiết vận đơn.</p>
    <c:set var="totalDue" value="${fees.forwardDue() + fees.returnDue() + fees.surchargeDue()}"/>
    <section class="card-ute p-4 mb-4" aria-labelledby="fee-breakdown-title">
        <h2 id="fee-breakdown-title" class="h5 text-ute-primary mb-3">Các khoản còn phải trả</h2>
        <dl class="row mb-0">
            <dt class="col-sm-8 fw-normal mb-2">Cước chiều đi còn thiếu khi hoàn hàng</dt>
            <dd class="col-sm-4 text-sm-end"><fmt:formatNumber value="${fees.forwardDue()}" pattern="#,##0" var="forwardFormatted"/>${fn:replace(forwardFormatted, ',', '.')}đ</dd>
            <dt class="col-sm-8 fw-normal mb-2">Phí hoàn hàng</dt>
            <dd class="col-sm-4 text-sm-end"><fmt:formatNumber value="${fees.returnDue()}" pattern="#,##0" var="returnFormatted"/>${fn:replace(returnFormatted, ',', '.')}đ</dd>
            <dt class="col-sm-8 fw-normal mb-2">Phụ thu cân nặng</dt>
            <dd class="col-sm-4 text-sm-end"><fmt:formatNumber value="${fees.surchargeDue()}" pattern="#,##0" var="surchargeFormatted"/>${fn:replace(surchargeFormatted, ',', '.')}đ</dd>
            <dt class="col-sm-8 border-top pt-3">Tổng cần thanh toán tại đây</dt>
            <dd id="shipment-fees-total" class="col-sm-4 text-sm-end border-top pt-3 fw-bold text-ute-primary"><fmt:formatNumber value="${totalDue}" pattern="#,##0" var="totalFormatted"/>${fn:replace(totalFormatted, ',', '.')}đ</dd>
        </dl>
    </section>
    <c:choose>
        <c:when test="${totalDue == 0}">
            <div id="shipment-fees-empty" class="alert alert-info" role="status">Hiện không có phí hoàn hoặc phụ thu cần thanh toán. Bạn có thể quay lại chi tiết để xem trạng thái đơn và cước vận chuyển ban đầu.</div>
        </c:when>
        <c:otherwise>
            <c:forEach items="${['RETURN','WEIGHT_SURCHARGE']}" var="kind">
                <c:if test="${kind == 'RETURN' ? fees.forwardDue() + fees.returnDue() > 0 : fees.surchargeDue() > 0}">
                    <section class="card-ute p-4 mb-3" aria-labelledby="payment-title-${kind}">
                        <h2 id="payment-title-${kind}" class="h5">${kind == 'RETURN' ? 'Thanh toán các khoản hoàn hàng' : 'Thanh toán phụ thu cân nặng'}</h2>
                        <p class="text-ute-muted">Chọn phương thức thanh toán cho khoản này.</p>
                        <form method="post" action="${pageContext.request.contextPath}/user/shipment-fees/${fees.shipmentId()}/wallet" class="mb-3">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <input type="hidden" name="obligation" value="${kind}">
                            <button type="submit" class="btn btn-ute-primary">Thanh toán bằng ví</button>
                        </form>
                        <form method="post" action="${pageContext.request.contextPath}/user/shipment-fees/${fees.shipmentId()}/demo-vnpay" class="border-top pt-3">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <input type="hidden" name="obligation" value="${kind}">
                            <div class="form-check mb-3">
                                <input id="confirm-demo-${kind}" class="form-check-input" type="checkbox" name="confirmDemo" value="true" required>
                                <label class="form-check-label" for="confirm-demo-${kind}">Tôi xác nhận thanh toán VNPay mô phỏng, không phát sinh giao dịch ngân hàng thật.</label>
                            </div>
                            <button type="submit" class="btn btn-outline-primary">Thanh toán VNPay demo</button>
                        </form>
                    </section>
                </c:if>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
