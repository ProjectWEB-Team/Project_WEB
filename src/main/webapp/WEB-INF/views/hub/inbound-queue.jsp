<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Nhận hàng vào hub</title></head><body>
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container">
<h1>Nhận hàng tại hub đích</h1>
<c:if test="${not empty operationMessage}"><p><c:out value="${operationMessage}"/></p></c:if>
<h2>Phiếu bàn giao đang chờ nhận <c:if test="${not empty handoverPicks}">(<c:out value="${handoverPicks.size()}"/>)</c:if></h2>
<c:choose>
  <c:when test="${empty handoverPicks}"><p class="text-muted">Chưa có phiếu nào đang trên đường tới bưu cục của bạn.</p></c:when>
  <c:otherwise>
    <table><thead><tr><th>Phiếu</th><th>Từ bưu cục</th><th>Số bao</th></tr></thead><tbody>
    <c:forEach items="${handoverPicks}" var="h">
      <tr><td>#<c:out value="${h[0]}"/></td><td><c:out value="${h[1]}"/></td>
        <td><c:set var="n" value="0"/><c:forEach items="${containerPicks}" var="cp"><c:if test="${cp[0] == h[0]}"><c:set var="n" value="${n + 1}"/></c:if></c:forEach><c:out value="${n}"/></td></tr>
    </c:forEach></tbody></table>
  </c:otherwise>
</c:choose>
<form method="post" action="${pageContext.request.contextPath}/hub/inbound/receive">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
  <label>Bàn giao <select name="handoverId" required><option value="">Chọn phiếu</option><c:forEach items="${handoverPicks}" var="p"><option value="${p[0]}">#<c:out value="${p[0]}"/> từ <c:out value="${p[1]}"/></option></c:forEach></select></label>
  <label>Bao hàng <select name="containerId" required><option value="">Chọn bao</option><c:forEach items="${containerPicks}" var="p"><option value="${p[1]}">Phiếu #<c:out value="${p[0]}"/> – bao <c:out value="${p[2]}"/></option></c:forEach></select></label>
  <label>Đơn hàng <select name="shipmentId" required><option value="">Chọn đơn</option><c:forEach items="${shipmentPicks}" var="p"><option value="${p[1]}">Bao #<c:out value="${p[0]}"/> – <c:out value="${p[2]}"/></option></c:forEach></select></label>
  <button type="submit">Ghi nhận đã nhận hàng</button>
</form>
<h2>Mở bao xong và đối chiếu danh sách</h2>
<form method="post" action="${pageContext.request.contextPath}/hub/inbound/reconcile">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
  <label>Bàn giao <select name="handoverId" required><option value="">Chọn phiếu</option><c:forEach items="${handoverPicks}" var="p"><option value="${p[0]}">#<c:out value="${p[0]}"/> từ <c:out value="${p[1]}"/></option></c:forEach></select></label>
  <label>Bao hàng <select name="containerId" required><option value="">Chọn bao</option><c:forEach items="${containerPicks}" var="p"><option value="${p[1]}">Phiếu #<c:out value="${p[0]}"/> – bao <c:out value="${p[2]}"/></option></c:forEach></select></label>
  <label>Các đơn đã nhận (giữ Ctrl để chọn nhiều) <select name="receivedShipmentIds" multiple size="6" required><c:forEach items="${shipmentPicks}" var="p"><option value="${p[1]}">Bao #<c:out value="${p[0]}"/> – <c:out value="${p[2]}"/></option></c:forEach></select></label>
  <button type="submit">Ghi nhận kiện đã nhận và kiện thiếu</button>
</form>
<form method="post" action="${pageContext.request.contextPath}/hub/inbound/reconcile-empty">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
  <label>Bàn giao <select name="handoverId" required><option value="">Chọn phiếu</option><c:forEach items="${handoverPicks}" var="p"><option value="${p[0]}">#<c:out value="${p[0]}"/> từ <c:out value="${p[1]}"/></option></c:forEach></select></label>
  <label>Bao hàng <select name="containerId" required><option value="">Chọn bao</option><c:forEach items="${containerPicks}" var="p"><option value="${p[1]}">Phiếu #<c:out value="${p[0]}"/> – bao <c:out value="${p[2]}"/></option></c:forEach></select></label>
  <label><input type="checkbox" name="confirmAllMissing" value="true" required /> Không nhận được kiện nào trong danh sách</label>
  <button type="submit">Báo thiếu toàn bộ</button>
</form>
<h2>Nhận lại kiện giao thất bại từ shipper</h2><form method="post" action="${pageContext.request.contextPath}/hub/delivery-returns/receive">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><label>Mã ID vận đơn<input name="shipmentId" type="number" required></label>
<label>Quét mã vận đơn<input name="trackingNumber" required></label><button>Xác nhận nhận kiện vật lý</button></form>
</main><jsp:include page="/WEB-INF/views/layout/footer.jsp" />
