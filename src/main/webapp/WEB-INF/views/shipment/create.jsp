<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tạo vận đơn | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
    <script src="${pageContext.request.contextPath}/assets/js/shipment-wizard.js" defer></script>
    <script src="${pageContext.request.contextPath}/assets/js/shipment-address.js" defer></script>
</head>
<body class="bg-ute-canvas">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container shipment-form-container py-4">
    <a class="d-inline-block text-ute-primary fw-bold text-decoration-none mb-3" href="${pageContext.request.contextPath}/">UTEExpress</a>
    <h1 class="h3 mb-1">Tạo vận đơn</h1>
    <p class="text-ute-muted mb-4">Điền thông tin, xem báo giá rồi chọn cách gửi hàng. Bạn có thể quay lại để chỉnh sửa trước khi tạo đơn.</p>
    <nav id="shipment-step-navigation" aria-label="Các bước tạo vận đơn" hidden>
        <ol class="shipment-steps">
            <li><button type="button" data-step-target="0" aria-controls="shipment-step-1">1. Người gửi và nhận</button></li>
            <li><button type="button" data-step-target="1" aria-controls="shipment-step-2">2. Kiện hàng</button></li>
            <li><button type="button" data-step-target="2" aria-controls="shipment-step-3">3. Cước phí</button></li>
            <li><button type="button" data-step-target="3" aria-controls="shipment-step-4">4. Xác nhận</button></li>
        </ol>
    </nav>
    <spring:hasBindErrors name="createShipmentRequest">
        <c:if test="${not empty errors.globalErrors}">
            <div id="shipment-form-errors" class="alert alert-danger" role="alert" tabindex="-1"><c:forEach items="${errors.globalErrors}" var="e"><div><c:out value="${e.defaultMessage}"/></div></c:forEach></div>
        </c:if>
    </spring:hasBindErrors>
    <form:form id="shipment-create-form" method="post" action="${pageContext.request.contextPath}/user/shipments" modelAttribute="createShipmentRequest" data-has-quote="${not empty quote}" data-address-catalog-url="${pageContext.request.contextPath}/user/shipments/address-catalog">
        <c:if test="${not empty quote}"><input type="hidden" name="quoteReference" value="<c:out value='${quoteReference}'/>"></c:if>
        <section id="shipment-step-1" data-shipment-step="0" class="card-ute p-4 mb-3" aria-labelledby="shipment-step-title-1">
            <h2 id="shipment-step-title-1" class="h5 text-ute-primary" tabindex="-1">1. Người gửi và người nhận</h2>
            <p class="small text-ute-muted">Địa chỉ theo địa giới từ 01/07/2025: 34 tỉnh/thành và 3.321 phường/xã. Chọn tỉnh/thành, phường/xã rồi nhập số nhà, tên đường hoặc thôn/ấp.</p>
            <fieldset class="row g-3 mb-4" data-address-group="sender">
                <legend class="h6 mt-3">Thông tin người gửi</legend>
                <div class="col-md-6"><label class="form-label" for="senderName">Họ tên người gửi</label><form:input id="senderName" path="senderName" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" required="required" maxlength="100" autocomplete="name"/><form:errors path="senderName" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
                <div class="col-md-6"><label class="form-label" for="senderPhone">Điện thoại người gửi</label><form:input id="senderPhone" path="senderPhone" type="tel" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" required="required" maxlength="20" autocomplete="tel"/><form:errors path="senderPhone" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
                <div class="col-md-6">
                    <label class="form-label" for="senderProvinceCode">Tỉnh/thành người gửi</label>
                    <form:select id="senderProvinceCode" path="senderProvinceCode" cssClass="form-select" cssErrorClass="form-select is-invalid" required="required" autocomplete="address-level1" aria-describedby="sender-area-status">
                        <form:option value="" label="Chọn tỉnh/thành"/>
                        <form:options items="${addressProvinces}" itemValue="code" itemLabel="name" htmlEscape="true"/>
                    </form:select>
                    <form:errors path="senderProvinceCode" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="senderWardCode">Phường/xã/đặc khu người gửi</label>
                    <input id="senderWardSearch" type="search" class="form-control mb-2" placeholder="Tìm phường/xã…" aria-label="Tìm phường/xã người gửi" aria-controls="senderWardCode" autocomplete="off">
                    <form:select id="senderWardCode" path="senderWardCode" cssClass="form-select" cssErrorClass="form-select is-invalid" required="required" autocomplete="address-level2" aria-describedby="sender-area-status">
                        <form:option value="" label="Chọn phường/xã/đặc khu"/>
                        <form:options items="${senderWards}" itemValue="code" itemLabel="name" htmlEscape="true"/>
                    </form:select>
                    <form:errors path="senderWardCode" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                </div>
                <div class="col-12"><p id="sender-area-status" class="small text-ute-muted mb-0" role="status" aria-live="polite">Chọn địa chỉ để tự xác định vùng tính cước.</p></div>
                <div class="col-12"><label class="form-label" for="senderAddress">Số nhà, tên đường / thôn, ấp</label><form:input id="senderAddress" path="senderAddress" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" required="required" maxlength="180" placeholder="Ví dụ: 12 Nguyễn Huệ, tòa nhà A, tầng 2" autocomplete="address-line1"/><form:errors path="senderAddress" cssClass="invalid-feedback d-block" htmlEscape="true"/><div class="form-text">Không nhập lại tỉnh/thành và phường/xã đã chọn.</div></div>
            </fieldset>
            <fieldset class="row g-3 mb-4" data-address-group="receiver">
                <legend class="h6 mt-3">Thông tin người nhận</legend>
                <div class="col-md-6"><label class="form-label" for="receiverName">Họ tên người nhận</label><form:input id="receiverName" path="receiverName" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" required="required" maxlength="100" autocomplete="name"/><form:errors path="receiverName" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
                <div class="col-md-6"><label class="form-label" for="receiverPhone">Điện thoại người nhận</label><form:input id="receiverPhone" path="receiverPhone" type="tel" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" required="required" maxlength="20" autocomplete="tel"/><form:errors path="receiverPhone" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
                <div class="col-md-6">
                    <label class="form-label" for="receiverProvinceCode">Tỉnh/thành người nhận</label>
                    <form:select id="receiverProvinceCode" path="receiverProvinceCode" cssClass="form-select" cssErrorClass="form-select is-invalid" required="required" autocomplete="address-level1" aria-describedby="receiver-area-status">
                        <form:option value="" label="Chọn tỉnh/thành"/>
                        <form:options items="${addressProvinces}" itemValue="code" itemLabel="name" htmlEscape="true"/>
                    </form:select>
                    <form:errors path="receiverProvinceCode" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="receiverWardCode">Phường/xã/đặc khu người nhận</label>
                    <input id="receiverWardSearch" type="search" class="form-control mb-2" placeholder="Tìm phường/xã…" aria-label="Tìm phường/xã người nhận" aria-controls="receiverWardCode" autocomplete="off">
                    <form:select id="receiverWardCode" path="receiverWardCode" cssClass="form-select" cssErrorClass="form-select is-invalid" required="required" autocomplete="address-level2" aria-describedby="receiver-area-status">
                        <form:option value="" label="Chọn phường/xã/đặc khu"/>
                        <form:options items="${receiverWards}" itemValue="code" itemLabel="name" htmlEscape="true"/>
                    </form:select>
                    <form:errors path="receiverWardCode" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                </div>
                <div class="col-12"><p id="receiver-area-status" class="small text-ute-muted mb-0" role="status" aria-live="polite">Chọn địa chỉ để tự xác định vùng tính cước.</p></div>
                <div class="col-12"><label class="form-label" for="receiverAddress">Số nhà, tên đường / thôn, ấp</label><form:input id="receiverAddress" path="receiverAddress" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" required="required" maxlength="180" placeholder="Ví dụ: 12 Nguyễn Huệ, tòa nhà A, tầng 2" autocomplete="address-line1"/><form:errors path="receiverAddress" cssClass="invalid-feedback d-block" htmlEscape="true"/><div class="form-text">Không nhập lại tỉnh/thành và phường/xã đã chọn.</div></div>
            </fieldset>
            <noscript><p class="alert alert-warning">Vui lòng bật JavaScript để chọn địa chỉ liên kết và xem từng bước tạo đơn.</p></noscript>
        </section>
        <section id="shipment-step-2" data-shipment-step="1" class="card-ute p-4 mb-3" aria-labelledby="shipment-step-title-2">
            <h2 id="shipment-step-title-2" class="h5 text-ute-primary" tabindex="-1">2. Kiện hàng và dịch vụ</h2>
            <div class="row g-3">
                        <div class="col-md-4"><label class="form-label" for="serviceCode">Dịch vụ</label><form:select id="serviceCode" path="serviceCode" required="required" cssClass="form-select" cssErrorClass="form-select is-invalid"><form:option value="STANDARD" label="Tiêu chuẩn"/><form:option value="FAST" label="Nhanh"/><form:option value="EXPRESS" label="Hỏa tốc"/></form:select><form:errors path="serviceCode" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>


                        <div class="col-md-3"><label class="form-label" for="weightGrams">Khối lượng (gram)</label><form:input id="weightGrams" path="weightGrams" type="number" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" required="required" min="1" max="1000000"/><form:errors path="weightGrams" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
                        <div class="col-md-3"><label class="form-label" for="lengthCm">Dài (cm)</label><form:input id="lengthCm" path="lengthCm" type="number" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" required="required" min="1" max="1000"/><form:errors path="lengthCm" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
                        <div class="col-md-3"><label class="form-label" for="widthCm">Rộng (cm)</label><form:input id="widthCm" path="widthCm" type="number" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" required="required" min="1" max="1000"/><form:errors path="widthCm" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
                        <div class="col-md-3"><label class="form-label" for="heightCm">Cao (cm)</label><form:input id="heightCm" path="heightCm" type="number" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" required="required" min="1" max="1000"/><form:errors path="heightCm" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
                        <div class="col-md-6"><label class="form-label" for="declaredValue">Giá trị khai báo (VND)</label><form:input id="declaredValue" path="declaredValue" type="number" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" required="required" min="0" max="9999999999999.99" step="0.01"/><form:errors path="declaredValue" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
                        <div class="col-md-6"><label class="form-label" for="codAmount">Tiền thu hộ COD (VND)</label><form:input id="codAmount" path="codAmount" type="number" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" required="required" min="0" max="9999999999999.99" step="0.01"/><form:errors path="codAmount" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
                        <div class="col-12"><label class="form-label" for="inspectionPolicy">Kiểm hàng</label><form:select id="inspectionPolicy" path="inspectionPolicy" required="required" cssClass="form-select" cssErrorClass="form-select is-invalid"><form:option value="NO_INSPECTION" label="Không cho kiểm"/><form:option value="INSPECTION_ONLY" label="Cho xem hàng"/><form:option value="TRIAL_ALLOWED" label="Cho thử hàng"/></form:select><form:errors path="inspectionPolicy" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
            </div>
        </section>
        <section id="shipment-step-3" data-shipment-step="2" class="card-ute p-4 mb-3" aria-labelledby="shipment-step-title-3">
            <h2 id="shipment-step-title-3" class="h5 text-ute-primary" tabindex="-1">3. Cước phí và thanh toán</h2>
            <div class="row g-3">
                        <div class="col-md-6"><label class="form-label" for="feePayer">Người trả cước</label><form:select id="feePayer" path="feePayer" required="required" cssClass="form-select" cssErrorClass="form-select is-invalid"><form:option value="SENDER" label="Người gửi"/><form:option value="RECEIVER" label="Người nhận"/></form:select><form:errors path="feePayer" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
                        <div class="col-md-6"><label class="form-label" for="paymentMethod">Hình thức</label><form:select id="paymentMethod" path="paymentMethod" required="required" cssClass="form-select" cssErrorClass="form-select is-invalid"><form:option value="VNPAY" label="Trực tuyến (demo)"/><form:option value="CASH" label="Tiền mặt"/></form:select><form:errors path="paymentMethod" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
            </div>
            <p class="small text-ute-muted mt-3 mb-0">Tiền mặt do người gửi trả được thu khi shipper lấy hàng hoặc khi bạn mang ra bưu cục. Người nhận trả cước thì thu khi giao hàng. Không thu tiền lúc tạo đơn.</p>
            <p class="small text-ute-muted mt-2">Nếu người nhận trả cước, chọn hình thức tiền mặt.</p>
            <c:if test="${not empty quote}">
                <div id="shipment-quote" class="alert alert-info" role="status">Cước phí: <strong><fmt:formatNumber value="${quote.shippingFee}" pattern="#,##0" var="quoteFormatted"/>${fn:replace(quoteFormatted, ',', '.')}đ</strong>. Hệ thống sẽ kiểm tra lại bảng giá khi tạo đơn.</div>
            </c:if>
            <p id="shipment-quote-notice" class="small text-ute-muted" aria-live="polite">Xem báo giá trước khi chuyển sang xác nhận.</p>
            <button id="shipment-quote-button" type="submit" formaction="${pageContext.request.contextPath}/user/shipments/quote" class="btn btn-outline-primary">Xem báo giá</button>
        </section>
        <section id="shipment-step-4" data-shipment-step="3" class="card-ute p-4 mb-3" aria-labelledby="shipment-step-title-4">
            <h2 id="shipment-step-title-4" class="h5 text-ute-primary" tabindex="-1">4. Cách gửi hàng và xác nhận</h2>
            <div class="row g-3">
                        <div class="col-12"><label class="form-label" for="pickupMethod">Hình thức gửi</label><form:select id="pickupMethod" path="pickupMethod" required="required" cssClass="form-select" cssErrorClass="form-select is-invalid"><form:option value="SELF_DROPOFF" label="Tự mang ra bưu cục"/><form:option value="HOME_PICKUP" label="Shipper đến lấy tại nhà"/></form:select><form:errors path="pickupMethod" cssClass="invalid-feedback d-block" htmlEscape="true"/></div>
            </div>
            <p class="small text-ute-muted mt-3">Tự mang hàng: mang kiện hàng và mã vận đơn đến bưu cục sau khi tạo đơn. Lấy tại nhà: chờ xác nhận và phân công shipper đến địa chỉ người gửi.</p>
            <div id="shipment-summary" class="mt-4" hidden>
                <h3 class="h6">Kiểm tra trước khi tạo đơn</h3>
                <dl class="shipment-summary mb-3">
                    <dt>Người gửi</dt><dd data-summary="sender"></dd>
                    <dt>Người nhận</dt><dd data-summary="receiver"></dd>
                    <dt>Tuyến vận chuyển</dt><dd data-summary="route"></dd>
                    <dt>Kiện hàng / dịch vụ</dt><dd data-summary="parcel"></dd>
                    <dt>Giá trị khai báo / COD</dt><dd data-summary="values"></dd>
                    <dt>Thanh toán cước</dt><dd data-summary="payment"></dd>
                    <dt>Cách gửi hàng</dt><dd data-summary="pickup"></dd>
                    <dt>Cước phí đã báo</dt><dd data-summary="fee"></dd>
                </dl>
            </div>
            <c:if test="${not empty quote}"><button id="shipment-create-button" type="submit" class="btn btn-ute-accent">Xác nhận tạo vận đơn</button></c:if>
        </section>
        <div id="shipment-step-actions" hidden>
            <div class="d-flex gap-2 justify-content-between align-items-center">
                <button id="shipment-previous" type="button" class="btn btn-outline-secondary">Quay lại</button>
                <p id="shipment-step-status" class="small text-ute-muted mb-0" aria-live="polite"></p>
                <button id="shipment-next" type="button" class="btn btn-ute-primary">Tiếp tục</button>
            </div>
        </div>
    </form:form>
</main>
</body>
</html>
