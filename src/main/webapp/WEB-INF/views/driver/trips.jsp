<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><title>Chuyến xe của tài xế</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/ute-express.css"></head>
<body>
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" /><main class="container py-4"><h1>Chuyến xe trung chuyển được giao</h1>
<c:if test="${not empty operationMessage}"><div class="alert alert-info"><c:out value="${operationMessage}"/></div></c:if>
<p>Bạn chỉ thao tác được trên các bao đã nhận bàn giao. Báo tới nơi chưa phải là hub đích đã nhận hàng.</p>
<form method="post" action="${pageContext.request.contextPath}/driver/handovers/scan"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>ID phiếu bàn giao <input name="handoverId" type="number" required></label><label>ID bao <input name="containerId" type="number" required></label><button type="submit">Quét bao</button></form>
<form method="post" action="${pageContext.request.contextPath}/driver/handovers/accept"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>ID phiếu bàn giao <input name="handoverId" type="number" required></label>
<label>Xe còn trống <select name="vehicleId" required><option value="">Chọn xe</option><c:forEach items="${vehicles}" var="vehicle"><option value="${vehicle.id}"><c:out value="${vehicle.plateNumber}"/> — <c:out value="${vehicle.vehicleType}"/></option></c:forEach></select></label><button type="submit">Nhận bàn giao</button></form>
<form method="post" action="${pageContext.request.contextPath}/driver/trips/start"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>ID chuyến <input name="tripId" type="number" required></label><button type="submit">Bắt đầu chuyến</button></form>
<form method="post" action="${pageContext.request.contextPath}/driver/trips/arrive"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>ID chuyến <input name="tripId" type="number" required></label><button type="submit">Báo đã tới nơi</button></form>
<form method="post" action="${pageContext.request.contextPath}/driver/trips/delay"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>ID chuyến <input name="tripId" type="number" required></label><label>Ghi chú trễ <textarea name="note" maxlength="500" required></textarea></label><label>Tham chiếu bằng chứng <input name="evidenceReference" maxlength="120"></label><button type="submit">Báo chuyến bị trễ</button></form>
</main></body></html>
