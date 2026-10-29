<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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
<main class="ute-staff flex-grow-1">
    <h1>Phân loại &amp; Tiếp nhận kiện hàng tại Hub</h1>
    <p class="ute-staff-sub">Cân đo kiện hàng khi nhận vào Hub; hệ thống xác định Hub kế tiếp.</p>
    <c:if test="${not empty operationMessage}"><div class="alert alert-info"><c:out value="${operationMessage}"/></div></c:if>
    <c:if test="${not empty sortingDecision}">
        <div class="ute-result-banner">
            <strong>&#10003; Hub kế tiếp: <c:out value="${sortingDecision.nextHubCode}"/></strong>
            <span>Nhóm đích: <c:out value="${sortingDecision.destinationGrouping}"/> &middot; Sẵn sàng đóng bao: <c:out value="${sortingDecision.containerReady ? 'CÓ' : 'CHƯA'}"/></span>
        </div>
    </c:if>
    <section class="ute-staff-card">
        <div>
            <h2 class="ute-staff-h2">1. Nhận kiện hàng vào Hub &amp; Cân đo khối lượng thực tế</h2>
            <p class="ute-staff-note">Quét mã vận đơn, nhập số đo thực tế và tiền mặt thu tại quầy (nếu có).</p>
        </div>
        <form class="ute-form-grid" method="post" action="${pageContext.request.contextPath}/hub/inbound">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <div><label for="inboundShipmentId">ID đơn hàng</label><input id="inboundShipmentId" name="shipmentId" type="number" min="1" class="form-control" required></div>
            <div><label for="trackingNumber">Mã vận đơn trên kiện</label><input id="trackingNumber" name="trackingNumber" class="form-control"></div>
            <div><label for="actualWeightGrams">Khối lượng thực tế (g)</label><input id="actualWeightGrams" name="actualWeightGrams" type="number" min="1" class="form-control"></div>
            <div><label for="actualLengthCm">Dài (cm)</label><input id="actualLengthCm" name="actualLengthCm" type="number" min="1" class="form-control"></div>
            <div><label for="actualWidthCm">Rộng (cm)</label><input id="actualWidthCm" name="actualWidthCm" type="number" min="1" class="form-control"></div>
            <div><label for="actualHeightCm">Cao (cm)</label><input id="actualHeightCm" name="actualHeightCm" type="number" min="1" class="form-control"></div>
            <div><label for="cashReceived">Tiền mặt đã nhận tại quầy (VND)</label><input id="cashReceived" name="cashReceived" type="number" min="0" step="1" class="form-control"></div>
            <div>
                <label for="surchargeDecision">Xử lý nếu phát sinh phụ thu vượt cân</label>
                <select id="surchargeDecision" name="surchargeDecision" class="form-select">
                    <option value="">Chưa quyết định</option><option value="ACCEPT">Đồng ý và trả tiền mặt ngay tại quầy</option>
                    <option value="PAY_ONLINE">Yêu cầu thanh toán phụ thu bằng ví / VNPay demo</option><option value="REJECT">Từ chối phụ thu và hủy đơn</option>
                </select>
            </div>
            <div class="ute-form-grid-foot">
                <p>Khối lượng quy đổi = Dài &times; Rộng &times; Cao / 6000.</p>
                <button class="btn btn-ute-primary" type="submit">Nhận kiện vào Hub</button>
            </div>
        </form>
    </section>
    <section class="ute-staff-card">
        <h2 class="ute-staff-h2">2. Phân loại theo tuyến &amp; Chỉ định Hub kế tiếp</h2>
        <form class="ute-staff-form" method="post" action="${pageContext.request.contextPath}/hub/sorter">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <div>
                <label for="sortShipmentId">Nhập ID đơn hàng hoặc quét mã kiện cần phân tuyến</label>
                <input id="sortShipmentId" name="shipmentId" type="number" min="1" class="form-control" required>
            </div>
            <button class="btn btn-ute-orange" type="submit">Xác định Hub kế tiếp</button>
        </form>
    </section>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
