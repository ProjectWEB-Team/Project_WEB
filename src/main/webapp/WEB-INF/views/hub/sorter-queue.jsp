<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phân loại tại hub | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-4">
    <h1 class="h3">Phân loại tại hub</h1>
    <p class="text-muted">Nhận hàng vào hub của bạn, rồi để hệ thống chỉ ra hub kế tiếp theo tuyến.</p>
    <c:if test="${not empty operationMessage}"><div class="alert alert-info"><c:out value="${operationMessage}"/></div></c:if>
    <c:if test="${not empty sortingDecision}">
        <div class="alert alert-success">
            Hub kế tiếp: <strong><c:out value="${sortingDecision.nextHubCode}"/></strong>
            <span class="ms-2"><c:out value="${sortingDecision.destinationGrouping}"/></span>
            <span class="ms-2">Sẵn sàng đóng bao: <c:out value="${sortingDecision.containerReady}"/></span>
        </div>
    </c:if>
    <div class="row g-3">
        <section class="col-md-6"><div class="card"><div class="card-body">
            <h2 class="h5">Nhận hàng vào hub</h2>
            <form method="post" action="${pageContext.request.contextPath}/hub/inbound">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <label class="form-label" for="inboundShipmentId">ID đơn</label>
                <input id="inboundShipmentId" name="shipmentId" type="number" min="1" class="form-control" required>
                <p class="text-muted mt-2">Với đơn khách tự mang tới: quét kiện và nhập số đo thực tế. Tiền mặt gồm cước chưa trả và phụ thu (nếu khách đồng ý).</p>
                <label class="form-label" for="trackingNumber">Mã vận đơn trên kiện</label>
                <input id="trackingNumber" name="trackingNumber" class="form-control">
                <label class="form-label" for="actualWeightGrams">Khối lượng thực tế (g)</label>
                <input id="actualWeightGrams" name="actualWeightGrams" type="number" min="1" class="form-control">
                <label class="form-label" for="actualLengthCm">Dài (cm)</label>
                <input id="actualLengthCm" name="actualLengthCm" type="number" min="1" class="form-control">
                <label class="form-label" for="actualWidthCm">Rộng (cm)</label>
                <input id="actualWidthCm" name="actualWidthCm" type="number" min="1" class="form-control">
                <label class="form-label" for="actualHeightCm">Cao (cm)</label>
                <input id="actualHeightCm" name="actualHeightCm" type="number" min="1" class="form-control">
                <label class="form-label" for="cashReceived">Tiền mặt đã nhận (VND)</label>
                <input id="cashReceived" name="cashReceived" type="number" min="0" step="1" class="form-control">
                <label class="form-label" for="surchargeDecision">Lựa chọn của người gửi nếu phát sinh phụ thu</label>
                <select id="surchargeDecision" name="surchargeDecision" class="form-select">
                    <option value="">Chưa quyết định</option><option value="ACCEPT">Đồng ý và trả tiền mặt</option>
                    <option value="PAY_ONLINE">Yêu cầu thanh toán phụ thu bằng ví / VNPay demo</option><option value="REJECT">Từ chối phụ thu và hủy đơn</option>
                </select>
                <button class="btn btn-primary mt-3" type="submit">Nhận vào hub</button>
            </form>
        </div></div></section>
        <section class="col-md-6"><div class="card"><div class="card-body">
            <h2 class="h5">Phân loại theo tuyến</h2>
            <form method="post" action="${pageContext.request.contextPath}/hub/sorter">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <label class="form-label" for="sortShipmentId">ID đơn</label>
                <input id="sortShipmentId" name="shipmentId" type="number" min="1" class="form-control" required>
                <button class="btn btn-success mt-3" type="submit">Xác định hub kế tiếp</button>
            </form>
        </div></div></section>
    </div>
</main>
</body>
</html>
