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
<main class="ute-staff flex-grow-1">
    <c:if test="${not empty message}"><div class="alert alert-success" role="status"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
    <h1>Đơn chờ phân công Shipper lấy hàng</h1>
    <p class="ute-staff-sub">Điều phối nhân viên giao nhận (Shipper) thuộc Bưu cục đến địa chỉ người gửi để nhận kiện hàng tận nơi.</p>
    <c:if test="${empty shippers}"><div class="alert alert-warning" role="status">Bưu cục chưa có shipper đang hoạt động.</div></c:if>
    <c:choose>
        <c:when test="${empty pickups}"><p class="ute-staff-card text-ute-muted mb-0">Không có đơn chờ lấy hàng.</p></c:when>
        <c:otherwise>
            <c:forEach items="${pickups}" var="p">
                <section class="ute-staff-card">
                    <div class="ute-staff-card-head">
                        <div>
                            <h2><c:out value="${p.trackingNumber}"/> &middot; <c:out value="${p.senderName}"/> &middot; <c:out value="${p.senderPhone}"/></h2>
                            <p>Địa chỉ lấy hàng: <c:out value="${p.senderAddress}"/></p>
                        </div>
                        <span class="ute-chip-warn">Chờ từ <c:out value="${p.waitingSince}"/></span>
                    </div>
                    <div class="ute-staff-row">
                        <form class="ute-staff-form" method="post" action="${pageContext.request.contextPath}/hub/pickups/${p.shipmentId}/assign">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <div>
                                <label for="shipper-${p.shipmentId}">Chọn Shipper phụ trách lấy hàng</label>
                                <select class="form-select" id="shipper-${p.shipmentId}" name="shipperId" required>
                                    <c:forEach items="${shippers}" var="s"><option value="${s.userId}"><c:out value="${s.fullName}"/></option></c:forEach>
                                </select>
                            </div>
                            <button class="btn btn-ute-orange" type="submit">Phân công lấy hàng</button>
                        </form>
                        <form class="ute-staff-form" method="post" action="${pageContext.request.contextPath}/hub/pickups/${p.shipmentId}/service-failure">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <div>
                                <label for="note-${p.shipmentId}">Lý do hủy dịch vụ (Nếu quá tải / ngoài vùng phục vụ)</label>
                                <input class="form-control" id="note-${p.shipmentId}" name="note" maxlength="200" required placeholder="Nhập lý do lỗi dịch vụ UTEExpress...">
                            </div>
                            <button class="btn btn-ute-danger-soft" type="submit">Hủy do lỗi dịch vụ</button>
                        </form>
                    </div>
                </section>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
