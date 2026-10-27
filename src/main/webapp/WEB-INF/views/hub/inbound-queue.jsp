<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html><head><title>Nhận hàng vào hub</title></head><body>
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<h1>Nhận hàng tại hub đích</h1>
<c:if test="${not empty operationMessage}"><p><c:out value="${operationMessage}"/></p></c:if>
<form method="post" action="${pageContext.request.contextPath}/hub/inbound/receive">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
  <label>Bàn giao <input name="handoverId" required /></label>
  <label>Bao hàng <input name="containerId" required /></label>
  <label>Đơn hàng <input name="shipmentId" required /></label>
  <button type="submit">Ghi nhận đã nhận hàng</button>
</form>
<h2>Mở bao xong và đối chiếu danh sách</h2>
<form method="post" action="${pageContext.request.contextPath}/hub/inbound/reconcile">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
  <label>Bàn giao <input name="handoverId" type="number" min="1" required /></label>
  <label>Bao hàng <input name="containerId" type="number" min="1" required /></label>
  <label>ID các đơn đã nhận, cách nhau bằng dấu phẩy <input name="receivedShipmentIds" required /></label>
  <button type="submit">Ghi nhận kiện đã nhận và kiện thiếu</button>
</form>
<form method="post" action="${pageContext.request.contextPath}/hub/inbound/reconcile-empty">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
  <label>Bàn giao <input name="handoverId" type="number" min="1" required /></label>
  <label>Bao hàng <input name="containerId" type="number" min="1" required /></label>
  <label><input type="checkbox" name="confirmAllMissing" value="true" required /> Không nhận được kiện nào trong danh sách</label>
  <button type="submit">Báo thiếu toàn bộ</button>
</form>
<h2>Nhận lại kiện giao thất bại từ shipper</h2><form method="post" action="${pageContext.request.contextPath}/hub/delivery-returns/receive">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><label>Mã ID vận đơn<input name="shipmentId" type="number" required></label>
<label>Quét mã vận đơn<input name="trackingNumber" required></label><button>Xác nhận nhận kiện vật lý</button></form>
</body></html>
